package com.guardpay.shared.domain

import com.guardpay.shared.domain.PaymentEvent.ClockAdvanced
import com.guardpay.shared.domain.PaymentEvent.ContractRejected
import com.guardpay.shared.domain.PaymentEvent.ErrorDismissed
import com.guardpay.shared.domain.PaymentEvent.HoldCancelled
import com.guardpay.shared.domain.PaymentEvent.QueueConfirmed
import com.guardpay.shared.domain.PaymentEvent.SignRequested
import com.guardpay.shared.domain.PaymentEvent.SigningCancelled
import com.guardpay.shared.domain.PaymentEvent.SubmissionFailed
import com.guardpay.shared.domain.PaymentEvent.TransferConfirmed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PaymentStateTest {
    private val intent = PaymentIntent(STRANGER, usdc(150))
    private val draft = PaymentState.Draft(intent)
    private val signingNew = PaymentState.Signing(SigningOrigin.NewPayment(intent))
    private val held = PaymentState.Held(hold())
    private val ready = PaymentState.ReadyToSend(hold())
    private val signingRelease = PaymentState.Signing(SigningOrigin.Release(hold()))

    private val allEvents: List<PaymentEvent> = listOf(
        SignRequested, SigningCancelled, QueueConfirmed(hold()), TransferConfirmed(hash(1)),
        ContractRejected(hash(2)), ContractRejected(null), SubmissionFailed, ErrorDismissed,
        ClockAdvanced(LedgerTime(1_000)), ClockAdvanced(LedgerTime(1_500)), ClockAdvanced(LedgerTime(9_999)),
        HoldCancelled(Party.Guardian, LedgerTime(1_060)), HoldCancelled(Party.Owner, null),
    )

    private fun PaymentState.moveTo(event: PaymentEvent): PaymentState {
        val t = on(event)
        assertIs<Transition.Moved>(t, "$this on $event")
        return t.to
    }

    private fun PaymentState.rejects(event: PaymentEvent) {
        assertIs<Transition.Invalid>(on(event), "$this must reject $event")
    }

    // --- valid paths -------------------------------------------------------

    @Test
    fun directPaymentToContact() {
        val sent = draft.moveTo(SignRequested).moveTo(TransferConfirmed(hash(1)))
        assertEquals(PaymentState.Sent(intent, hash(1), hold = null), sent)
    }

    @Test
    fun heldPaymentGoesThroughWaitThenNeedsANewSignature() {
        val h = hold()
        var s = draft.moveTo(SignRequested).moveTo(QueueConfirmed(h))
        assertEquals(PaymentState.Held(h), s)
        s = s.moveTo(ClockAdvanced(LedgerTime(1_119)))
        assertEquals(PaymentState.Held(h), s, "still held one second before readyAt")
        s = s.moveTo(ClockAdvanced(LedgerTime(1_120)))
        assertEquals(PaymentState.ReadyToSend(h), s)
        s = s.moveTo(SignRequested)
        assertEquals(PaymentState.Signing(SigningOrigin.Release(h)), s)
        s = s.moveTo(TransferConfirmed(hash(3)))
        assertEquals(PaymentState.Sent(h.intent, hash(3), h), s)
    }

    @Test
    fun guardianCanStopHeldAndReady() {
        assertEquals(PaymentState.Stopped(hold(), Party.Guardian, LedgerTime(1_060)), held.moveTo(HoldCancelled(Party.Guardian, LedgerTime(1_060))))
        assertEquals(PaymentState.Stopped(hold(), Party.Guardian, null), ready.moveTo(HoldCancelled(Party.Guardian, null)))
    }

    @Test
    fun ownerCanStopHerOwnHold() {
        assertIs<PaymentState.Stopped>(held.moveTo(HoldCancelled(Party.Owner, null)))
    }

    @Test
    fun matureHoldExpires() {
        assertEquals(PaymentState.Expired(hold()), ready.moveTo(ClockAdvanced(LedgerTime(1_721))))
        assertEquals(ready, ready.moveTo(ClockAdvanced(LedgerTime(1_720))), "expiresAt itself is still valid")
        assertEquals(PaymentState.Expired(hold()), held.moveTo(ClockAdvanced(LedgerTime(1_721))))
    }

    @Test
    fun holdWithoutExpiryNeverExpires() {
        val r = PaymentState.ReadyToSend(hold(expiresAt = null))
        assertEquals(r, r.moveTo(ClockAdvanced(LedgerTime(Long.MAX_VALUE))))
    }

    @Test
    fun cancellingTheSigningPromptChangesNothing() {
        assertEquals(draft, signingNew.moveTo(SigningCancelled))
        assertEquals(ready, signingRelease.moveTo(SigningCancelled))
    }

    @Test
    fun networkErrorRetriesOrGoesBack() {
        val err = signingNew.moveTo(SubmissionFailed)
        assertEquals(PaymentState.NetworkError(SigningOrigin.NewPayment(intent)), err)
        assertEquals(signingNew, err.moveTo(SignRequested))
        assertEquals(draft, err.moveTo(ErrorDismissed))

        val relErr = signingRelease.moveTo(SubmissionFailed)
        assertEquals(ready, relErr.moveTo(ErrorDismissed))
    }

    @Test
    fun contractRejection() {
        assertEquals(PaymentState.Rejected(intent, hash(2)), signingNew.moveTo(ContractRejected(hash(2))))
        // A release attempt after the guardian stopped it: "el contrato no permitió este envío".
        assertEquals(PaymentState.Rejected(hold().intent, hash(4)), signingRelease.moveTo(ContractRejected(hash(4))))
    }

    // --- invalid transitions --------------------------------------------------

    @Test
    fun finalStatesNeverChange() {
        val finals = listOf(
            PaymentState.Sent(intent, hash(1), null),
            PaymentState.Stopped(hold(), Party.Guardian, null),
            PaymentState.Expired(hold()),
            PaymentState.Rejected(intent, hash(2)),
            PaymentState.Rejected(intent, null),
        )
        for (s in finals) {
            assertTrue(s.isFinal)
            allEvents.forEach { s.rejects(it) }
        }
    }

    @Test
    fun onlyAnExplicitQueueEntersHeld() {
        // From Draft nothing but a signature request is accepted.
        allEvents.filter { it != SignRequested }.forEach { draft.rejects(it) }
        // A queue result cannot land on a release signature.
        signingRelease.rejects(QueueConfirmed(hold()))
    }

    @Test
    fun queueRecordMustMatchWhatWasSigned() {
        signingNew.rejects(QueueConfirmed(hold(amount = usdc(151))))
        signingNew.rejects(QueueConfirmed(hold(destination = ANA.address)))
        signingNew.rejects(QueueConfirmed(hold(status = HoldStatus.Executed)))
    }

    @Test
    fun heldCannotBeSentOrSignedBeforeReady() {
        held.rejects(SignRequested)
        held.rejects(TransferConfirmed(hash(1)))
        held.rejects(QueueConfirmed(hold()))
    }

    @Test
    fun signingIgnoresClockAndCancellation() {
        signingNew.rejects(ClockAdvanced(LedgerTime(5_000)))
        signingRelease.rejects(HoldCancelled(Party.Guardian, null))
        signingNew.rejects(SignRequested)
    }

    @Test
    fun heldStatesNeedAHeldRecord() {
        assertFailsWith<IllegalArgumentException> { PaymentState.Held(hold(status = HoldStatus.Executed)) }
        assertFailsWith<IllegalArgumentException> { PaymentState.ReadyToSend(hold(status = HoldStatus.Cancelled, cancelledBy = Party.Guardian)) }
    }

    // --- seal and derivation ------------------------------------------------------

    @Test
    fun sealOnlyOnChainBackedStates() {
        assertFalse(draft.verifiedOnChain)
        assertFalse(signingNew.verifiedOnChain)
        assertFalse(PaymentState.NetworkError(SigningOrigin.NewPayment(intent)).verifiedOnChain)
        assertFalse(PaymentState.Rejected(intent, null).verifiedOnChain)
        assertTrue(PaymentState.Rejected(intent, hash(2)).verifiedOnChain)
        assertTrue(held.verifiedOnChain)
        assertTrue(ready.verifiedOnChain)
    }

    @Test
    fun stateIsRebuiltFromRegistryAndLedgerTime() {
        assertEquals(PaymentState.Held(hold()), stateOf(hold(), LedgerTime(1_000)))
        assertEquals(PaymentState.ReadyToSend(hold()), stateOf(hold(), LedgerTime(1_120)))
        assertEquals(PaymentState.Expired(hold()), stateOf(hold(), LedgerTime(1_721)))
        assertEquals(
            PaymentState.Sent(hold().intent, null, hold(status = HoldStatus.Executed)),
            stateOf(hold(status = HoldStatus.Executed), LedgerTime(1_500)),
        )
        val cancelled = hold(status = HoldStatus.Cancelled, cancelledBy = Party.Guardian)
        assertEquals(PaymentState.Stopped(cancelled, Party.Guardian, LedgerTime(1_060)), stateOf(cancelled, LedgerTime(1_500)))
    }
}
