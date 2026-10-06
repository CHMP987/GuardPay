package com.guardpay.shared.ui

import com.guardpay.shared.domain.ACCOUNT
import com.guardpay.shared.domain.ANA
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.STRANGER
import com.guardpay.shared.domain.addr
import com.guardpay.shared.domain.usdc
import com.guardpay.shared.stellar.AccountRules
import com.guardpay.shared.stellar.FakeSigner
import com.guardpay.shared.stellar.FakeStellarGateway
import com.guardpay.shared.stellar.SubmitResult
import com.guardpay.shared.ui.guardian.GuardianChain
import com.guardpay.shared.ui.guardian.HoldWatcher
import com.guardpay.shared.ui.wiring.GatewayGuardianChain
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** P8: the guardian hears about each held payment once, with what the registry says. */
class HoldWatcherTest {
    private val ownerKey = ByteArray(32) { 1 }
    private val guardianKey = ByteArray(32) { 2 }
    private val owner = FakeSigner(ownerKey)
    private val other = addr('Y')

    private fun fake() = FakeStellarGateway(
        account = ACCOUNT,
        ownerKey = ownerKey,
        guardianKey = guardianKey,
        rules = AccountRules(addr('D'), dailyCap = usdc(50), holdDurationSeconds = 120, expiryWindowSeconds = 600),
        contacts = listOf(ANA),
        startBalance = usdc(1_000),
    )

    private fun watcher(gw: FakeStellarGateway, seen: Set<Long> = emptySet()) =
        HoldWatcher(GatewayGuardianChain(gw, ACCOUNT, FakeSigner(guardianKey)), seen, TimeZone.UTC)

    @Test
    fun aNewHoldAlertsOnceWithTheDestinationAndAmountFromTheRegistry() = runTest {
        val gw = fake()
        val w = watcher(gw)
        assertTrue(w.check().isEmpty())

        assertIs<SubmitResult.Confirmed>(gw.submitQueue(ACCOUNT, PaymentIntent(STRANGER, usdc(150)), owner))
        val alert = w.check().single()
        val record = gw.readHolds(ACCOUNT).single()
        assertEquals(record.id, alert.holdId)
        assertEquals("Pago retenido: 150 USDC", alert.title)
        assertTrue(STRANGER.value in alert.text, "the whole destination, as the registry stores it")
        assertEquals(record.createdAt, alert.createdAt)

        assertTrue(w.check().isEmpty(), "never twice for the same record")
    }

    @Test
    fun onlyWaitingHoldsAlertAndAlreadyAlertedOnesStaySilentAfterARestart() = runTest {
        val gw = fake()
        gw.submitQueue(ACCOUNT, PaymentIntent(STRANGER, usdc(150)), owner)
        gw.submitQueue(ACCOUNT, PaymentIntent(other, usdc(60)), owner)
        gw.submitCancel(ACCOUNT, 1, FakeSigner(guardianKey))

        val first = watcher(gw)
        assertEquals(listOf(0L), first.check().map { it.holdId }, "a stopped record is not news")

        gw.submitQueue(ACCOUNT, PaymentIntent(other, usdc(70)), owner)
        val restarted = watcher(gw, first.alertedIds)
        assertEquals(listOf(2L), restarted.check().map { it.holdId })
    }

    @Test
    fun aFailedReadThrowsInsteadOfLookingQuiet() = runTest {
        val down = object : GuardianChain {
            override suspend fun latestLedgerTime() = error("rpc down")
            override suspend fun readHolds() = error("rpc down")
            override suspend fun readTrustedContacts() = error("rpc down")
            override suspend fun submitCancel(holdId: Long, onSigned: () -> Unit) = error("rpc down")
        }
        assertFailsWith<IllegalStateException> { HoldWatcher(down).check() }
    }
}
