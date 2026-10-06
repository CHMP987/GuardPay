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
import com.guardpay.shared.stellar.ProvisionalAbi.POLICY_CONFIG
import com.guardpay.shared.stellar.ProvisionalAbi.POLICY_CONTACTS
import com.guardpay.shared.stellar.ProvisionalAbi.POLICY_SPENT_TODAY
import com.guardpay.shared.stellar.ProvisionalAbi.REGISTRY_CANCEL
import com.guardpay.shared.stellar.ProvisionalAbi.REGISTRY_HOLDS_OF
import com.guardpay.shared.stellar.ProvisionalAbi.REGISTRY_QUEUE
import com.guardpay.shared.stellar.ProvisionalAbi.TOKEN_BALANCE
import com.guardpay.shared.stellar.ProvisionalAbi.TOKEN_TRANSFER
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.AbstractTransaction
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.Auth
import com.soneso.stellar.sdk.InvokeHostFunctionOperation
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.Network
import com.soneso.stellar.sdk.TimeBounds
import com.soneso.stellar.sdk.Transaction
import com.soneso.stellar.sdk.TransactionBuilder
import com.soneso.stellar.sdk.crypto.getSha256Crypto
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.rpc.exception.PrepareTransactionException
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.smartaccount.core.Ed25519Signature
import com.soneso.stellar.sdk.smartaccount.core.ExternalSigner
import com.soneso.stellar.sdk.smartaccount.core.SmartAccountAuth
import com.soneso.stellar.sdk.xdr.HostFunctionXdr
import com.soneso.stellar.sdk.xdr.InvokeContractArgsXdr
import com.soneso.stellar.sdk.xdr.SCValXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizationEntryXdr
import com.soneso.stellar.sdk.xdr.SorobanCredentialsXdr
import com.soneso.stellar.sdk.xdr.XdrWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * [StellarGateway] over stellar-sdk 1.14.0 and Soroban RPC. Testnet only.
 *
 * SKELETON (day 3): transport, signing, checks and decoding are real; the
 * contract function names in [ProvisionalAbi] are placeholders until
 * INTERFACES.md, and the addresses arrive at Sync 2 (day 4).
 *
 * Reads simulate view functions: free, and they change nothing.
 * Writes: build -> simulate (records the auth entries) -> check each entry against
 * the call we built and sign it -> prepare (simulates again with the signatures,
 * so `__check_auth` and the policy run) -> fee payer signs -> send -> poll until
 * a final status or until the time bounds have provably closed.
 *
 * [feePayer] is a plain G account on this device that pays fees and owns nothing;
 * where its key lives is decided in P6. It never authorizes a contract call.
 */
class KmpStellarGateway(
    private val network: GuardPayNetwork,
    private val feePayer: KeyPair,
    private val server: SorobanServer = SorobanServer(network.rpcUrl),
) : StellarGateway {
    private val stellarNetwork = Network.TESTNET

    // --- reads ------------------------------------------------------------------

    override suspend fun latestLedgerTime(): LedgerTime = reading("latest ledger") { LedgerTime(latestLedger().closeTime) }

    override suspend fun readBalance(account: StellarAddress): UsdcAmount =
        ChainDecoders.amount(view(network.usdc, TOKEN_BALANCE, account.scVal()))

    override suspend fun readAccountRules(account: StellarAddress): AccountRules =
        ChainDecoders.accountRules(view(network.guardianHold, POLICY_CONFIG, account.scVal()))

    override suspend fun readTrustedContacts(account: StellarAddress): List<TrustedContact> =
        ChainDecoders.contacts(view(network.guardianHold, POLICY_CONTACTS, account.scVal()))

    override suspend fun readDailySpent(account: StellarAddress): UsdcAmount =
        ChainDecoders.amount(view(network.guardianHold, POLICY_SPENT_TODAY, account.scVal()))

    override suspend fun readHolds(account: StellarAddress): List<HeldPayment> {
        val guardian = readAccountRules(account).guardian
        val records = view(network.holdRegistry, REGISTRY_HOLDS_OF, account.scVal())
        return ChainDecoders.holds(records, account, network.usdc, guardian)
    }

    // No view function to name yet: OZ's context-rule getters on pin b40c5ea and the
    // account's exact ABI arrive with INTERFACES.md. Fail loudly instead of guessing.
    override suspend fun readAccountShape(account: StellarAddress): AccountShape =
        throw ChainReadException("account shape: no ABI until INTERFACES.md")

    private suspend fun view(contract: StellarAddress, function: String, vararg args: SCValXdr): SCValXdr =
        reading("$function on ${contract.short()}") {
            val sim = server.simulateTransaction(buildTx(invoke(contract, function, args.toList()), validUntil = null))
            sim.error?.let { throw ChainReadException("$function failed: $it") }
            sim.results?.firstOrNull()?.parseXdr() ?: throw ChainReadException("$function returned no value")
        }

    private inline fun <T> reading(what: String, block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ChainReadException) {
        throw e
    } catch (e: Exception) {
        throw ChainReadException("could not read $what: ${e.message}", e)
    }

    // --- writes -----------------------------------------------------------------

    override suspend fun submitTransfer(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult =
        submit(account, owner) {
            invoke(network.usdc, TOKEN_TRANSFER, listOf(account.scVal(), intent.destination.scVal(), intent.amount.scVal()))
        }

    override suspend fun submitQueue(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult =
        submit(account, owner) {
            invoke(
                network.holdRegistry,
                REGISTRY_QUEUE,
                listOf(account.scVal(), network.usdc.scVal(), intent.destination.scVal(), intent.amount.scVal()),
            )
        }

    /**
     * The caller is the guardian's G account when [signer] holds the guardian key,
     * otherwise the account itself (the owner cancelling through the policy).
     */
    override suspend fun submitCancel(account: StellarAddress, holdId: Long, signer: Signer): SubmitResult =
        submit(account, signer) {
            require(holdId >= 0) { "hold id must not be negative" }
            val guardian = readAccountRules(account).guardian
            val caller = if (guardian.value == accountIdOf(signer)) guardian else account
            invoke(network.holdRegistry, REGISTRY_CANCEL, listOf(caller.scVal(), Scv.toUint64(holdId.toULong())))
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
            val (sequence, closeTime) = latestLedger()
            // Validity comes from ledger time, never the phone clock.
            validUntil = closeTime + TX_VALIDITY_SECONDS
            val recorded = server.simulateTransaction(buildTx(op, validUntil))
            recorded.error?.let { return SubmitClassifier.simulationFailed(it) }
            val entries = recorded.results?.firstOrNull()?.parseAuth().orEmpty()
            val expiration = sequence + AUTH_VALIDITY_LEDGERS
            val signed = entries.map { authorize(it, expected, account, signer, expiration) }
            tx = try {
                server.prepareTransaction(buildTx(InvokeHostFunctionOperation(op.hostFunction, signed), validUntil))
            } catch (e: PrepareTransactionException) {
                return SubmitClassifier.simulationFailed(e.simulationError ?: e.message)
            }
            // A hostile RPC could inflate the resource fee to drain the fee payer.
            if (tx.fee > MAX_FEE_STROOPS) return SubmitResult.NetworkFailure("fee ${tx.fee} above the cap")
            tx.sign(feePayer)
            hash = TxHash(tx.hashHex().lowercase())
        } catch (e: SigningCancelledException) {
            return SubmitResult.SigningCancelled
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

    /** Checks one recorded auth entry and signs it, or refuses it. */
    private suspend fun authorize(
        entry: SorobanAuthorizationEntryXdr,
        expected: InvokeContractArgsXdr,
        account: StellarAddress,
        signer: Signer,
        expiration: Long,
    ): SorobanAuthorizationEntryXdr {
        val scAddress = when (val c = entry.credentials) {
            is SorobanCredentialsXdr.Address -> c.value.address
            is SorobanCredentialsXdr.AddressV2 -> c.value.address
            // Source-account entries are authorized by the fee payer's envelope signature: never expected.
            else -> throw UnexpectedAuthException("credential type ${c::class.simpleName}")
        }
        if (!AuthEntryGuard.matchesCall(entry.rootInvocation, expected)) {
            throw UnexpectedAuthException("auth entry for a call the app did not build")
        }
        val address = Address.fromSCAddress(scAddress).getEncodedAddress()
        val signerId = accountIdOf(signer)
        return when (AuthEntryGuard.role(address, account, signerId)) {
            AuthEntryGuard.Role.SmartAccount -> {
                val exp = expiration.toUInt()
                val payload = SmartAccountAuth.buildAuthPayloadHash(entry, exp, stellarNetwork.networkPassphrase)
                val digest = SmartAccountAuth.buildAuthDigest(payload, network.contextRuleIds)
                SmartAccountAuth.signAuthEntry(
                    entry = entry,
                    signer = ExternalSigner.ed25519(network.ed25519Verifier.value, signer.publicKey),
                    signature = Ed25519Signature(publicKey = signer.publicKey, signature = signer.signAuthDigest(digest)),
                    expirationLedger = exp,
                    contextRuleIds = network.contextRuleIds,
                )
            }
            AuthEntryGuard.Role.ClassicAccount -> Auth.authorizeEntry(
                entry,
                object : Auth.Signer {
                    override suspend fun sign(preimage: com.soneso.stellar.sdk.xdr.HashIDPreimageXdr): Auth.Signature {
                        val bytes = XdrWriter().also { preimage.encode(it) }.toByteArray()
                        return Auth.Signature(signerId, signer.signAuthDigest(getSha256Crypto().hash(bytes)))
                    }
                },
                expiration,
                stellarNetwork,
            )
            null -> throw UnexpectedAuthException("auth requested for ${address.take(4)}…, not this account or signer")
        }
    }

    // --- helpers ----------------------------------------------------------------

    private data class Ledger(val sequence: Long, val closeTime: Long)

    private suspend fun latestLedger(): Ledger {
        val r = server.getLatestLedger()
        return Ledger(r.sequence, r.closeTime ?: throw ChainReadException("RPC returned no ledger close time"))
    }

    private suspend fun buildTx(op: InvokeHostFunctionOperation, validUntil: Long?): Transaction {
        val builder = TransactionBuilder(server.getAccount(feePayer.getAccountId()), stellarNetwork)
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

    private fun accountIdOf(signer: Signer): String = KeyPair.fromPublicKey(signer.publicKey).getAccountId()

    private fun StellarAddress.scVal(): SCValXdr = Address(value).toSCVal()

    private fun UsdcAmount.scVal(): SCValXdr = Scv.toInt128(BigInteger.fromLong(units))

    private companion object {
        const val TX_VALIDITY_SECONDS = 60L
        const val AUTH_VALIDITY_LEDGERS = 60L // ~5 minutes
        const val READ_TIMEOUT_SECONDS = 30L
        const val MAX_FEE_STROOPS = 20_000_000L // 2 XLM
        const val POLL_INTERVAL_MS = 2_000L

        // Enough to outlast the validity window and then some, on a responsive RPC.
        const val MAX_POLLS = 90
    }
}
