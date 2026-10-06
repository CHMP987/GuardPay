// Spike C (P1-C): can kmp-stellar-sdk 1.14.0 produce, from Kotlin, an ed25519
// signature that an OpenZeppelin smart account's __check_auth accepts on testnet?
//
// Target: the SDK demo's account wasm (OZ stellar-contracts v0.7.0), NOT our pin
// b40c5ea. The result must be re-run against Abraham's Spike A account.
//
// TESTNET ONLY. Every key is generated per run, lives in memory, and is never
// printed or written to disk.

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.AbstractTransaction
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.FriendBot
import com.soneso.stellar.sdk.InvokeHostFunctionOperation
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.Network
import com.soneso.stellar.sdk.Transaction
import com.soneso.stellar.sdk.TransactionBuilder
import com.soneso.stellar.sdk.contract.ContractClient
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.rpc.responses.GetTransactionStatus
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.smartaccount.core.Ed25519Signature
import com.soneso.stellar.sdk.smartaccount.core.ExternalSigner
import com.soneso.stellar.sdk.smartaccount.core.SmartAccountAuth
import com.soneso.stellar.sdk.xdr.SCValXdr
import com.soneso.stellar.sdk.xdr.SorobanAuthorizationEntryXdr
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

const val RPC_URL = "https://soroban-testnet.stellar.org"
val NETWORK = Network.TESTNET

// From smart-account-demo/shared/.../config/DemoConfig.kt at kmp-stellar-sdk v1.14.0.
const val ACCOUNT_WASM_HASH = "86b49fe03f7df0ad1c2a28bd8361b923ab57096e09f397f92f0c00ae3bd06d28"
const val ED25519_VERIFIER = "CAW2Z46INPO5VIJEILMYSSEOLBVJIIII5GOE3TN5EUURSRM2FJCF7AJ6"
const val NATIVE_XLM_SAC = "CDLZFC3SYJYDZT7K67VZ75HPJVIEUVNIXF47ZG2FB2RMQQVU2HHGCYSC"

const val STROOPS_PER_XLM = 10_000_000L
val DEFAULT_RULE_IDS = listOf(0u)

fun log(msg: String) = println("[spike-c] $msg")
fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

/**
 * XDR of ScVal::Vec([ScVal::U32(id), ...]), written by hand so the digest check
 * does not depend on the SDK's own encoder.
 * SCV_VEC = 16, optional vec present = 1, length, then SCV_U32 = 3 + value.
 */
fun ruleIdsXdrByHand(ids: List<UInt>): ByteArray {
    val out = ByteArrayOutputStream()
    fun u32(v: Long) = out.write(byteArrayOf((v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte()))
    u32(16); u32(1); u32(ids.size.toLong())
    ids.forEach { u32(3); u32(it.toLong()) }
    return out.toByteArray()
}

suspend fun buildTx(server: SorobanServer, payer: KeyPair, op: InvokeHostFunctionOperation): Transaction =
    TransactionBuilder(server.getAccount(payer.getAccountId()), NETWORK)
        .setBaseFee(AbstractTransaction.MIN_BASE_FEE)
        .addOperation(op)
        .setTimeout(120)
        .build()

/** Simulates, assembles, signs as fee payer, sends and waits. Returns the tx hash. */
suspend fun submit(server: SorobanServer, payer: KeyPair, op: InvokeHostFunctionOperation, label: String): String {
    val prepared = server.prepareTransaction(buildTx(server, payer, op))
    prepared.sign(payer)
    val sent = server.sendTransaction(prepared)
    val hash = sent.hash ?: error("$label: no hash, status=${sent.status} error=${sent.errorResultXdr}")
    val result = server.pollTransaction(hash, maxAttempts = 60)
    log("$label: status=${result.status} hash=$hash")
    check(result.status == GetTransactionStatus.SUCCESS) { "$label failed: ${result.resultXdr}" }
    return hash
}

fun transferOp(from: String, to: String, stroops: Long) = InvokeHostFunctionOperation.invokeContractFunction(
    contractAddress = NATIVE_XLM_SAC,
    functionName = "transfer",
    parameters = listOf(Address(from).toSCVal(), Address(to).toSCVal(), Scv.toInt128(BigInteger.fromLong(stroops))),
)

/** Simulates [unsigned] with [signedEntry] attached. Returns the host's error, or null if accepted. */
suspend fun simulateWithAuth(
    server: SorobanServer,
    payer: KeyPair,
    unsigned: InvokeHostFunctionOperation,
    signedEntry: SorobanAuthorizationEntryXdr,
): String? {
    val op = InvokeHostFunctionOperation(unsigned.hostFunction, listOf(signedEntry))
    return server.simulateTransaction(buildTx(server, payer, op)).error
}

suspend fun main() {
    val server = SorobanServer(RPC_URL)
    log("network: ${NETWORK.networkPassphrase}")

    // Fee payer (plain G account), owner key (the "duena", ed25519 External signer)
    // and an attacker key that is not a signer of the account.
    val payer = KeyPair.random()
    val owner = KeyPair.random()
    val attacker = KeyPair.random()
    check(FriendBot.fundTestnetAccount(payer.getAccountId())) { "friendbot failed" }
    log("fee payer G: ${payer.getAccountId()}")
    log("owner ed25519 pubkey: ${owner.getPublicKey().hex()}")

    // 1. Deploy an OZ v0.7.0 smart account: signer = External(ed25519 verifier, owner), no policies.
    val ownerSigner = ExternalSigner.ed25519(ED25519_VERIFIER, owner.getPublicKey())
    val account = ContractClient.deployFromWasmId(
        wasmId = ACCOUNT_WASM_HASH,
        constructorArgs = listOf(
            Scv.toVec(listOf(ownerSigner.toScVal())),
            Scv.toMap(linkedMapOf<SCValXdr, SCValXdr>()),
        ),
        source = payer.getAccountId(),
        signer = payer,
        network = NETWORK,
        rpcUrl = RPC_URL,
        loadSpec = false,
    ).contractId
    log("smart account C: $account")

    // 2. Fund the account with 20 XLM (payer authorizes as source account).
    submit(server, payer, transferOp(payer.getAccountId(), account, 20 * STROOPS_PER_XLM), "fund account")

    // 3. Transfer C -> payer (1 XLM), which only the account can authorize.
    val transfer = transferOp(account, payer.getAccountId(), 1 * STROOPS_PER_XLM)
    val sim = server.simulateTransaction(buildTx(server, payer, transfer))
    check(sim.error == null) { "simulation error: ${sim.error}" }
    val entry = sim.results!!.first().parseAuth()!!.single()
    log("auth entry credential arm: ${entry.credentials::class.simpleName}")
    val expiration = server.getLatestLedger().sequence.toUInt() + 100u

    // 4. Digest: the SDK's value vs. an independent computation.
    val payloadHash = SmartAccountAuth.buildAuthPayloadHash(entry, expiration, NETWORK.networkPassphrase)
    val ruleIdsXdr = ruleIdsXdrByHand(DEFAULT_RULE_IDS)
    val sdkDigest = SmartAccountAuth.buildAuthDigest(payloadHash, DEFAULT_RULE_IDS)
    val handDigest = sha256(payloadHash + ruleIdsXdr)
    log("signature_payload     = ${payloadHash.hex()}")
    log("xdr(context_rule_ids) = ${ruleIdsXdr.hex()}")
    log("digest (SDK)          = ${sdkDigest.hex()}")
    log("digest (independent)  = ${handDigest.hex()}")
    check(sdkDigest.contentEquals(handDigest)) { "DIGEST MISMATCH" }
    log("digest check: MATCH")

    suspend fun signedWith(key: KeyPair, digest: ByteArray) = SmartAccountAuth.signAuthEntry(
        entry = entry,
        signer = ownerSigner,
        signature = Ed25519Signature(publicKey = owner.getPublicKey(), signature = key.sign(digest)),
        expirationLedger = expiration,
        contextRuleIds = DEFAULT_RULE_IDS,
    )

    // 5. Negative controls, simulation only (nothing is submitted).
    val n1 = simulateWithAuth(server, payer, transfer, signedWith(owner, payloadHash))
    log("N1 owner signs payload WITHOUT context_rule_ids -> ${if (n1 != null) "REJECTED" else "ACCEPTED (bad)"}")
    log("N1 error: ${n1?.take(400)}")
    val n2 = simulateWithAuth(server, payer, transfer, signedWith(attacker, handDigest))
    log("N2 non-owner key signs the correct digest -> ${if (n2 != null) "REJECTED" else "ACCEPTED (bad)"}")
    log("N2 error: ${n2?.take(400)}")

    // 6. Positive: the owner signs the correct digest; submit for real.
    val signedEntry = signedWith(owner, handDigest)
    val ok = simulateWithAuth(server, payer, transfer, signedEntry)
    check(ok == null) { "simulation with owner signature rejected: $ok" }
    val hash = submit(
        server, payer,
        InvokeHostFunctionOperation(transfer.hostFunction, listOf(signedEntry)),
        "signed transfer from C",
    )
    log("RESULT: transfer signed in Kotlin accepted by __check_auth. tx=$hash")
    log("explorer: https://stellar.expert/explorer/testnet/tx/$hash")
    server.close()
}
