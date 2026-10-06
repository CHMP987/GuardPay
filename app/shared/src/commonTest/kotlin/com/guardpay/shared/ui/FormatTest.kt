package com.guardpay.shared.ui

import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.PaymentState
import com.guardpay.shared.domain.hold
import com.guardpay.shared.domain.usdc
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.SigningOrigin
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.domain.addr
import com.guardpay.shared.domain.ACCOUNT
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.model.Viewpoint
import com.guardpay.shared.ui.model.chipFor
import com.guardpay.shared.ui.model.clock
import com.guardpay.shared.ui.model.display
import com.guardpay.shared.ui.model.parseUsdc
import com.guardpay.shared.ui.model.phraseFor
import com.guardpay.shared.ui.model.remaining
import com.guardpay.shared.ui.model.showSeal
import com.guardpay.shared.ui.model.spokenAddress
import com.guardpay.shared.ui.owner.addressProblem
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatTest {
    private val utc = TimeZone.UTC
    private fun view(p: Party) = Viewpoint(p, "Laura", "Diego", utc) { "Ana" }

    @Test
    fun amountsUseSpanishSeparatorsAndNeverRound() {
        assertEquals("1.250", usdc(1_250).display())
        assertEquals("0,0000001", UsdcAmount(1).display())
        assertEquals("150,5", UsdcAmount(1_505_000_000).display())
    }

    @Test
    fun parsesWhatPeopleType() {
        assertEquals(usdc(150), parseUsdc("150"))
        assertEquals(UsdcAmount(1_505_000_000), parseUsdc("150,5"))
        assertEquals(UsdcAmount(1_505_000_000), parseUsdc(" 150.5 "))
        assertNull(parseUsdc("0"))
        assertNull(parseUsdc("1,12345678"))
        assertNull(parseUsdc("1.000,5"))
        assertNull(parseUsdc("-3"))
        assertNull(parseUsdc(""))
    }

    @Test
    fun timesComeFromTheLedger() {
        assertEquals("14:32", LedgerTime(52_320).clock(utc))
        assertEquals("Faltan 1 h 05 min", remaining(LedgerTime(0), LedgerTime(3_900)))
        assertEquals("Faltan 12 min", remaining(LedgerTime(0), LedgerTime(720)))
        assertEquals("Falta menos de 1 min", remaining(LedgerTime(0), LedgerTime(59)))
        assertEquals("dirección que termina en AAAA", spokenAddress(addr('A').value))
    }

    @Test
    fun addressProblems() {
        assertNull(addressProblem("", ACCOUNT))
        assertNotNull(addressProblem("M" + "A".repeat(68), ACCOUNT))
        assertNotNull(addressProblem("GABC", ACCOUNT))
        assertEquals("Es tu propia cuenta.", addressProblem(ACCOUNT.value, ACCOUNT))
        assertNull(addressProblem(addr('B').value, ACCOUNT))
    }

    @Test
    fun everyStateHasAWordNotJustAColor() {
        val h = hold()
        val states = listOf(
            PaymentState.Held(h), PaymentState.ReadyToSend(h), PaymentState.Sent(h.intent, null, h),
            PaymentState.Stopped(h, Party.Guardian, null), PaymentState.Expired(h),
            PaymentState.Rejected(h.intent, null), PaymentState.NetworkError(SigningOrigin.Release(h)),
        )
        val labels = states.map { assertNotNull(chipFor(it)).label }
        assertEquals(
            listOf("Retenido", "Listo para enviar", "Enviado", "Detenido", "Vencido", "Rechazado por el contrato", "Error de red"),
            labels,
        )
        assertNull(chipFor(PaymentState.Draft(h.intent)))
    }

    @Test
    fun phrasesDependOnWhoIsLooking() {
        val h = hold(readyAt = 52_320, expiresAt = null)
        assertEquals("Retenido hasta las 14:32. Diego puede detenerlo.", phraseFor(PaymentState.Held(h), view(Party.Owner)))
        assertEquals("Retenido hasta las 14:32. Puedes detenerlo.", phraseFor(PaymentState.Held(h), view(Party.Guardian)))
        val stopped = PaymentState.Stopped(hold(HoldStatus.Cancelled, cancelledBy = Party.Guardian), Party.Guardian, LedgerTime(52_320))
        assertEquals("Detenido por Diego a las 14:32", phraseFor(stopped, view(Party.Owner)))
        assertEquals("Detenido por ti a las 14:32", phraseFor(stopped, view(Party.Guardian)))
        assertEquals("Listo. Necesita tu firma para salir.", phraseFor(PaymentState.ReadyToSend(h), view(Party.Owner)))
    }

    @Test
    fun theSealNeedsChainDataAndAFreshRead() {
        val held = PaymentState.Held(hold())
        assertTrue(showSeal(held, DataSource.Chain, lastReadOk = true))
        assertFalse(showSeal(held, DataSource.Chain, lastReadOk = false))
        assertFalse(showSeal(held, DataSource.Simulation, lastReadOk = true))
        assertFalse(showSeal(PaymentState.NetworkError(SigningOrigin.Release(hold())), DataSource.Chain, true))
    }
}
