package com.guardpay.shared.stellar

import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.signing.Signer
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.soneso.stellar.sdk.AbstractTransaction
import com.soneso.stellar.sdk.Address
import com.soneso.stellar.sdk.FriendBot
import com.soneso.stellar.sdk.InvokeHostFunctionOperation
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.Network
import com.soneso.stellar.sdk.TransactionBuilder
import com.soneso.stellar.sdk.contract.ContractClient
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.rpc.responses.GetTransactionStatus
import com.soneso.stellar.sdk.scval.Scv
import com.soneso.stellar.sdk.smartaccount.core.ExternalSigner
import com.soneso.stellar.sdk.xdr.SCValXdr
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * KmpStellarGateway against Stellar TESTNET. Opt-in: excluded unless Gradle runs
 * with `-PliveTestnet`. Proves transport and signing only: the account is the
 * SDK demo's OZ v0.7.0 wasm with no policy, and the native XLM SAC stands in for
 * USDC (same SEP-41 `balance`/`transfer`). Not our contracts, not our pin.
 *
 * Every key is generated per run, stays in memory and is never printed.
 */
class KmpStellarGatewayLiveTest {
    private val rpc = "https://soroban-testnet.stellar.org"
    private val accountWasm = "86b49fe03f7df0ad1c2a28bd8361b923ab57096e09f397f92f0c00ae3bd06d28"
    private val verifier = StellarAddress("CAW2Z46INPO5VIJEILMYSSEOLBVJIIII5GOE3TN5EUURSRM2FJCF7AJ6")
    private val xlmSac = StellarAddress("CDLZFC3SYJYDZT7K67VZ75HPJVIEUVNIXF47ZG2FB2RMQQVU2HHGCYSC")
    private val stroopsPerXlm = 10_000_000L

    private fun log(msg: String) = println("[live] $msg")

    private fun keySigner(kp: KeyPair) = object : Signer {
        override val publicKey: ByteArray = kp.getPublicKey()
        override suspend fun signAuthDigest(digest: ByteArray): ByteArray = kp.sign(digest)
    }

    @Test
    fun gatewayTransfersFromSmartAccountAndClassicAccountOnTestnet() = runBlocking {
        val server = SorobanServer(rpc)
        val payer = KeyPair.random()
        val owner = KeyPair.random()
        val attacker = KeyPair.random()
        val classic = KeyPair.random()
        check(FriendBot.fundTestnetAccount(payer.getAccountId())) { "friendbot failed (payer)" }
        check(FriendBot.fundTestnetAccount(classic.getAccountId())) { "friendbot failed (classic)" }

        // Fixture setup with the raw SDK, as in Spike C: deploy the demo account and fund it.
        val account = StellarAddress(
            ContractClient.deployFromWasmId(
                wasmId = accountWasm,
                constructorArgs = listOf(
                    Scv.toVec(listOf(ExternalSigner.ed25519(verifier.value, owner.getPublicKey()).toScVal())),
                    Scv.toMap(linkedMapOf<SCValXdr, SCValXdr>()),
                ),
                source = payer.getAccountId(),
                signer = payer,
                network = Network.TESTNET,
                rpcUrl = rpc,
                loadSpec = false,
            ).contractId,
        )
        log("smart account: ${account.value}")
        val fund = InvokeHostFunctionOperation.invokeContractFunction(
            xlmSac.value,
            "transfer",
            listOf(Address(payer.getAccountId()).toSCVal(), Address(account.value).toSCVal(), Scv.toInt128(BigInteger.fromLong(20 * stroopsPerXlm))),
        )
        val fundTx = server.prepareTransaction(
            TransactionBuilder(server.getAccount(payer.getAccountId()), Network.TESTNET)
                .setBaseFee(AbstractTransaction.MIN_BASE_FEE).addOperation(fund).setTimeout(120).build(),
        )
        fundTx.sign(payer)
        val fundHash = server.sendTransaction(fundTx).hash!!
        assertEquals(GetTransactionStatus.SUCCESS, server.pollTransaction(fundHash, maxAttempts = 60).status)
        log("fixture fund tx: $fundHash")

        // Placeholders: transfer never touches the registry or the policy.
        val gw = KmpStellarGateway(GuardPayNetwork(rpc, xlmSac, verifier, verifier, verifier), payer, server)
        val payerG = StellarAddress(payer.getAccountId())
        val oneXlm = PaymentIntent(payerG, UsdcAmount(stroopsPerXlm))

        // Reads.
        assertTrue(gw.latestLedgerTime().epochSeconds > 1_700_000_000)
        assertEquals(UsdcAmount(20 * stroopsPerXlm), gw.readBalance(account))

        // Negative: a key that is not the account's signer. Refused before anything is sent.
        val refused = gw.submitTransfer(account, oneXlm, keySigner(attacker))
        log("attacker transfer: $refused")
        assertIs<SubmitResult.Rejected>(refused)
        assertNull(refused.txHash)
        assertEquals(UsdcAmount(20 * stroopsPerXlm), gw.readBalance(account))

        // SmartAccount path: the owner's key signs the OZ digest.
        val smart = gw.submitTransfer(account, oneXlm, keySigner(owner))
        log("smart account transfer: $smart")
        assertIs<SubmitResult.Confirmed>(smart)
        assertEquals(UsdcAmount(19 * stroopsPerXlm), gw.readBalance(account))

        // ClassicAccount path (what the guardian's cancel uses): a G account authorizes
        // its own transfer through Address credentials; the payer only pays the fee.
        val classicG = StellarAddress(classic.getAccountId())
        val before = gw.readBalance(classicG).units
        val classicResult = gw.submitTransfer(classicG, oneXlm, keySigner(classic))
        log("classic account transfer: $classicResult")
        assertIs<SubmitResult.Confirmed>(classicResult)
        assertEquals(before - stroopsPerXlm, gw.readBalance(classicG).units)

        server.close()
    }
}
