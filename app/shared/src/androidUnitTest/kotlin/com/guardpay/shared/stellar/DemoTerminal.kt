package com.guardpay.shared.stellar

import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.stellar.TestnetProvisioner.Companion.UNIT
import com.guardpay.shared.stellar.TestnetProvisioner.Companion.addr
import com.guardpay.shared.stellar.TestnetProvisioner.Companion.i128
import com.soneso.stellar.sdk.AbstractTransaction
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.InvokeHostFunctionOperation
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.Network
import com.soneso.stellar.sdk.Transaction
import com.soneso.stellar.sdk.TransactionBuilder
import com.soneso.stellar.sdk.rpc.SorobanDataBuilder
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.rpc.requests.SimulateTransactionRequest
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.smartaccount.core.SmartAccountAuth
import com.soneso.stellar.sdk.xdr.ContractDataDurabilityXdr
import com.soneso.stellar.sdk.xdr.HashXdr
import com.soneso.stellar.sdk.xdr.HostFunctionXdr
import com.soneso.stellar.sdk.xdr.Int64Xdr
import com.soneso.stellar.sdk.xdr.LedgerKeyContractCodeXdr
import com.soneso.stellar.sdk.xdr.LedgerKeyContractDataXdr
import com.soneso.stellar.sdk.xdr.LedgerKeyXdr
import com.soneso.stellar.sdk.xdr.SCNonceKeyXdr
import com.soneso.stellar.sdk.xdr.SCValXdr
import com.soneso.stellar.sdk.xdr.SorobanAddressCredentialsXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizationEntryXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedFunctionXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizedInvocationXdr
import com.soneso.stellar.sdk.xdr.SorobanCredentialsXdr
import com.soneso.stellar.sdk.xdr.Uint32Xdr
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

/**
 * The terminal half of the demo (plan §13, scenes 2, 4, 5 and 6) on Stellar TESTNET,
 * for the timed rehearsal. Not a test: run it with `java -cp` next to the emulator
 * (Gradle and the emulator do not fit in RAM together). It reads one scene number
 * per line on stdin and prints "> listo" after each.
 *
 * It provisions its own throwaway account ([TestnetProvisioner]); the owner,
 * guardian and stranger keys exist only in this process and are never printed. The
 * phones' keys stay in their Keystores, so this is not the phones' account.
 *
 * Calls the contracts refuse do not simulate, so they are sent anyway with a
 * footprint written by hand (as `scripts/attacks/invoke.mjs` does): the rejection
 * is then decided on chain and leaves a hash.
 */
fun main() = runBlocking {
    val server = SorobanServer(TestnetProvisioner.RPC)
    val owner = KeyPair.random()
    val guardian = KeyPair.random()
    say("preparando una cuenta de testnet nueva (claves solo en memoria)…")
    val p = TestnetProvisioner(server) { say(it) }.provision(owner.getAccountId(), guardian.getAccountId(), contacts = 1, strangers = 1)
    val t = DemoTerminal(server, p.config, owner, guardian, p.strangers.single())
    t.intro()
    say("> listo")
    while (true) {
        val line = readlnOrNull()?.trim() ?: break
        when (line) {
            "2" -> t.scene2()
            "4" -> t.scene4()
            "5" -> t.scene5()
            "6" -> t.scene6()
            "q" -> break
            else -> say("escena desconocida: $line")
        }
        say("> listo")
    }
}

private fun say(msg: String) {
    println(msg)
    System.out.flush()
}

internal class DemoTerminal(
    private val server: SorobanServer,
    private val c: TestnetConfig,
    private val owner: KeyPair,
    private val guardian: KeyPair,
    private val stranger: String,
) {
    private val gw = KmpStellarGateway(c.network(), server)
    private val account = StellarAddress(c.account)
    private var heldId: Long? = null

    suspend fun intro() {
        val shape = gw.readAccountShape(account)
        say("cuenta     ${c.account}")
        say("registro   ${c.holdRegistry}")
        say("dueña      ${c.owner}")
        say("guardián   ${c.guardian}")
        say("extraño    $stranger")
        say("reglas: ${shape.contextRuleCount}, firmantes: ${shape.signers.map { it.value.take(6) }}, guardián firmante: ${StellarAddress(c.guardian) in shape.signers}")
    }

    /** Scene 2: the owner holds 150 for a stranger, then tries to send it right away. */
    suspend fun scene2() {
        say("[escena 2] la dueña retiene 150 USDC para el extraño (queue)")
        val queued = gw.submitQueue(account, PaymentIntent(StellarAddress(stranger), UsdcAmount(150 * UNIT)), signer(owner))
        say("  queue: ${describe(queued)}")
        heldId = gw.readHolds(account).last { it.status == HoldStatus.Held }.id
        say("  hold #$heldId en el registro: Retenido")
        say("[escena 2] la dueña firma el transfer de esos 150 ya, sin esperar")
        force(transfer(stranger, 150), owner)
    }

    /** Scene 4: the guardian stops that hold; the owner signs its transfer anyway. */
    suspend fun scene4() {
        val id = heldId ?: return say("primero la escena 2")
        say("[escena 4] el guardián detiene el hold #$id (cancel)")
        say("  cancel: ${describe(gw.submitCancel(account, id, signer(guardian)))}")
        say("  hold #$id: ${gw.readHolds(account).single { it.id == id }.status}")
        say("[escena 4] la dueña firma el transfer del pago detenido")
        force(transfer(stranger, 150), owner)
    }

    /** Scene 5: the guardian signs a transfer from the owner's account to himself. */
    suspend fun scene5() {
        say("[escena 5] el guardián firma un transfer de 20 USDC hacia sí mismo")
        force(transfer(c.guardian, 20), guardian)
    }

    /** Scene 6: `approve` signed by the owner; then a second rule, which has no function. */
    suspend fun scene6() {
        say("[escena 6] la dueña firma approve de 25 USDC al guardián")
        val expiration = Scv.toUint32((server.getLatestLedger().sequence + 1000).toUInt())
        force(InvokeHostFunctionOperation.invokeContractFunction(c.usdc, "approve", listOf(addr(c.account), addr(c.guardian), i128(25 * UNIT), expiration)), owner)
        say("[escena 6] add_context_rule en la cuenta")
        val op = InvokeHostFunctionOperation.invokeContractFunction(c.account, "add_context_rule", emptyList())
        val sim = server.simulateTransaction(build(server.getAccount(owner.getAccountId()), op, AbstractTransaction.MIN_BASE_FEE))
        val error = sim.error
        say(if (error == null) "  ¡la simulación pasó! (no se envía)" else "  simulación: ${firstLine(error)}")
        say("  no hay transacción: la función no existe")
        say("  reglas de la cuenta: ${gw.readAccountShape(account).contextRuleCount}")
    }

    private fun transfer(to: String, amount: Long) =
        InvokeHostFunctionOperation.invokeContractFunction(c.usdc, "transfer", listOf(addr(c.account), addr(to), i128(amount * UNIT)))

    /**
     * Sends [op] with an auth payload naming [signer] as the account's `Delegated`
     * signer; [signer] also pays and signs the transaction. Prints what the enforcing
     * simulation said, then the hash and result of the real send.
     */
    private suspend fun force(op: InvokeHostFunctionOperation, signer: KeyPair) {
        val fn = (op.hostFunction as HostFunctionXdr.InvokeContract).value
        val nonce = Random.nextLong(0, Long.MAX_VALUE)
        val expiration = (server.getLatestLedger().sequence + 60).toUInt()
        val credentials = SorobanAddressCredentialsXdr(Address(c.account).toSCAddress(), Int64Xdr(nonce), Uint32Xdr(expiration), Scv.toVoid())
        val invocation = SorobanAuthorizedInvocationXdr(SorobanAuthorizedFunctionXdr.ContractFn(fn), emptyList())
        val unsigned = SorobanAuthorizationEntryXdr(SorobanCredentialsXdr.Address(credentials), invocation)
        val digest = SmartAccountAuth.buildAuthDigest(
            SmartAccountAuth.buildAuthPayloadHash(unsigned, expiration, Network.TESTNET.networkPassphrase),
            RULES,
        )
        val auth = listOf(
            SorobanAuthorizationEntryXdr(SorobanCredentialsXdr.Address(credentials.copy(signature = payload(signer.getAccountId()))), invocation),
            checkAuth(digest),
        )
        val signed = InvokeHostFunctionOperation(op.hostFunction, auth)

        val sim = server.simulateTransaction(
            build(server.getAccount(signer.getAccountId()), signed, AbstractTransaction.MIN_BASE_FEE),
            SimulateTransactionRequest.ResourceConfig(10_000_000L),
            SimulateTransactionRequest.AuthMode.ENFORCE,
        )
        say("  simulación: ${sim.error?.let { contractCode(it)?.let { n -> "rechazo del contrato, código $n${NAMES[n]?.let { s -> " ($s)" } ?: ""}" } ?: firstLine(it) } ?: "pasaría"}")

        val tx = build(server.getAccount(signer.getAccountId()), signed, FEE)
        tx.sign(signer)
        val hash = server.sendTransaction(tx).hash ?: error("no hash")
        val result = server.pollTransaction(hash, maxAttempts = 60)
        // Testnet RPC returns no diagnostic events, so the contract code is the simulation's.
        val op = runCatching { Regex(""""invoke_host_function"\s*:\s*"(\w+)"""").find(result.parseResultXdr()!!.toXdrJson())?.groupValues?.get(1) }.getOrNull()
        say("  enviado de todos modos: $hash")
        say("  resultado en la cadena: ${result.status}, ledger ${result.ledger}${op?.let { ", $it" } ?: ""}")
        say("  https://stellar.expert/explorer/testnet/tx/$hash")
    }

    private fun build(source: com.soneso.stellar.sdk.TransactionBuilderAccount, op: InvokeHostFunctionOperation, fee: Long): Transaction {
        val b = TransactionBuilder(source, Network.TESTNET).setBaseFee(fee).addOperation(op).setTimeout(120)
        if (fee == FEE) b.setSorobanData(footprint(op))
        return b.build()
    }

    /** Every key a refused call touches; see `submitRejected` in `scripts/attacks/invoke.mjs`. */
    private fun footprint(op: InvokeHostFunctionOperation): com.soneso.stellar.sdk.xdr.SorobanTransactionDataXdr {
        fun data(contract: String, key: SCValXdr, durability: ContractDataDurabilityXdr = ContractDataDurabilityXdr.PERSISTENT) =
            LedgerKeyXdr.ContractData(LedgerKeyContractDataXdr(Address(contract).toSCAddress(), key, durability))
        fun instance(contract: String) = data(contract, Scv.toLedgerKeyContractInstance())
        fun code(hex: String) = LedgerKeyXdr.ContractCode(LedgerKeyContractCodeXdr(HashXdr(hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray())))
        fun key(vararg parts: SCValXdr) = Scv.toVec(parts.toList())
        val acc = addr(c.account)
        val readOnly = listOf(
            instance(c.usdc), instance(c.guardianHold), instance(c.holdRegistry), instance(c.account),
            data(c.account, key(Scv.toSymbol("ContextRuleData"), Scv.toUint32(0u))),
            data(c.account, key(Scv.toSymbol("PolicyData"), Scv.toUint32(0u))),
            data(c.account, key(Scv.toSymbol("SignerData"), Scv.toUint32(0u))),
        ) + (0 until 16).map { data(c.holdRegistry, key(Scv.toSymbol("Hold"), acc, Scv.toUint64(it.toULong()))) } +
            listOf(ACCOUNT_WASM, GUARDIAN_HOLD_WASM, HOLD_REGISTRY_WASM).map(::code)
        val nonce = (op.auth.first().credentials as SorobanCredentialsXdr.Address).value.nonce
        val readWrite = listOf(
            data(c.guardianHold, key(Scv.toSymbol("Config"), acc)),
            data(c.holdRegistry, key(Scv.toSymbol("NextId"), acc)),
            data(c.account, Scv.toLedgerKeyNonce(SCNonceKeyXdr(nonce)), ContractDataDurabilityXdr.TEMPORARY),
        )
        return SorobanDataBuilder()
            .setReadOnly(readOnly)
            .setReadWrite(readWrite)
            .setResources(SorobanDataBuilder.Resources(10_000_000L, 40_000L, 2_000L))
            .setResourceFee(RESOURCE_FEE)
            .build()
    }

    private fun payload(signer: String): SCValXdr = Scv.toMap(
        linkedMapOf(
            Scv.toSymbol("context_rule_ids") to Scv.toVec(RULES.map { Scv.toUint32(it) }),
            Scv.toSymbol("signers") to Scv.toMap(
                linkedMapOf<SCValXdr, SCValXdr>(Scv.toVec(listOf(Scv.toSymbol("Delegated"), Address(signer).toSCVal())) to Scv.toBytes(ByteArray(0))),
            ),
        ),
    )

    private fun checkAuth(digest: ByteArray): SorobanAuthorizationEntryXdr {
        val fn = (InvokeHostFunctionOperation.invokeContractFunction(c.account, "__check_auth", listOf(Scv.toBytes(digest))).hostFunction as HostFunctionXdr.InvokeContract).value
        return SorobanAuthorizationEntryXdr(
            SorobanCredentialsXdr.Void,
            SorobanAuthorizedInvocationXdr(SorobanAuthorizedFunctionXdr.ContractFn(fn), emptyList()),
        )
    }

    private fun signer(kp: KeyPair) = object : Signer {
        override val publicKey: ByteArray = kp.getPublicKey()
        override suspend fun signHash(hash: ByteArray): ByteArray = kp.sign(hash)
    }

    private fun describe(r: SubmitResult) = when (r) {
        is SubmitResult.Confirmed -> "confirmada ${r.txHash.hex}"
        else -> r.toString()
    }

    private companion object {
        val RULES = listOf(0u)
        const val RESOURCE_FEE = 4_000_000L
        const val FEE = RESOURCE_FEE + 1_000L
        const val ACCOUNT_WASM = TestnetProvisioner.ACCOUNT_WASM
        const val GUARDIAN_HOLD_WASM = "b36588780d05c900d891f63cbbade21bdf6b6db4a3b12554c0124c2e7a2a85ce"
        const val HOLD_REGISTRY_WASM = TestnetProvisioner.HOLD_REGISTRY_WASM
        val NAMES = mapOf(3 to "NotAllowed", 7 to "HoldNotReady", 3016 to "UnauthorizedSigner")

        fun contractCode(text: String): Int? = Regex("""Error\(Contract,\s*#(\d+)\)""").find(text)?.groupValues?.get(1)?.toInt()

        fun firstLine(text: String) = text.lineSequence().first().take(160)
    }
}
