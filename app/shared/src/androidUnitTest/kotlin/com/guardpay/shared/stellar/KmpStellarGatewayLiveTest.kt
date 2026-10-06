package com.guardpay.shared.stellar

import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.stellar.TestnetProvisioner.Companion.UNIT
import com.guardpay.shared.stellar.TestnetProvisioner.Companion.addr
import com.guardpay.shared.stellar.TestnetProvisioner.Companion.i128
import com.soneso.stellar.sdk.InvokeHostFunctionOperation
import com.soneso.stellar.sdk.KeyPair
import com.soneso.stellar.sdk.rpc.SorobanServer
import com.soneso.stellar.sdk.rpc.responses.GetTransactionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The app's gateway against our own contracts on Stellar TESTNET (P6). Opt-in:
 * excluded unless Gradle runs with `-PliveTestnet`. Deploys a fresh registry and
 * account from the uploaded wasm ([TestnetProvisioner]); every key is generated per
 * run, stays in memory and is never printed. Hashes are printed with `[live]` so
 * they can be copied into evidence.
 *
 * The owner signs here with an in-memory key through the same [Signer] interface
 * the phone's Keystore signer implements.
 */
class KmpStellarGatewayLiveTest {
    private fun log(msg: String) = println("[live] $msg")

    private fun keySigner(kp: KeyPair) = object : Signer {
        override val publicKey: ByteArray = kp.getPublicKey()
        override suspend fun signHash(hash: ByteArray): ByteArray = kp.sign(hash)
    }

    private fun confirmed(label: String, r: SubmitResult): SubmitResult.Confirmed {
        log("$label: $r")
        return assertIs<SubmitResult.Confirmed>(r, label)
    }

    @Test
    fun ownerSignsGuardianStopsAndNothingElsePasses() = runBlocking {
        val server = SorobanServer(TestnetProvisioner.RPC)
        val owner = KeyPair.random()
        val guardian = KeyPair.random()
        val attacker = KeyPair.random()
        val p = TestnetProvisioner(server, ::log).provision(owner.getAccountId(), guardian.getAccountId(), contacts = 1, strangers = 1)
        p.txHashes.forEach { (label, hash) -> log("setup $label: $hash") }
        TestnetProvisioner(server, ::log).fund(attacker.getAccountId())
        val c = p.config
        val gw = KmpStellarGateway(c.network(), server)
        val account = StellarAddress(c.account)
        val contact = StellarAddress(c.contacts.single())
        val stranger = StellarAddress(p.strangers.single())

        // The account the app reads: one rule, the owner its only signer, the guardian nowhere in it.
        val shape = gw.readAccountShape(account)
        log("shape: $shape")
        assertEquals(1, shape.contextRuleCount)
        assertEquals(listOf(StellarAddress(c.owner)), shape.signers)
        assertEquals(listOf(StellarAddress(c.guardianHold)), shape.policies)
        assertFalse(StellarAddress(c.guardian) in shape.signers, "the guardian must never be an account signer")
        val rules = gw.readAccountRules(account)
        assertEquals(StellarAddress(c.guardian), rules.guardian)
        assertEquals(UsdcAmount(TestnetProvisioner.DAILY_CAP), rules.dailyCap)
        assertEquals(listOf(contact), gw.readTrustedContacts(account).map { it.address })
        assertEquals(UsdcAmount(TestnetProvisioner.FUNDING), gw.readBalance(account))
        assertTrue(gw.readHolds(account).isEmpty())

        // Contact lane: the owner pays 10 to a trusted contact, under the cap.
        confirmed("owner transfer 10 to contact", gw.submitTransfer(account, PaymentIntent(contact, UsdcAmount(10 * UNIT)), keySigner(owner)))
        assertEquals(UsdcAmount(490 * UNIT), gw.readBalance(account))
        assertEquals(UsdcAmount(10 * UNIT), gw.readDailySpent(account))

        // GH-01 from the app: the attacker's key is not the account's signer. The
        // gateway's enforcing simulation stops it; nothing is sent.
        val viaApp = gw.submitTransfer(account, PaymentIntent(contact, UsdcAmount(10 * UNIT)), keySigner(attacker))
        log("GH-01 attacker via gateway: $viaApp")
        assertIs<SubmitResult.Rejected>(viaApp)
        assertNull(viaApp.txHash)

        // GH-01 on chain: transfer C→T of 10 with an empty signer map and rule [0].
        val adversary = Adversary(server, c.owner)
        val toContact = transferOp(c, contact.value, 10 * UNIT)
        val (gh01Hash, gh01Status) = adversary.send(toContact, c.account, attacker, payloadSigners = emptyList())
        log("GH-01 empty signer map on chain: $gh01Hash $gh01Status")
        assertEquals(GetTransactionStatus.FAILED, gh01Status)
        assertEquals(UsdcAmount(490 * UNIT), gw.readBalance(account))

        // Held lane: 150 to a stranger is queued, not paid.
        confirmed("owner queue 150", gw.submitQueue(account, PaymentIntent(stranger, UsdcAmount(150 * UNIT)), keySigner(owner)))
        confirmed("owner queue 60", gw.submitQueue(account, PaymentIntent(stranger, UsdcAmount(60 * UNIT)), keySigner(owner)))
        val holds = gw.readHolds(account)
        log("holds: $holds")
        assertEquals(listOf(HoldStatus.Held, HoldStatus.Held), holds.map { it.status })
        assertEquals(listOf(stranger, stranger), holds.map { it.destination })
        assertEquals(ContractAbi.HOLD_DURATION_SECS, holds[0].readyAt.epochSeconds - holds[0].createdAt.epochSeconds)

        // GH-05: too early, the contract refuses (code 7, HoldNotReady).
        val early = gw.submitTransfer(account, PaymentIntent(stranger, UsdcAmount(150 * UNIT)), keySigner(owner))
        log("early transfer: $early")
        assertEquals(SubmitResult.Rejected(null, 7), early)

        // Only the guardian stops: the owner's cancel needs the guardian's authorization,
        // which the owner cannot give, so it is refused before sending; the guardian's lands.
        val ownerCancel = gw.submitCancel(account, 1, keySigner(owner))
        log("owner cancel: $ownerCancel")
        assertEquals(SubmitResult.Rejected(null, null), ownerCancel)
        confirmed("guardian cancel hold 1", gw.submitCancel(account, 1, keySigner(guardian)))
        val stopped = gw.readHolds(account)[1]
        assertEquals(HoldStatus.Cancelled, stopped.status)
        assertEquals(Party.Guardian, stopped.cancelledBy)

        // Wait for hold 0 to mature on the ledger clock.
        while (gw.latestLedgerTime() < holds[0].readyAt) delay(5_000)

        // GH-24: hold 0 is mature, but without the owner's signature nothing executes.
        val matured = transferOp(c, stranger.value, 150 * UNIT)
        val (gh24a, gh24aStatus) = adversary.send(matured, c.account, attacker, payloadSigners = emptyList())
        log("GH-24 no signature on chain: $gh24a $gh24aStatus")
        assertEquals(GetTransactionStatus.FAILED, gh24aStatus)
        val (gh24b, gh24bStatus) = adversary.send(matured, c.account, attacker, payloadSigners = listOf(attacker.getAccountId()))
        log("GH-24 attacker signature on chain: $gh24b $gh24bStatus")
        assertEquals(GetTransactionStatus.FAILED, gh24bStatus)
        assertEquals(UsdcAmount(490 * UNIT), gw.readBalance(account))
        assertEquals(HoldStatus.Held, gw.readHolds(account)[0].status)

        // The owner releases it.
        confirmed("owner transfer matured hold 0", gw.submitTransfer(account, PaymentIntent(stranger, UsdcAmount(150 * UNIT)), keySigner(owner)))
        assertEquals(UsdcAmount(340 * UNIT), gw.readBalance(account))
        assertEquals(listOf(HoldStatus.Executed, HoldStatus.Cancelled), gw.readHolds(account).map { it.status })
        assertEquals(UsdcAmount(150 * UNIT), gw.readBalance(stranger))

        // The stopped hold can never be released.
        val afterStop = gw.submitTransfer(account, PaymentIntent(stranger, UsdcAmount(60 * UNIT)), keySigner(owner))
        log("transfer of the stopped hold: $afterStop")
        assertIs<SubmitResult.Rejected>(afterStop)

        server.close()
    }

    private fun transferOp(c: TestnetConfig, to: String, amount: Long) =
        InvokeHostFunctionOperation.invokeContractFunction(c.usdc, "transfer", listOf(addr(c.account), addr(to), i128(amount)))
}
