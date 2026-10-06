package com.guardpay.shared.stellar

import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TrustedContact
import com.guardpay.shared.domain.TxHash
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.signing.SigningCancelledException
import com.guardpay.shared.signing.signWith
import com.guardpay.shared.stellar.ContractAbi.AuthPayload
import com.guardpay.shared.stellar.ContractAbi.Storage
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.AbstractTransaction
import com.soneso.stellar.sdk.Account
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.DecoratedSignature
import com.soneso.stellar.sdk.InvokeHostFunctionOperation
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.Network
import com.soneso.stellar.sdk.TimeBounds
import com.soneso.stellar.sdk.Transaction
import com.soneso.stellar.sdk.TransactionBuilder
import com.soneso.stellar.sdk.TransactionBuilderAccount
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.rpc.requests.SimulateTransactionRequest
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.smartaccount.core.SmartAccountAuth
import com.soneso.stellar.sdk.xdr.ContractDataDurabilityXdr
import com.soneso.stellar.sdk.xdr.HostFunctionXdr
import com.soneso.stellar.sdk.xdr.InvokeContractArgsXdr
import com.soneso.stellar.sdk.xdr.LedgerEntryDataXdr
import com.soneso.stellar.sdk.xdr.LedgerKeyContractDataXdr
import com.soneso.stellar.sdk.xdr.LedgerKeyXdr
import com.soneso.stellar.sdk.xdr.SCValXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizationEntryXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedFunctionXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedInvocationXdr
import com.soneso.stellar.sdk.xdr.SorobanAddressCredentialsXdr
import com.soneso.stellar.sdk.xdr.SorobanCredentialsXdr
import com.soneso.stellar.sdk.xdr.Uint32Xdr
import com.soneso.stellar.sdk.xdr.XdrWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * [StellarGateway] over stellar-sdk 1.14.0 and Soroban RPC, against the deployed
 * `account`, `guardian_hold` and `hold_registry` contracts. Testnet only.
 *
 * Reads: the token balance and `spent_today` are simulated view calls (free, they
 * change nothing, and need no funded account). Everything else is read straight
 * from contract storage with `getLedgerEntries`, because the account exports only
 * `__check_auth` and the registry's views return ids, not records.
 *
 * Writes. The signer's G account is the transaction source, so it needs a few XLM
 * (FriendBot on testnet); there is no fee payer:
 *  1. build the call, simulate in record mode;
 *  2. check every recorded auth entry against the call we built ([AuthEntryGuard]);
 *  3. for the smart account's entry, fill the OZ `AuthPayload` naming the signer as
 *     a `Delegated` signer, and add the source-account entry
 *     `account.__check_auth(digest)` that the envelope signature authorizes (the
 *     scheme proven on testnet by `scripts/sign-delegated.mjs`);
 *  4. simulate again enforcing auth, so `__check_auth` and the policy run before
 *     anything is signed. A contract refusal ends here as [SubmitResult.Rejected];
 *  5. the signer signs the transaction hash; send; poll until final or until the
 *     time bounds have provably closed.
 * The key never signs anything but the hash of a transaction this class built.
 */
class KmpStellarGateway(
    private val network: GuardPayNetwork,
    private val server: SorobanServer = SorobanServer(network.rpcUrl),
) : StellarGateway {
    private val stellarNetwork = Network.TESTNET

    // --- reads ------------------------------------------------------------------

    override suspend fun latestLedgerTime(): LedgerTime = reading("latest ledger") { LedgerTime(latestLedger().closeTime) }

    override suspend fun readBalance(account: StellarAddress): UsdcAmount =
        ChainDecoders.amount(view(network.usdc, ContractAbi.TOKEN_BALANCE, account.scVal()))

    override suspend fun readDailySpent(account: StellarAddress): UsdcAmount =
        ChainDecoders.amount(view(network.guardianHold, ContractAbi.POLICY_SPENT_TODAY, account.scVal()))

    override suspend fun readAccountRules(account: StellarAddress): AccountRules {
        val (configVal, guardianVal) = entries(policyConfigKey(account), dataKey(network.holdRegistry, key(Storage.REGISTRY_GUARDIAN)))
        val config = policyConfig(account, configVal)
        return AccountRules(
            guardian = ChainDecoders.address(guardianVal ?: throw ChainReadException("the registry has no guardian")),
            dailyCap = config.dailyCap,
            holdDurationSeconds = ContractAbi.HOLD_DURATION_SECS,
            expiryWindowSeconds = null,
        )
    }

    override suspend fun readTrustedContacts(account: StellarAddress): List<TrustedContact> =
        ChainDecoders.contacts(policyConfig(account, entries(policyConfigKey(account)).single()).trustedContacts)

    /**
     * Every record the registry holds for [account], oldest first. Ids run from 0 to
     * `NextId - 1` and records are never deleted, so a missing one (archived, or a
     * lying RPC) fails the read instead of silently shrinking the list.
     */
    override suspend fun readHolds(account: StellarAddress): List<HeldPayment> {
        val next = entries(dataKey(network.holdRegistry, key(Storage.REGISTRY_NEXT_ID, account.scVal()))).single()
        val count = next?.let(ChainDecoders::u64) ?: 0L
        return (0 until count).chunked(MAX_KEYS_PER_READ).flatMap { ids ->
            val keys = ids.map { dataKey(network.holdRegistry, key(Storage.REGISTRY_HOLD, account.scVal(), Scv.toUint64(it.toULong()))) }
            entries(*keys.toTypedArray()).zip(ids).map { (v, id) ->
                ChainDecoders.hold(id, v ?: throw ChainReadException("hold $id is missing from the registry"), account, network.usdc)
            }
        }
    }

    /** The account's rule count, plus the signers and policies of its rule, read from OZ storage. */
    override suspend fun readAccountShape(account: StellarAddress): AccountShape {
        val instanceKey = LedgerKeyXdr.ContractData(
            LedgerKeyContractDataXdr(Address(account.value).toSCAddress(), Scv.toLedgerKeyContractInstance(), ContractDataDurabilityXdr.PERSISTENT),
        )
        val ruleId = network.contextRuleIds.first()
        val (instance, rule) = entries(instanceKey, dataKey(account, key(Storage.ACCOUNT_RULE, Scv.toUint32(ruleId))))
        val count = ChainDecoders.u32(
            ChainDecoders.instanceValue(instance ?: throw ChainReadException("the account has no instance"), Storage.ACCOUNT_RULE_COUNT),
        )
        val (signerIds, policyIds) = ChainDecoders.ruleIds(rule ?: throw ChainReadException("the account has no rule $ruleId"))
        val keys = signerIds.map { dataKey(account, key(Storage.ACCOUNT_SIGNER, Scv.toUint32(it.toUInt()))) } +
            policyIds.map { dataKey(account, key(Storage.ACCOUNT_POLICY, Scv.toUint32(it.toUInt()))) }
        val values = entries(*keys.toTypedArray()).map { it ?: throw ChainReadException("the account rule references a missing entry") }
        return AccountShape(
            contextRuleCount = count,
            signers = values.take(signerIds.size).map(ChainDecoders::signerEntry),
            policies = values.drop(signerIds.size).map(ChainDecoders::policyEntry),
        )
    }

    /** The policy config, checked against the contracts this app was built for. */
    private fun policyConfig(account: StellarAddress, v: SCValXdr?): PolicyConfig {
        val config = ChainDecoders.policyConfig(v ?: throw ChainReadException("${account.short()} has no GuardianHold config"))
        if (config.usdc != network.usdc || config.registry != network.holdRegistry) {
            throw ChainDecodeException("the account's policy points at another token or registry")
        }
        return config
    }

    private fun policyConfigKey(account: StellarAddress) = dataKey(network.guardianHold, key(Storage.POLICY_CONFIG, account.scVal()))

    /** The stored values for [keys], in order; null where the entry does not exist. */
    private suspend fun entries(vararg keys: LedgerKeyXdr): List<SCValXdr?> = reading("contract storage") {
        val found = server.getLedgerEntries(keys.toList()).entries.orEmpty().associate { e ->
            val data = e.parseXdr() as? LedgerEntryDataXdr.ContractData ?: throw ChainDecodeException("ledger entry is not contract data")
            hex(e.parseKey()) to data.value.`val`
        }
        keys.map { found[hex(it)] }
    }

    private suspend fun view(contract: StellarAddress, function: String, vararg args: SCValXdr): SCValXdr =
        reading("$function on ${contract.short()}") {
            val tx = buildTx(Account(SIMULATION_SOURCE, 0L), invoke(contract, function, args.toList()), validUntil = null)
            val sim = server.simulateTransaction(tx)
            sim.error?.let { throw ChainReadException("$function failed: $it") }
            sim.results?.firstOrNull()?.parseXdr() ?: throw ChainReadException("$function returned no value")
        }

    private inline fun <T> reading(what: String, block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ChainReadException) {
        throw e
    } catch (e: ChainDecodeException) {
        throw e
    } catch (e: Exception) {
        throw ChainReadException("could not read $what: ${e.message}", e)
    }

    // --- writes -----------------------------------------------------------------

    override suspend fun submitTransfer(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult =
        submit(account, owner) {
            invoke(network.usdc, ContractAbi.TOKEN_TRANSFER, listOf(account.scVal(), intent.destination.scVal(), intent.amount.scVal()))
        }

    override suspend fun submitQueue(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult =
        submit(account, owner) {
            invoke(
                network.holdRegistry,
                ContractAbi.REGISTRY_QUEUE,
                listOf(account.scVal(), network.usdc.scVal(), intent.destination.scVal(), intent.amount.scVal()),
            )
        }

    /**
     * `cancel(account, id)` requires the registry guardian's auth. The guardian's G
     * account is the source, so its envelope signature is that auth; any other
     * signer is refused by the registry during simulation.
     */
    override suspend fun submitCancel(account: StellarAddress, holdId: Long, signer: Signer): SubmitResult =
        submit(account, signer) {
            require(holdId >= 0) { "hold id must not be negative" }
            invoke(network.holdRegistry, ContractAbi.REGISTRY_CANCEL, listOf(account.scVal(), Scv.toUint64(holdId.toULong())))
        }

    private suspend fun submit(
        account: StellarAddress,
        signer: Signer,
        call: suspend () -> InvokeHostFunctionOperation,
    ): SubmitResult {
        val hash: TxHash
        val validUntil: Long
        val tx: Transaction
        try {
            val op = call()
            val expected = (op.hostFunction as HostFunctionXdr.InvokeContract).value
            val signerId = accountIdOf(signer)
            val source = server.getAccount(signerId)
            val sequence = source.sequenceNumber
            val ledger = latestLedger()
            // Validity comes from ledger time, never the phone clock.
            validUntil = ledger.closeTime + TX_VALIDITY_SECONDS
            val recorded = server.simulateTransaction(buildTx(source, op, validUntil))
            recorded.error?.let { return SubmitClassifier.simulationFailed(it) }
            val expiration = (ledger.sequence + AUTH_VALIDITY_LEDGERS).toUInt()
            val auth = recorded.results?.firstOrNull()?.parseAuth().orEmpty()
                .flatMap { authorize(it, expected, account, signerId, expiration) }

            source.setSequenceNumber(sequence) // building consumed one; the real tx needs the same
            val unsigned = buildTx(source, InvokeHostFunctionOperation(op.hostFunction, auth), validUntil)
            val enforced = server.simulateTransaction(
                unsigned,
                SimulateTransactionRequest.ResourceConfig(INSTRUCTION_LEEWAY),
                SimulateTransactionRequest.AuthMode.ENFORCE,
            )
            enforced.error?.let { return SubmitClassifier.simulationFailed(it) }
            tx = server.prepareTransaction(unsigned, enforced)
            // A hostile RPC could inflate the resource fee to drain the signer's XLM.
            if (tx.fee > MAX_FEE_STROOPS) return SubmitResult.NetworkFailure("fee ${tx.fee} above the cap")
            val txHash = tx.hash()
            val signature = signWith(signer, txHash) ?: return SubmitResult.SigningFailed
            (tx.signatures as MutableList<DecoratedSignature>).add(DecoratedSignature(signer.publicKey.copyOfRange(28, 32), signature))
            hash = TxHash(tx.hashHex().lowercase())
        } catch (e: SigningCancelledException) {
            return SubmitResult.SigningCancelled
        } catch (e: UnexpectedAuthException) {
            // The call needs an authorization this signer cannot give (e.g. the owner
            // trying to cancel: only the guardian may). Refused before sending.
            return SubmitResult.Rejected(null, null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Nothing has been handed to the network yet.
            return SubmitResult.NetworkFailure(e.message ?: e::class.simpleName ?: "error")
        }

        // From here the transaction may land: only polling decides.
        try {
            val sent = server.sendTransaction(tx)
            when (val next = SubmitClassifier.afterSend(sent.status, hash)) {
                is SubmitClassifier.AfterSend.Done -> return next.result
                is SubmitClassifier.AfterSend.Poll -> Unit
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The send may have reached the network before failing. Poll.
        }
        return awaitFinal(hash, validUntil)
    }

    private suspend fun awaitFinal(hash: TxHash, validUntil: Long): SubmitResult {
        var lastError: Throwable? = null
        repeat(MAX_POLLS) {
            val step = try {
                val r = server.getTransaction(hash.hex)
                SubmitClassifier.afterPoll(r.status, hash, r.latestLedgerCloseTime, validUntil)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                SubmitClassifier.PollStep.Again
            }
            if (step is SubmitClassifier.PollStep.Done) return step.result
            delay(POLL_INTERVAL_MS)
        }
        throw SubmissionOutcomeUnknownException(hash, lastError)
    }

    /**
     * Checks one recorded auth entry and returns what goes in the transaction. A
     * source-account entry is kept as is: the envelope signature covers it. The
     * smart account's entry gets the delegated-signer payload plus its companion
     * `__check_auth(digest)` entry. Anything else is refused before signing.
     */
    private suspend fun authorize(
        entry: SorobanAuthorizationEntryXdr,
        expected: InvokeContractArgsXdr,
        account: StellarAddress,
        signerId: String,
        expiration: UInt,
    ): List<SorobanAuthorizationEntryXdr> {
        if (!AuthEntryGuard.matchesCall(entry.rootInvocation, expected)) {
            throw UnexpectedAuthException("auth entry for a call the app did not build")
        }
        // Newer RPCs record `AddressV2` (its payload hash also binds the address).
        // Delegates are never expected: the account authorizes on its own.
        val credentials = when (val c = entry.credentials) {
            is SorobanCredentialsXdr.Void -> return listOf(entry)
            is SorobanCredentialsXdr.Address -> c.value
            is SorobanCredentialsXdr.AddressV2 -> c.value
            else -> throw UnexpectedAuthException("credential type ${c::class.simpleName}")
        }
        val address = Address.fromSCAddress(credentials.address).getEncodedAddress()
        if (!AuthEntryGuard.isAccount(address, account)) {
            throw UnexpectedAuthException("auth requested for ${address.take(4)}…, not this account")
        }
        val withExpiration = entry.copy(
            credentials = rewrap(entry.credentials, credentials.copy(signatureExpirationLedger = Uint32Xdr(expiration))),
        )
        val payloadHash = SmartAccountAuth.buildAuthPayloadHash(withExpiration, expiration, stellarNetwork.networkPassphrase)
        val digest = SmartAccountAuth.buildAuthDigest(payloadHash, network.contextRuleIds)
        val delegated = entry.copy(
            credentials = rewrap(
                entry.credentials,
                credentials.copy(signatureExpirationLedger = Uint32Xdr(expiration), signature = authPayload(signerId)),
            ),
        )
        val checkAuth = (invoke(account, ContractAbi.ACCOUNT_CHECK_AUTH, listOf(Scv.toBytes(digest))).hostFunction as HostFunctionXdr.InvokeContract).value
        val companion = SorobanAuthorizationEntryXdr(
            SorobanCredentialsXdr.Void,
            SorobanAuthorizedInvocationXdr(SorobanAuthorizedFunctionXdr.ContractFn(checkAuth), emptyList()),
        )
        return listOf(delegated, companion)
    }

    /** Same credential variant as [original], with [updated] inside. */
    private fun rewrap(original: SorobanCredentialsXdr, updated: SorobanAddressCredentialsXdr): SorobanCredentialsXdr =
        when (original) {
            is SorobanCredentialsXdr.AddressV2 -> SorobanCredentialsXdr.AddressV2(updated)
            else -> SorobanCredentialsXdr.Address(updated)
        }

    /** OZ `AuthPayload { context_rule_ids, signers: { Delegated(signer): b"" } }`, keys in sorted order. */
    private fun authPayload(signerId: String): SCValXdr = Scv.toMap(
        linkedMapOf(
            Scv.toSymbol(AuthPayload.CONTEXT_RULE_IDS) to Scv.toVec(network.contextRuleIds.map { Scv.toUint32(it) }),
            Scv.toSymbol(AuthPayload.SIGNERS) to Scv.toMap(
                linkedMapOf(
                    Scv.toVec(listOf(Scv.toSymbol(AuthPayload.DELEGATED), Address(signerId).toSCVal())) to Scv.toBytes(ByteArray(0)),
                ),
            ),
        ),
    )

    // --- helpers ----------------------------------------------------------------

    private data class Ledger(val sequence: Long, val closeTime: Long)

    private suspend fun latestLedger(): Ledger {
        val r = server.getLatestLedger()
        return Ledger(r.sequence, r.closeTime ?: throw ChainReadException("RPC returned no ledger close time"))
    }

    private fun buildTx(source: TransactionBuilderAccount, op: InvokeHostFunctionOperation, validUntil: Long?): Transaction {
        val builder = TransactionBuilder(source, stellarNetwork)
            .setBaseFee(AbstractTransaction.MIN_BASE_FEE)
            .addOperation(op)
        if (validUntil == null) builder.setTimeout(READ_TIMEOUT_SECONDS) else builder.addTimeBounds(TimeBounds(0, validUntil))
        return builder.build()
    }

    private fun invoke(contract: StellarAddress, function: String, args: List<SCValXdr>) =
        InvokeHostFunctionOperation.invokeContractFunction(
            contractAddress = contract.value,
            functionName = function,
            parameters = args,
        )

    /** A persistent `DataKey` entry of [contract]. */
    private fun dataKey(contract: StellarAddress, key: SCValXdr): LedgerKeyXdr =
        LedgerKeyXdr.ContractData(LedgerKeyContractDataXdr(Address(contract.value).toSCAddress(), key, ContractDataDurabilityXdr.PERSISTENT))

    /** A `#[contracttype]` enum variant: `vec[Symbol(name), fields…]`. */
    private fun key(variant: String, vararg fields: SCValXdr): SCValXdr = Scv.toVec(listOf(Scv.toSymbol(variant)) + fields)

    private fun hex(key: LedgerKeyXdr): String =
        XdrWriter().also { key.encode(it) }.toByteArray().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private fun accountIdOf(signer: Signer): String = KeyPair.fromPublicKey(signer.publicKey).getAccountId()

    private fun StellarAddress.scVal(): SCValXdr = Address(value).toSCVal()

    private fun UsdcAmount.scVal(): SCValXdr = Scv.toInt128(BigInteger.fromLong(units))

    private companion object {
        /** All-zero account id: a simulation needs a source, not an existing one. */
        const val SIMULATION_SOURCE = "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF"
        const val TX_VALIDITY_SECONDS = 60L
        const val AUTH_VALIDITY_LEDGERS = 60L // ~5 minutes
        const val READ_TIMEOUT_SECONDS = 30L
        const val MAX_FEE_STROOPS = 20_000_000L // 2 XLM
        const val POLL_INTERVAL_MS = 2_000L
        const val MAX_KEYS_PER_READ = 100

        /** Extra instructions for `__check_auth` + policy + registry, as in `sign-delegated.mjs`. */
        const val INSTRUCTION_LEEWAY = 8_000_000L

        // Enough to outlast the validity window and then some, on a responsive RPC.
        const val MAX_POLLS = 90
    }
}
