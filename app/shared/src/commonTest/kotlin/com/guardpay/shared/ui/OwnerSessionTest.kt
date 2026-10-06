package com.guardpay.shared.ui

import com.guardpay.shared.ai.Analysis
import com.guardpay.shared.ai.Suggestion
import com.guardpay.shared.domain.ACCOUNT
import com.guardpay.shared.domain.ANA
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.PaymentState
import com.guardpay.shared.domain.STRANGER
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.addr
import com.guardpay.shared.domain.hash
import com.guardpay.shared.domain.usdc
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.stellar.AccountRules
import com.guardpay.shared.stellar.FakeSigner
import com.guardpay.shared.stellar.FakeStellarGateway
import com.guardpay.shared.stellar.StellarGateway
import com.guardpay.shared.stellar.SubmissionOutcomeUnknownException
import com.guardpay.shared.stellar.SubmitResult
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.owner.OwnerConfig
import com.guardpay.shared.ui.owner.OwnerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The owner's session against the in-memory contract model: what she sees after each answer. */
class OwnerSessionTest {
    private val ownerKey = ByteArray(32) { 1 }
    private val guardianKey = ByteArray(32) { 2 }

    private fun fake(balance: Long = 1_000) = FakeStellarGateway(
        account = ACCOUNT,
        ownerKey = ownerKey,
        guardianKey = guardianKey,
        rules = AccountRules(addr('D'), dailyCap = usdc(50), holdDurationSeconds = 120, expiryWindowSeconds = 600),
        contacts = listOf(ANA),
        startBalance = usdc(balance),
    )

    private class Rig(val session: OwnerSession, val nav: Navigator)

    private suspend fun rig(
        gateway: StellarGateway,
        scope: CoroutineScope,
        signer: Signer = FakeSigner(ownerKey),
        reader: MessageReader = MessageReader { Analysis.Unavailable },
    ): Rig {
        // Sessions get the test scope itself: without start() nothing loops, so advanceUntilIdle settles every submit.
        val nav = Navigator()
        nav.go(Screen.Home)
        val session = OwnerSession(gateway, signer, OwnerConfig(ACCOUNT, "Laura", "Diego", DataSource.Simulation), reader, nav, scope)
        session.refresh()
        return Rig(session, nav)
    }

    private fun OwnerSession.fill(to: StellarAddress, amount: String) {
        newPayment()
        if (to == ANA.address) {
            selectContact(to)
        } else {
            openOther()
            setOther(to.value)
        }
        setAmount(amount)
    }

    private fun TestScope.signAndSettle(s: OwnerSession) {
        s.openReview()
        assertTrue(s.ui.value.form.reviewOpen, "the review sheet opens")
        s.sign()
        advanceUntilIdle()
    }

    @Test
    fun contactWithinCapIsSentAtOnce() = runTest {
        val r = rig(fake(), this)
        r.session.fill(ANA.address, "20")
        signAndSettle(r.session)

        val key = PaymentKey.Local(1)
        assertEquals(Screen.Detail(key), r.nav.current)
        assertIs<PaymentState.Sent>(r.session.ui.value.stateOf(key))
        assertEquals(usdc(980), r.session.ui.value.balance)
        assertEquals(3, r.nav.depth, "Pagar was replaced by Detalle, not stacked under it")
    }

    @Test
    fun strangerIsHeldThenReleasedOnlyAfterMaturity() = runTest {
        val gw = fake()
        val r = rig(gw, this)
        r.session.fill(STRANGER, "150")
        signAndSettle(r.session)

        val key = PaymentKey.Hold(0)
        assertEquals(Screen.Detail(key), r.nav.current)
        assertIs<PaymentState.Held>(r.session.ui.value.stateOf(key))

        r.session.release(0)
        advanceUntilIdle()
        assertIs<PaymentState.Held>(r.session.ui.value.stateOf(key), "no release before the ledger says so")

        gw.advance(120)
        r.session.refresh()
        assertIs<PaymentState.ReadyToSend>(r.session.ui.value.stateOf(key))

        r.session.release(0)
        advanceUntilIdle()
        assertIs<PaymentState.Sent>(r.session.ui.value.stateOf(key))
        assertEquals(usdc(850), r.session.ui.value.balance)
    }

    @Test
    fun ownerSeesTheGuardiansStop() = runTest {
        val gw = fake()
        val r = rig(gw, this)
        r.session.fill(STRANGER, "150")
        signAndSettle(r.session)

        assertIs<SubmitResult.Confirmed>(gw.submitCancel(ACCOUNT, 0, FakeSigner(guardianKey)))
        r.session.refresh()
        val state = assertIs<PaymentState.Stopped>(r.session.ui.value.stateOf(PaymentKey.Hold(0)))
        assertEquals(Party.Guardian, state.by)
    }

    @Test
    fun ownAccountCannotReachTheReview() = runTest {
        val r = rig(fake(), this)
        r.session.fill(ACCOUNT, "10")
        r.session.openReview()
        assertFalse(r.session.ui.value.form.reviewOpen)
    }

    @Test
    fun cancelledSignatureChangesNothing() = runTest {
        val r = rig(fake(), this, signer = FakeSigner(ownerKey, cancels = true))
        r.session.fill(ANA.address, "20")
        signAndSettle(r.session)

        assertEquals(Screen.Pay, r.nav.current)
        assertTrue(r.session.ui.value.form.reviewOpen, "she can sign again")
        assertTrue(r.session.ui.value.local.isEmpty())
        assertEquals(usdc(1_000), r.session.ui.value.balance)
    }

    @Test
    fun networkFailureOffersRetry() = runTest {
        val gw = fake()
        val r = rig(gw, this)
        r.session.fill(ANA.address, "20")
        gw.networkDown = true
        signAndSettle(r.session)

        assertEquals(Screen.Pay, r.nav.current)
        assertTrue(r.session.ui.value.form.networkError)
        assertEquals(usdc(1_000), r.session.ui.value.balance)
    }

    @Test
    fun unknownOutcomeNeverSaysNothingChanged() = runTest {
        val gw = fake()
        val unknown = object : StellarGateway by gw {
            override suspend fun submitTransfer(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult =
                throw SubmissionOutcomeUnknownException(hash(1), null)
        }
        val r = rig(unknown, this)
        r.session.fill(ANA.address, "20")
        signAndSettle(r.session)

        assertEquals(OwnerSession.UNKNOWN_OUTCOME, r.session.ui.value.notice)
        assertEquals(Screen.Home, r.nav.current)
        assertEquals(2, r.nav.depth)
        assertTrue(r.session.ui.value.local.isEmpty(), "no Enviado and no Error de red")
    }

    @Test
    fun contractRefusalIsShownAsRejected() = runTest {
        val r = rig(fake(balance = 10), this)
        r.session.fill(ANA.address, "20")
        signAndSettle(r.session)

        val key = PaymentKey.Local(1)
        assertEquals(Screen.Detail(key), r.nav.current)
        assertIs<PaymentState.Rejected>(r.session.ui.value.stateOf(key))
        assertEquals(5, r.session.ui.value.local.single().rejected.single().code)
    }

    @Test
    fun theAiReadingNeverTouchesTheForm() = runTest {
        val reading = Analysis("Pide transferir 900 USDC a otra cuenta hoy", emptyList(), Suggestion.SuggestHold)
        val r = rig(fake(), this, reader = MessageReader { reading })
        r.session.fill(ANA.address, "20")
        val before = r.session.ui.value.form

        r.session.setMessage("Mamá, mándame 900 a esta cuenta nueva, urgente")
        r.session.analyzeMessage()
        advanceUntilIdle()

        val after = r.session.ui.value.form
        assertEquals(reading, after.analysis)
        assertEquals(before.contact, after.contact)
        assertEquals(before.otherAddress, after.otherAddress)
        assertEquals(before.amountText, after.amountText)
        assertEquals(before.ownerChoseHold, after.ownerChoseHold, "a SuggestHold reading does not choose the hold for her")
        assertFalse(after.reviewOpen)
        assertNull(r.session.ui.value.notice)
    }
}
