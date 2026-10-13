package com.guardpay.shared.simulation

import com.guardpay.shared.domain.ACCOUNT
import com.guardpay.shared.domain.ANA
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.STRANGER
import com.guardpay.shared.domain.addr
import com.guardpay.shared.domain.usdc
import com.guardpay.shared.stellar.AccountRules
import com.guardpay.shared.stellar.SubmitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** The simulation follows guardian_hold's transfer order (F-01). It runs on the wall clock. */
class SimulatedStellarGatewayTest {
    private val ownerKey = ByteArray(32) { 1 }
    private val guardianKey = ByteArray(32) { 2 }
    private val owner = SimulatedSigner(ownerKey)
    private val guardian = SimulatedSigner(guardianKey)

    private fun gateway(holdSeconds: Long) = SimulatedStellarGateway(
        account = ACCOUNT,
        ownerKey = ownerKey,
        guardianKey = guardianKey,
        policy = addr('P'),
        rules = AccountRules(addr('D'), dailyCap = usdc(50), holdDurationSeconds = holdSeconds, expiryWindowSeconds = 900),
        contacts = listOf(ANA),
        startBalance = usdc(1_000),
    )

    /** Real time, not the test scheduler's: the simulation reads the wall clock. */
    private suspend fun waitForReadyAt() = withContext(Dispatchers.Default) { delay(1_100) }

    @Test
    fun maturedContactHoldOverCapIsExecuted() = runTest {
        val g = gateway(holdSeconds = 1)
        val intent = PaymentIntent(ANA.address, usdc(60))
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, intent, owner))
        assertIs<SubmitResult.Confirmed>(g.submitQueue(ACCOUNT, intent, owner))
        waitForReadyAt()
        assertIs<SubmitResult.Confirmed>(g.submitTransfer(ACCOUNT, intent, owner))
        assertEquals(HoldStatus.Executed, g.readHolds(ACCOUNT).single().status)
        assertEquals(usdc(0), g.readDailySpent(ACCOUNT))
        assertIs<SubmitResult.Rejected>(g.submitCancel(ACCOUNT, 0, guardian))
    }

    @Test
    fun maturedContactHoldWithinCapIsExecutedWithoutUsingTheCap() = runTest {
        val g = gateway(holdSeconds = 1)
        val intent = PaymentIntent(ANA.address, usdc(10))
        assertIs<SubmitResult.Confirmed>(g.submitQueue(ACCOUNT, intent, owner))
        waitForReadyAt()
        assertIs<SubmitResult.Confirmed>(g.submitTransfer(ACCOUNT, intent, owner))
        assertEquals(HoldStatus.Executed, g.readHolds(ACCOUNT).single().status)
        assertEquals(usdc(0), g.readDailySpent(ACCOUNT))
    }

    @Test
    fun unmaturedContactHoldBlocksOnlyThatPayment() = runTest {
        val g = gateway(holdSeconds = 3_600)
        val intent = PaymentIntent(ANA.address, usdc(10))
        assertIs<SubmitResult.Confirmed>(g.submitQueue(ACCOUNT, intent, owner))
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, intent, owner))
        assertEquals(HoldStatus.Held, g.readHolds(ACCOUNT).single().status)
        assertIs<SubmitResult.Confirmed>(g.submitTransfer(ACCOUNT, PaymentIntent(ANA.address, usdc(20)), owner))
        assertEquals(usdc(20), g.readDailySpent(ACCOUNT))
    }

    @Test
    fun strangerStillNeedsAMaturedHold() = runTest {
        val g = gateway(holdSeconds = 3_600)
        val intent = PaymentIntent(STRANGER, usdc(10))
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, intent, owner))
        assertIs<SubmitResult.Confirmed>(g.submitQueue(ACCOUNT, intent, owner))
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, intent, owner))
    }
}
