package com.guardpay.shared.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.stateOf
import com.guardpay.shared.ui.components.Body
import com.guardpay.shared.ui.components.ButtonKind
import com.guardpay.shared.ui.components.Collapsible
import com.guardpay.shared.ui.components.Field
import com.guardpay.shared.ui.components.GpButton
import com.guardpay.shared.ui.components.GpCard
import com.guardpay.shared.ui.components.GpSheet
import com.guardpay.shared.ui.components.GpText
import com.guardpay.shared.ui.components.GuardianBand
import com.guardpay.shared.ui.components.LocalGpLayout
import com.guardpay.shared.ui.components.Note
import com.guardpay.shared.ui.components.NoticeCard
import com.guardpay.shared.ui.components.ProofCard
import com.guardpay.shared.ui.components.ReadLine
import com.guardpay.shared.ui.components.ScreenHeader
import com.guardpay.shared.ui.components.ScreenScaffold
import com.guardpay.shared.ui.components.SectionTitle
import com.guardpay.shared.ui.components.SimulationBanner
import com.guardpay.shared.ui.components.StateChip
import com.guardpay.shared.ui.components.StellarSeal
import com.guardpay.shared.ui.components.Title
import com.guardpay.shared.ui.components.WaitLine
import com.guardpay.shared.ui.components.rememberDisplayNow
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.model.Viewpoint
import com.guardpay.shared.ui.model.chipFor
import com.guardpay.shared.ui.model.clock
import com.guardpay.shared.ui.model.phraseFor
import com.guardpay.shared.ui.model.proofRows
import com.guardpay.shared.ui.model.showSeal
import com.guardpay.shared.ui.model.spokenAddress
import com.guardpay.shared.ui.model.usdc
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.GpSpace
import com.guardpay.shared.ui.theme.LocalGpType

@Composable
private fun GuardianTop(session: GuardianSession) {
    GuardianBand(session.config.ownerName)
    if (session.config.source == DataSource.Simulation) SimulationBanner()
}

/** The "Cuenta nueva" mark: a word on a neutral pill, never color alone. */
@Composable
private fun NewAccountTag() {
    Box(
        Modifier
            .background(GpColor.Navy8, RoundedCornerShape(50))
            .padding(horizontal = GpSpace.s12, vertical = GpSpace.s4),
    ) { GpText("Cuenta nueva", LocalGpType.current.label) }
}

/** E. Guardián (lista): the owner's waiting payments. No action here, so nobody stops without looking. */
@Composable
fun GuardianListScreen(session: GuardianSession, onOpen: (Long) -> Unit) {
    val s by session.ui.collectAsState()
    val cfg = session.config
    val now = rememberDisplayNow(s.ledgerNow, s.readMark)
    ScreenScaffold(top = { GuardianTop(session) }) {
        Title("Pagos de ${cfg.ownerName} en espera")
        s.notice?.let { NoticeCard(it) }
        if (s.ledgerNow == null) {
            Body("Todavía no se pudo leer la cuenta de ${cfg.ownerName}.")
        } else if (s.waiting.isEmpty()) {
            Body("Ningún pago de ${cfg.ownerName} está esperando ahora")
        }
        s.waiting.forEach { hold -> HoldRow(session, s, hold, now) { onOpen(hold.id) } }
        ReadLine(s.ledgerNow, s.lastReadOk, cfg.tz)
        // P8: never depend on background work alone; this reads the chain now.
        GpButton("Actualizar", { session.reload() }, kind = ButtonKind.Secondary)
        if (s.stoppedByYou.isNotEmpty()) {
            SectionTitle("Ya detuviste")
            s.stoppedByYou.forEach { hold -> HoldRow(session, s, hold, null) { onOpen(hold.id) } }
        }
    }
}

@Composable
private fun HoldRow(session: GuardianSession, s: GuardianUi, hold: HeldPayment, now: LedgerTime?, onClick: () -> Unit) {
    val type = LocalGpType.current
    val tz = session.config.tz
    Box(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        GpCard(leftBar = if (hold.status == HoldStatus.Held) GpColor.Warning else null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                GpText(hold.amount.usdc(), type.cardTitle, Modifier.weight(1f))
                if (s.isNewAccount(hold.destination)) NewAccountTag()
            }
            GpText(session.nameOf(hold.destination), type.mono)
            if (hold.status == HoldStatus.Held && now != null) WaitLine(hold, now, tz, null)
            else hold.cancelledAt?.let { Note("Detenido a las ${it.clock(tz)}") }
        }
    }
}

/** E2. Detalle (modo guardián): time and state, full destination, amount, creation, proof. One verb. */
@Composable
fun GuardianDetailScreen(session: GuardianSession, holdId: Long, onBack: () -> Unit) {
    val s by session.ui.collectAsState()
    val cfg = session.config
    val hold = s.holds.firstOrNull { it.id == holdId }
    val ledger = s.ledgerNow
    val now = rememberDisplayNow(s.ledgerNow, s.readMark)
    val state = if (hold != null && ledger != null) stateOf(hold, ledger) else null
    val view = Viewpoint(Party.Guardian, cfg.ownerName, cfg.guardianName, cfg.tz, session::nameOf)
    val idle = s.phase == GuardianPhase.Idle
    val chain = cfg.source == DataSource.Chain

    val proof: @Composable ColumnScope.() -> Unit = {
        Collapsible("Ver en Stellar") {
            if (chain) ProofCard(proofRows(hold, s.txs[holdId].orEmpty(), s.rejected[holdId].orEmpty(), cfg.links, cfg.tz), seal = s.lastReadOk)
            else Note("Simulación: no hay datos de Stellar que mostrar.")
        }
    }
    // Only while it waits. No disabled button otherwise: it would suggest a power that does not exist.
    val bottom: (@Composable ColumnScope.() -> Unit)? =
        if (hold?.status == HoldStatus.Held) ({ GpButton("Detener este pago", { session.askStop(holdId) }, kind = ButtonKind.DangerOutline, enabled = idle) })
        else null

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(top = { GuardianTop(session) }, bottom = bottom, side = proof) {
            ScreenHeader("Pago de ${cfg.ownerName}", onBack)
            s.notice?.let { NoticeCard(it) }
            if (hold == null || state == null) {
                Body("Todavía no se pudo leer este pago.")
                ReadLine(s.ledgerNow, s.lastReadOk, cfg.tz)
                return@ScreenScaffold
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                chipFor(state)?.let { StateChip(it) }
                if (showSeal(state, cfg.source, s.lastReadOk)) StellarSeal(null)
            }
            Body(phraseFor(state, view))
            if (hold.status == HoldStatus.Held && now != null) WaitLine(hold, now, cfg.tz, null)

            Field("Destino", hold.destination.value, mono = true, spoken = spokenAddress(hold.destination.value))
            Body(
                when {
                    s.isContact(hold.destination) -> "Contacto de confianza"
                    s.isNewAccount(hold.destination) -> "Cuenta nueva: ${cfg.ownerName} nunca le ha pagado"
                    else -> "${cfg.ownerName} ya le pagó antes"
                },
            )
            Field("Monto", hold.amount.usdc())
            Field("Lo creó ${cfg.ownerName}", hold.createdAt.clock(cfg.tz))
            ReadLine(s.ledgerNow, s.lastReadOk, cfg.tz)
            // The AI reading stays on the owner's phone: without a backend the guardian has none to show.
            if (!LocalGpLayout.current.sidePanel) proof()
        }
        if (s.confirmFor == holdId) {
            GpSheet("¿Detienes este pago?") {
                Body(
                    "¿Detienes este pago de ${hold?.amount?.usdc() ?: ""}? ${cfg.ownerName} no podrá enviarlo. " +
                        "Si de verdad quiere pagar, tendrá que crear uno nuevo y esperar otra vez. Esto no se puede deshacer.",
                )
                val label = when (s.phase) {
                    GuardianPhase.Idle -> "Detener el pago"
                    GuardianPhase.WaitingKey -> "Firmando…"
                    GuardianPhase.Sending -> "Enviando a Stellar…"
                }
                GpButton(label, session::confirmStop, kind = ButtonKind.DangerFilled, enabled = idle)
                GpButton("Volver", session::closeStop, kind = ButtonKind.Secondary, enabled = idle)
            }
        }
    }
}
