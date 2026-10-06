package com.guardpay.shared.ui

import com.guardpay.shared.domain.ACCOUNT
import com.guardpay.shared.domain.ANA
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.STRANGER
import com.guardpay.shared.domain.addr
import com.guardpay.shared.domain.usdc
import com.guardpay.shared.stellar.AccountRules
import com.guardpay.shared.stellar.FakeSigner
import com.guardpay.shared.stellar.FakeStellarGateway
import com.guardpay.shared.stellar.SubmitResult
import com.guardpay.shared.ui.guardian.GuardianConfig
import com.guardpay.shared.ui.guardian.GuardianSession
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.wiring.GatewayGuardianChain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The guardian's one verb, stop, against the in-memory contract model. */
class GuardianSessionTest {
    private val ownerKey = ByteArray(32) { 1 }
    private val guardianKey = ByteArray(32) { 2 }
    private val owner = FakeSigner(ownerKey)

    private fun fake() = FakeStellarGateway(
        account = ACCOUNT,
        ownerKey = ownerKey,
        guardianKey = guardianKey,
        rules = AccountRules(addr('D'), dailyCap = usdc(50), holdDurationSeconds = 120, expiryWindowSeconds = 600),
        contacts = listOf(ANA),
        startBalance = usdc(1_000),
    )

    private fun session(gw: FakeStellarGateway, scope: kotlinx.coroutines.CoroutineScope) = GuardianSession(
        GatewayGuardianChain(gw, ACCOUNT, FakeSigner(guardianKey)),
        GuardianConfig("Laura", "Diego", DataSource.Simulation),
        scope,
    )

    @Test
    fun stoppingAWaitingHoldCancelsItOnTheRegistry() = runTest {
        val gw = fake()
        assertIs<SubmitResult.Confirmed>(gw.submitQueue(ACCOUNT, PaymentIntent(STRANGER, usdc(150)), owner))
        val g = session(gw, this)
        g.refresh()
        assertEquals(listOf(0L), g.ui.value.waiting.map { it.id })

        g.askStop(0)
        assertEquals(0L, g.ui.value.confirmFor)
        g.confirmStop()
        advanceUntilIdle()

        val ui = g.ui.value
        assertNull(ui.confirmFor)
        assertTrue(ui.waiting.isEmpty())
        val stopped = ui.stoppedByYou.single()
        assertEquals(HoldStatus.Cancelled, stopped.status)
        assertEquals(Party.Guardian, stopped.cancelledBy)
        assertEquals("Detención", ui.txs.getValue(0).single().what)
        assertEquals(usdc(1_000), gw.readBalance(ACCOUNT), "stopping moves no money")
    }

    @Test
    fun onlyWaitingHoldsCanBeAskedToStop() = runTest {
        val gw = fake()
        gw.submitQueue(ACCOUNT, PaymentIntent(STRANGER, usdc(150)), owner)
        gw.advance(120)
        gw.submitTransfer(ACCOUNT, PaymentIntent(STRANGER, usdc(150)), owner)
        val g = session(gw, this)
        g.refresh()

        assertEquals(HoldStatus.Executed, g.ui.value.holds.single().status)
        g.askStop(0)
        assertNull(g.ui.value.confirmFor)
        g.askStop(42)
        assertNull(g.ui.value.confirmFor)
    }

    @Test
    fun newAccountMeansNoContactAndNoExecutedHold() = runTest {
        val gw = fake()
        val g = session(gw, this)
        g.refresh()
        assertTrue(g.ui.value.isNewAccount(STRANGER))
        assertFalse(g.ui.value.isNewAccount(ANA.address))

        gw.submitQueue(ACCOUNT, PaymentIntent(STRANGER, usdc(150)), owner)
        gw.advance(120)
        gw.submitTransfer(ACCOUNT, PaymentIntent(STRANGER, usdc(150)), owner)
        g.refresh()
        assertFalse(g.ui.value.isNewAccount(STRANGER))
    }

    @Test
    fun backClosesTheConfirmationFirst() = runTest {
        val gw = fake()
        gw.submitQueue(ACCOUNT, PaymentIntent(STRANGER, usdc(150)), owner)
        val g = session(gw, this)
        g.refresh()
        val nav = Navigator()
        nav.go(Screen.GuardianList)
        nav.go(Screen.GuardianDetail(0))
        g.askStop(0)

        assertTrue(handleBack(nav, owner = null, guardian = g))
        assertNull(g.ui.value.confirmFor)
        assertEquals(Screen.GuardianDetail(0), nav.current)

        assertTrue(handleBack(nav, owner = null, guardian = g))
        assertEquals(Screen.GuardianList, nav.current)
        assertTrue(handleBack(nav, owner = null, guardian = g))
        assertEquals(Screen.Entry, nav.current)
        assertFalse(handleBack(nav, owner = null, guardian = g), "the platform closes the app")
    }
}
