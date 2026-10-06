package com.guardpay.shared.stellar

import com.soneso.stellar.sdk.AbstractTransaction
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.InvokeHostFunctionOperation
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.Network
import com.soneso.stellar.sdk.TransactionBuilder
import com.soneso.stellar.sdk.TransactionBuilderAccount
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.rpc.requests.SimulateTransactionRequest
import com.soneso.stellar.sdk.rpc.responses.GetTransactionStatus
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.smartaccount.core.SmartAccountAuth
import com.soneso.stellar.sdk.xdr.HostFunctionXdr
import com.soneso.stellar.sdk.xdr.SCValXdr
import com.soneso.stellar.sdk.xdr.SorobanAddressCredentialsXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizationEntryXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedFunctionXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedInvocationXdr
import com.soneso.stellar.sdk.xdr.SorobanCredentialsXdr
import com.soneso.stellar.sdk.xdr.Uint32Xdr
import com.soneso.stellar.sdk.xdr.XdrWriter

/**
 * An attacker that bypasses the app and sends a transaction anyway, so a rejection
 * is decided by the contracts on testnet and leaves a transaction hash (GH-01,
 * GH-24). The app itself never gets this far: [KmpStellarGateway] stops at the
 * enforcing simulation.
 *
 * Resources come from simulating the same call as the owner would sign it (a
 * simulation checks no signatures). The attacker then swaps the account's auth
 * payload for its own and signs as the transaction source.
 */
internal class Adversary(private val server: SorobanServer, private val ownerId: String) {
    /**
     * Sends [op] for [account] with an auth payload naming [payloadSigners] as
     * `Delegated` signers (empty: no signature at all), paid and signed by [attacker].
     * Returns the hash and whether the network applied it.
     */
    suspend fun send(
        op: InvokeHostFunctionOperation,
        account: String,
        attacker: KeyPair,
        payloadSigners: List<String>,
    ): Pair<String, GetTransactionStatus> {
        val ownerSource = server.getAccount(ownerId)
        val recorded = server.simulateTransaction(build(ownerSource, op))
        check(recorded.error == null) { "the legitimate call does not simulate: ${recorded.error}" }
        val entry = recorded.results!!.single().parseAuth()!!.single {
            val c = it.credentials.addressOrNull()
            c != null && Address.fromSCAddress(c.address).getEncodedAddress() == account
        }
        val credentials = entry.credentials.addressOrNull()!!
        fun wrap(c: SorobanAddressCredentialsXdr) =
            if (entry.credentials is SorobanCredentialsXdr.AddressV2) SorobanCredentialsXdr.AddressV2(c) else SorobanCredentialsXdr.Address(c)
        val expiration = (server.getLatestLedger().sequence + 60).toUInt()
        val stamped = entry.copy(credentials = wrap(credentials.copy(signatureExpirationLedger = Uint32Xdr(expiration))))
        val digest = SmartAccountAuth.buildAuthDigest(
            SmartAccountAuth.buildAuthPayloadHash(stamped, expiration, Network.TESTNET.networkPassphrase),
            RULES,
        )
        fun withSigners(signers: List<String>) = listOf(
            entry.copy(credentials = wrap(credentials.copy(signatureExpirationLedger = Uint32Xdr(expiration), signature = payload(signers)))),
        ) + if (signers.isEmpty()) emptyList() else listOf(checkAuth(account, digest))

        // The owner's version, only to learn the footprint and fees.
        ownerSource.setSequenceNumber(ownerSource.sequenceNumber - 1)
        val legit = server.simulateTransaction(
            build(ownerSource, InvokeHostFunctionOperation(op.hostFunction, withSigners(listOf(ownerId)))),
            SimulateTransactionRequest.ResourceConfig(8_000_000L),
            SimulateTransactionRequest.AuthMode.ENFORCE,
        )
        check(legit.error == null) { "the owner's version does not simulate: ${legit.error}" }

        val attackAuth = withSigners(payloadSigners)
        val attack = build(server.getAccount(attacker.getAccountId()), InvokeHostFunctionOperation(op.hostFunction, attackAuth))
        val tx = server.prepareTransaction(attack, legit)
        val sentAuth = (tx.operations.single() as InvokeHostFunctionOperation).auth
        // XDR bytes, since the generated types hold ByteArrays (compared by reference).
        check(sentAuth.map(::xdr) == attackAuth.map(::xdr)) { "prepare replaced the attacker's auth" }
        tx.sign(attacker)
        val hash = server.sendTransaction(tx).hash ?: error("no hash")
        return hash to server.pollTransaction(hash, maxAttempts = 60).status
    }

    private fun SorobanCredentialsXdr.addressOrNull() = when (this) {
        is SorobanCredentialsXdr.Address -> value
        is SorobanCredentialsXdr.AddressV2 -> value
        else -> null
    }

    private fun xdr(e: SorobanAuthorizationEntryXdr): List<Byte> = XdrWriter().also { e.encode(it) }.toByteArray().toList()

    private fun build(source: TransactionBuilderAccount, op: InvokeHostFunctionOperation) =
        TransactionBuilder(source, Network.TESTNET).setBaseFee(AbstractTransaction.MIN_BASE_FEE).addOperation(op).setTimeout(120).build()

    private fun payload(signers: List<String>): SCValXdr = Scv.toMap(
        linkedMapOf(
            Scv.toSymbol("context_rule_ids") to Scv.toVec(RULES.map { Scv.toUint32(it) }),
            Scv.toSymbol("signers") to Scv.toMap(
                linkedMapOf<SCValXdr, SCValXdr>().apply {
                    signers.sorted().forEach { put(Scv.toVec(listOf(Scv.toSymbol("Delegated"), Address(it).toSCVal())), Scv.toBytes(ByteArray(0))) }
                },
            ),
        ),
    )

    private fun checkAuth(account: String, digest: ByteArray): SorobanAuthorizationEntryXdr {
        val fn = (InvokeHostFunctionOperation.invokeContractFunction(account, "__check_auth", listOf(Scv.toBytes(digest))).hostFunction as HostFunctionXdr.InvokeContract).value
        return SorobanAuthorizationEntryXdr(
            SorobanCredentialsXdr.Void,
            SorobanAuthorizedInvocationXdr(SorobanAuthorizedFunctionXdr.ContractFn(fn), emptyList()),
        )
    }

    private companion object {
        val RULES = listOf(0u)
    }
}
