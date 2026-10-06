package com.guardpay.shared.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.PaymentState
import com.guardpay.shared.domain.SigningOrigin
import com.guardpay.shared.domain.TxHash
import com.guardpay.shared.ui.PaymentKey
import com.guardpay.shared.ui.components.AiCard
import com.guardpay.shared.ui.components.Body
import com.guardpay.shared.ui.components.ButtonKind
import com.guardpay.shared.ui.components.Collapsible
import com.guardpay.shared.ui.components.Field
import com.guardpay.shared.ui.components.GpButton
import com.guardpay.shared.ui.components.GpSheet
import com.guardpay.shared.ui.components.LocalGpLayout
import com.guardpay.shared.ui.components.Note
import com.guardpay.shared.ui.components.NoticeCard
import com.guardpay.shared.ui.components.ProofCard
import com.guardpay.shared.ui.components.ReadLine
import com.guardpay.shared.ui.components.ScreenHeader
import com.guardpay.shared.ui.components.ScreenScaffold
import com.guardpay.shared.ui.components.SimulationBanner
import com.guardpay.shared.ui.components.StateChip
import com.guardpay.shared.ui.components.StellarSeal
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
import com.guardpay.shared.ui.theme.GpSpace

/** The intent behind any state, for "Destino" and "Monto". */
internal fun PaymentState.intentOrNull(): PaymentIntent? = when (this) {
    is PaymentState.Draft -> intent
    is PaymentState.Signing -> when (val o = origin) {
        is SigningOrigin.NewPayment -> o.intent
        is SigningOrigin.Release -> o.hold.intent
    }
    is PaymentState.Held -> hold.intent
    is PaymentState.ReadyToSend -> hold.intent
    is PaymentState.Sent -> intent
    is PaymentState.Stopped -> hold.intent
    is PaymentState.Expired -> hold.intent
    is PaymentState.Rejected -> intent
    is PaymentState.NetworkError -> when (val o = origin) {
        is SigningOrigin.NewPayment -> o.intent
        is SigningOrigin.Release -> o.hold.intent
    }
}

/** The transaction "Ver en Stellar" opens: the one that decided the state. */
internal fun PaymentState.decidingTx(): TxHash? = when (this) {
    is PaymentState.Sent -> txHash
    is PaymentState.Rejected -> txHash
    else -> null
}

/** D. Detalle del pago (dueña): state and next step first, then the wait line, then facts, then proof. */
@Composable
fun DetailScreen(session: OwnerSession, key: PaymentKey, onBack: () -> Unit) {
    val s by session.ui.collectAsState()
    val cfg = session.config
    val uri = LocalUriHandler.current
    val state = s.stateOf(key)
    val hold = s.holdOf(key)
    val local = (key as? PaymentKey.Local)?.let { k -> s.local.firstOrNull { it.key == k } }
    val extra = hold?.let { s.extras[it.id] }
    val intent = state?.intentOrNull()
    val name = intent?.let { session.nameOf(it.destination) } ?: "…"
    val now = rememberDisplayNow(s.ledgerNow, s.readMark)
    val view = Viewpoint(Party.Owner, cfg.ownerName, cfg.guardianName, cfg.tz, session::nameOf)
    val idle = s.phase == SigningPhase.Idle
    val chain = cfg.source == DataSource.Chain
    val txs = local?.txs ?: extra?.txs.orEmpty()
    val rejected = local?.rejected ?: extra?.rejected.orEmpty()
    val analysis = local?.analysis ?: extra?.analysis

    val proof: @Composable ColumnScope.() -> Unit = {
        Collapsible("Ver en Stellar") {
            if (chain) ProofCard(proofRows(hold, txs, rejected, cfg.links, cfg.tz), seal = s.lastReadOk)
            else Note("Simulación: no hay datos de Stellar que mostrar.")
        }
    }

    val bottom: (@Composable ColumnScope.() -> Unit)? = when (state) {
        is PaymentState.Held -> {
            {
                GpButton("Podrás enviarlo a las ${state.hold.readyAt.clock(cfg.tz)}", {}, enabled = false)
                if (cfg.ownerMayStop) GpButton("Detener mi pago", { session.askStop(state.hold.id) }, kind = ButtonKind.DangerOutline, enabled = idle)
            }
        }
        is PaymentState.ReadyToSend -> {
            {
                GpButton(signLabel(s.phase, "Firmar y enviar"), { session.release(state.hold.id) }, enabled = idle)
                if (cfg.ownerMayStop) GpButton("Detener mi pago", { session.askStop(state.hold.id) }, kind = ButtonKind.DangerOutline, enabled = idle)
            }
        }
        is PaymentState.NetworkError -> {
            val id = hold?.id
            if (id == null) null else {
                {
                    GpButton(signLabel(s.phase, "Reintentar"), { session.release(id) }, enabled = idle)
                    GpButton("Cerrar", { session.dismissError(id) }, kind = ButtonKind.Secondary, enabled = idle)
                }
            }
        }
        is PaymentState.Sent, is PaymentState.Rejected -> {
            val url = state.decidingTx()?.let { cfg.links.tx(it) }
            if (chain && url != null) ({ GpButton("Ver en Stellar", { uri.openUri(url) }, kind = ButtonKind.Secondary) }) else null
        }
        is PaymentState.Stopped, is PaymentState.Expired -> ({ GpButton("Crear un pago nuevo", session::newPayment) })
        else -> null
    }

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            top = { if (cfg.source == DataSource.Simulation) SimulationBanner() },
            bottom = bottom,
            side = proof,
        ) {
            ScreenHeader("Pago a $name", onBack)
            s.notice?.let { NoticeCard(it) }
            if (state == null) {
                Body("Todavía no se pudo leer este pago.")
                ReadLine(s.ledgerNow, s.lastReadOk, cfg.tz)
                return@ScreenScaffold
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                chipFor(state)?.let { StateChip(it) }
                if (showSeal(state, cfg.source, s.lastReadOk)) {
                    val url = state.decidingTx()?.let { cfg.links.tx(it) }
                    StellarSeal(url?.let { u -> { uri.openUri(u) } })
                }
            }
            val sentAt = extra?.sentAt?.clock(cfg.tz) ?: local?.at?.takeIf { state is PaymentState.Sent }?.clock(cfg.tz)
            Body(phraseFor(state, view, sentAt, s.rejectedAfterStop(key)))

            if (hold != null && now != null && hold.status == HoldStatus.Held) {
                WaitLine(hold, now, cfg.tz, "${cfg.guardianName} puede detenerlo")
            } else if (hold != null && now != null && hold.status == HoldStatus.Executed) {
                WaitLine(hold, now, cfg.tz, null, reachedSent = true)
            }

            if (intent != null) {
                if (session.config.contactNames[intent.destination] != null || s.contacts.any { it.address == intent.destination }) {
                    Field("Destino", name)
                } else {
                    Field("Destino", intent.destination.value, mono = true, spoken = spokenAddress(intent.destination.value))
                }
                Field("Monto", intent.amount.usdc())
            }
            if (hold != null) Field("Creado", hold.createdAt.clock(cfg.tz))
            Field("Guardián", cfg.guardianName)
            ReadLine(s.ledgerNow, s.lastReadOk, cfg.tz)

            analysis?.let { AiCard(it, null) }
            if (!LocalGpLayout.current.sidePanel) proof()
        }
        val stopId = s.stopSheetFor
        val stopHold = stopId?.let { id -> s.holds.firstOrNull { it.id == id } }
        if (stopHold != null) {
            GpSheet("¿Detienes este pago?") {
                Body(
                    "¿Detienes este pago de ${stopHold.amount.usdc()}? No podrás enviarlo. Si quieres pagar, " +
                        "tendrás que crear uno nuevo y esperar otra vez. Esto no se puede deshacer.",
                )
                GpButton(signLabel(s.phase, "Detener el pago"), session::confirmStop, kind = ButtonKind.DangerFilled, enabled = idle)
                GpButton("Volver", session::closeStop, kind = ButtonKind.Secondary, enabled = idle)
            }
        }
    }
}

internal fun signLabel(phase: SigningPhase, idle: String) = when (phase) {
    SigningPhase.Idle -> idle
    SigningPhase.WaitingPasskey -> "Esperando tu passkey…"
    SigningPhase.Sending -> "Enviando a Stellar…"
}
