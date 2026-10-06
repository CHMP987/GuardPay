package com.guardpay.shared.stellar

import com.guardpay.shared.domain.ACCOUNT
import com.guardpay.shared.domain.ANA
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LaneRules
import com.guardpay.shared.domain.LanePrediction
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.PaymentEvent
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.PaymentState
import com.guardpay.shared.domain.STRANGER
import com.guardpay.shared.domain.Transition
import com.guardpay.shared.domain.addr
import com.guardpay.shared.domain.on
import com.guardpay.shared.domain.predictLane
import com.guardpay.shared.domain.stateOf
import com.guardpay.shared.domain.usdc
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** The domain state machine driven by the in-memory contract model, end to end. */
class GatewayFlowTest {
    private val ownerKey = ByteArray(32) { 1 }
    private val guardianKey = ByteArray(32) { 2 }
    private val owner = FakeSigner(ownerKey)
    private val guardian = FakeSigner(guardianKey)
    private val attacker = FakeSigner(ByteArray(32) { 9 })

    private fun gateway() = FakeStellarGateway(
        account = ACCOUNT,
        ownerKey = ownerKey,
        guardianKey = guardianKey,
        rules = AccountRules(addr('D'), dailyCap = usdc(50), holdDurationSeconds = 120, expiryWindowSeconds = 600),
        contacts = listOf(ANA),
        startBalance = usdc(1_000),
    )

    private fun PaymentState.moveTo(e: PaymentEvent): PaymentState = (on(e) as Transition.Moved).to

    private fun SubmitResult.toEvent(): PaymentEvent = when (this) {
        is SubmitResult.Confirmed -> PaymentEvent.TransferConfirmed(txHash)
        is SubmitResult.Rejected -> PaymentEvent.ContractRejected(txHash)
        is SubmitResult.NetworkFailure -> PaymentEvent.SubmissionFailed
        SubmitResult.SigningCancelled -> PaymentEvent.SigningCancelled
    }

    private suspend fun FakeStellarGateway.laneFor(intent: PaymentIntent) = predictLane(
        intent,
        LaneRules(readTrustedContacts(ACCOUNT), readAccountRules(ACCOUNT).dailyCap, readDailySpent(ACCOUNT)),
    )

    @Test
    fun contactWithinCapLeavesImmediately() = runTest {
        val g = gateway()
        val intent = PaymentIntent(ANA.address, usdc(40))
        assertIs<LanePrediction.Immediate>(g.laneFor(intent))
        val s = PaymentState.Draft(intent).moveTo(PaymentEvent.SignRequested)
            .moveTo(g.submitTransfer(ACCOUNT, intent, owner).toEvent())
        assertIs<PaymentState.Sent>(s)
        assertEquals(usdc(40), g.readDailySpent(ACCOUNT))
    }

    @Test
    fun predictionAndContractAgreeOnTheCap() = runTest {
        val g = gateway()
        g.submitTransfer(ACCOUNT, PaymentIntent(ANA.address, usdc(40)), owner)
        val over = PaymentIntent(ANA.address, usdc(11))
        assertIs<LanePrediction.MustHold>(g.laneFor(over))
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, over, owner))
    }

    @Test
    fun strangerHeldThenReleasedWithNewSignature() = runTest {
        val g = gateway()
        val intent = PaymentIntent(STRANGER, usdc(150))
        assertIs<LanePrediction.MustHold>(g.laneFor(intent))

        // A direct transfer to a stranger is rejected by the contract.
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, intent, owner))

        var s = PaymentState.Draft(intent).moveTo(PaymentEvent.SignRequested)
        assertIs<SubmitResult.Confirmed>(g.submitQueue(ACCOUNT, intent, owner))
        val record = g.readHolds(ACCOUNT).single()
        assertEquals(g.now.epochSeconds + 120, record.readyAt.epochSeconds, "readyAt computed by the registry")
        s = s.moveTo(PaymentEvent.QueueConfirmed(record))
        assertIs<PaymentState.Held>(s)

        // GH-05 shape: not before readyAt.
        g.advance(60)
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, intent, owner))

        g.advance(61)
        s = s.moveTo(PaymentEvent.ClockAdvanced(g.latestLedgerTime()))
        assertIs<PaymentState.ReadyToSend>(s)
        s = s.moveTo(PaymentEvent.SignRequested).moveTo(g.submitTransfer(ACCOUNT, intent, owner).toEvent())
        assertIs<PaymentState.Sent>(s)
        assertEquals(HoldStatus.Executed, g.readHolds(ACCOUNT).single().status)
        assertIs<PaymentState.Sent>(stateOf(g.readHolds(ACCOUNT).single(), g.now))

        // GH-24 shape: the same record is never accepted twice.
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, intent, owner))
    }

    @Test
    fun guardianStopsAndTheReleaseIsRejected() = runTest {
        val g = gateway()
        val intent = PaymentIntent(STRANGER, usdc(150))
        g.submitQueue(ACCOUNT, intent, owner)
        val id = g.readHolds(ACCOUNT).single().id

        assertIs<SubmitResult.Confirmed>(g.submitCancel(ACCOUNT, id, guardian))
        val record = g.readHolds(ACCOUNT).single()
        assertEquals(PaymentState.Stopped(record, Party.Guardian, record.cancelledAt), stateOf(record, g.now))

        g.advance(500)
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, intent, owner))
        // Cancelling twice is rejected too: Stopped is final.
        assertIs<SubmitResult.Rejected>(g.submitCancel(ACCOUNT, id, guardian))
    }

    @Test
    fun guardianCanNeverMoveMoney() = runTest {
        val g = gateway()
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, PaymentIntent(ANA.address, usdc(1)), guardian))
        assertIs<SubmitResult.Rejected>(g.submitQueue(ACCOUNT, PaymentIntent(STRANGER, usdc(1)), guardian))
        assertEquals(usdc(1_000), g.readBalance(ACCOUNT))
    }

    @Test
    fun strangerKeyCannotCancel() = runTest {
        val g = gateway()
        g.submitQueue(ACCOUNT, PaymentIntent(STRANGER, usdc(150)), owner)
        assertIs<SubmitResult.Rejected>(g.submitCancel(ACCOUNT, 0, attacker))
        assertEquals(HoldStatus.Held, g.readHolds(ACCOUNT).single().status)
    }

    @Test
    fun duplicateActiveHoldIsRejected() = runTest {
        val g = gateway()
        val intent = PaymentIntent(STRANGER, usdc(150))
        assertIs<SubmitResult.Confirmed>(g.submitQueue(ACCOUNT, intent, owner))
        assertIs<SubmitResult.Rejected>(g.submitQueue(ACCOUNT, intent, owner))
    }

    @Test
    fun expiredHoldCannotBeSent() = runTest {
        val g = gateway()
        val intent = PaymentIntent(STRANGER, usdc(150))
        g.submitQueue(ACCOUNT, intent, owner)
        g.advance(120 + 600 + 1)
        assertIs<PaymentState.Expired>(stateOf(g.readHolds(ACCOUNT).single(), g.now))
        assertIs<SubmitResult.Rejected>(g.submitTransfer(ACCOUNT, intent, owner))
    }

    @Test
    fun cancelledPromptAndDeadNetworkChangeNothing() = runTest {
        val g = gateway()
        val intent = PaymentIntent(ANA.address, usdc(10))
        val draft = PaymentState.Draft(intent)

        val cancelling = FakeSigner(ownerKey, cancels = true)
        val back = draft.moveTo(PaymentEvent.SignRequested).moveTo(g.submitTransfer(ACCOUNT, intent, cancelling).toEvent())
        assertEquals(draft, back)

        g.networkDown = true
        val err = draft.moveTo(PaymentEvent.SignRequested).moveTo(g.submitTransfer(ACCOUNT, intent, owner).toEvent())
        assertIs<PaymentState.NetworkError>(err)
        assertEquals(usdc(1_000), g.readBalance(ACCOUNT))
        assertEquals(usdc(0), g.readDailySpent(ACCOUNT))
    }
}
