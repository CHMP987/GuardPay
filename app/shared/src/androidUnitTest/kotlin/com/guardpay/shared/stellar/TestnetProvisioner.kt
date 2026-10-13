package com.guardpay.shared.stellar

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.AbstractTransaction
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.Asset
import com.soneso.stellar.sdk.ChangeTrustOperation
import com.soneso.stellar.sdk.FriendBot
import com.soneso.stellar.sdk.InvokeHostFunctionOperation
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.Network
import com.soneso.stellar.sdk.Operation
import com.soneso.stellar.sdk.TransactionBuilder
import com.soneso.stellar.sdk.contract.ContractClient
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.rpc.responses.GetTransactionStatus
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.xdr.ContractExecutableXdr
import com.soneso.stellar.sdk.xdr.ContractIDPreimageXdr
import com.soneso.stellar.sdk.xdr.CreateContractArgsV2Xdr
import com.soneso.stellar.sdk.xdr.HostFunctionXdr
import com.soneso.stellar.sdk.xdr.SCValXdr
import java.security.MessageDigest

/**
 * Deploys a fresh GuardPay setup on Stellar TESTNET from the wasm already uploaded by
 * [uploadRelease] (hashes in `evidence/stellar/deployment.md`), so nobody's seed is needed:
 *  - a test token: classic asset `USDC:<random issuer>` wrapped in its SAC. It is
 *    NOT Circle's USDC; the issuer key exists only in this process;
 *  - a `hold_registry` whose guardian is [provision]'s `guardian`;
 *  - an `account` whose single Default rule is `Delegated(owner)` + the shared
 *    `guardian_hold`. The guardian is never passed to the account;
 *  - trustlines for the contacts and the extra destinations, then a mint to the account.
 *
 * Only public addresses leave this class (as [TestnetConfig]). Keys are never printed.
 */
internal class TestnetProvisioner(
    private val server: SorobanServer,
    private val log: (String) -> Unit,
) {
    class Provisioned(
        val config: TestnetConfig,
        /** Destinations with a trustline that are not trusted contacts (held lane). */
        val strangers: List<String>,
        val txHashes: List<Pair<String, String>>,
    )

    class Release(
        /** File name to wasm id (its sha256, hex). */
        val wasmIds: Map<String, String>,
        val guardianHold: String,
        val txHashes: List<Pair<String, String>>,
    )

    private val hashes = mutableListOf<Pair<String, String>>()

    /**
     * Uploads one contract release ([wasm]: file name to bytes, the three from CI's `wasm`
     * artifact) and deploys the shared `guardian_hold` from it. Done once per release;
     * its ids then go in this class's constants. The source key lives only in this call.
     */
    suspend fun uploadRelease(wasm: Map<String, ByteArray>): Release {
        val source = KeyPair.random()
        fund(source.getAccountId())
        val ids = wasm.mapValues { (name, bytes) ->
            run("upload $name", source, InvokeHostFunctionOperation.uploadContractWasm(bytes))
            sha256Hex(bytes)
        }
        val policy = deploy("guardian_hold", source, ids.getValue("guardian_hold.wasm"), emptyList())
        return Release(ids, policy, hashes.toList())
    }

    suspend fun provision(owner: String, guardian: String, contacts: Int = 1, strangers: Int = 1): Provisioned {
        require(owner != guardian) { "the guardian must not be the owner" }
        val issuer = KeyPair.random()
        val contactKeys = List(contacts) { KeyPair.random() }
        val strangerKeys = List(strangers) { KeyPair.random() }
        (listOf(issuer.getAccountId(), owner, guardian) + (contactKeys + strangerKeys).map { it.getAccountId() }).forEach { fund(it) }

        val asset = Asset.createNonNativeAsset("USDC", issuer.getAccountId())
        val sac = InvokeHostFunctionOperation(
            HostFunctionXdr.CreateContractV2(
                CreateContractArgsV2Xdr(ContractIDPreimageXdr.FromAsset(asset.toXdr()), ContractExecutableXdr.Void, emptyList()),
            ),
            emptyList(),
        )
        run("deploy test SAC", issuer, sac)
        val usdc = asset.getContractId(Network.TESTNET)
        (contactKeys + strangerKeys).forEach { run("trustline", it, ChangeTrustOperation(asset, ChangeTrustOperation.MAX_LIMIT)) }

        val registry = deploy("hold_registry", issuer, HOLD_REGISTRY_WASM, listOf(addr(guardian), addr(GUARDIAN_HOLD)))
        val contactIds = contactKeys.map { it.getAccountId() }
        // guardian_hold's InstallParams; Soroban maps must be in key order.
        val install = Scv.toMap(
            linkedMapOf<SCValXdr, SCValXdr>(
                Scv.toSymbol("daily_cap") to i128(DAILY_CAP),
                Scv.toSymbol("owner") to addr(owner),
                Scv.toSymbol("registry") to addr(registry),
                Scv.toSymbol("trusted_contacts") to Scv.toVec(contactIds.map(::addr)),
                Scv.toSymbol("usdc") to addr(usdc),
            ),
        )
        val account = deploy("account", issuer, ACCOUNT_WASM, listOf(addr(owner), addr(GUARDIAN_HOLD), install))
        run("mint", issuer, InvokeHostFunctionOperation.invokeContractFunction(usdc, "mint", listOf(addr(account), i128(FUNDING))))

        val config = TestnetConfig(RPC, usdc, registry, GUARDIAN_HOLD, account, owner, guardian, contactIds)
        log("provisioned account=$account registry=$registry usdc=$usdc")
        return Provisioned(config, strangerKeys.map { it.getAccountId() }, hashes.toList())
    }

    /** FriendBot refuses an account that already exists; that is fine. */
    suspend fun fund(id: String) {
        val exists = runCatching { server.getAccount(id) }.isSuccess
        if (!exists) check(FriendBot.fundTestnetAccount(id)) { "friendbot failed for ${id.take(4)}…" }
    }

    private suspend fun deploy(label: String, source: KeyPair, wasm: String, args: List<SCValXdr>): String =
        ContractClient.deployFromWasmId(
            wasmId = wasm,
            constructorArgs = args,
            source = source.getAccountId(),
            signer = source,
            network = Network.TESTNET,
            rpcUrl = RPC,
            loadSpec = false,
        ).contractId.also { log("deployed $label: $it") }

    private suspend fun run(label: String, source: KeyPair, op: Operation) {
        var tx = TransactionBuilder(server.getAccount(source.getAccountId()), Network.TESTNET)
            .setBaseFee(AbstractTransaction.MIN_BASE_FEE).addOperation(op).setTimeout(120).build()
        if (op is InvokeHostFunctionOperation) tx = server.prepareTransaction(tx)
        tx.sign(source)
        val hash = server.sendTransaction(tx).hash ?: error("$label: no hash")
        val status = server.pollTransaction(hash, maxAttempts = 60).status
        check(status == GetTransactionStatus.SUCCESS) { "$label $hash: $status" }
        hashes += label to hash
    }

    companion object {
        const val RPC = "https://soroban-testnet.stellar.org"
        // From PolicyReleaseLiveTest with CI run 38343802866 (F-01). The Oct 6 release was
        // guardian_hold CCGIEMIU…, account 986956cc…, hold_registry 031173c2….
        const val GUARDIAN_HOLD = "CBTIF2BF7YGCR6OT7TQMHGGMY6BVHKPOAJV5BYZQ2P3IWKSEXYHLWFMW"
        const val ACCOUNT_WASM = "d60dd1d299a98f886ad48027dd0499aaee07742fb7dd7051ada355e852cf40f9"
        const val HOLD_REGISTRY_WASM = "a57f01b70e0583fb7abf8ff30ce4414094dd08c27c53349ad8c89bb0438f93d0"
        const val UNIT = 10_000_000L // 7 decimals, like USDC on Stellar
        const val DAILY_CAP = 50 * UNIT
        const val FUNDING = 500 * UNIT

        fun addr(id: String): SCValXdr = Address(id).toSCVal()
        fun i128(v: Long): SCValXdr = Scv.toInt128(BigInteger.fromLong(v))
        fun sha256Hex(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
