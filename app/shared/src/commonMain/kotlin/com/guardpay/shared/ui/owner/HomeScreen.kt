package com.guardpay.shared.ui.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.stellar.AccountShape
import com.guardpay.shared.ui.PaymentKey
import com.guardpay.shared.ui.components.Avatar
import com.guardpay.shared.ui.components.Body
import com.guardpay.shared.ui.components.Collapsible
import com.guardpay.shared.ui.components.GpButton
import com.guardpay.shared.ui.components.GpCard
import com.guardpay.shared.ui.components.GpLink
import com.guardpay.shared.ui.components.GpText
import com.guardpay.shared.ui.components.Note
import com.guardpay.shared.ui.components.NoticeCard
import com.guardpay.shared.ui.components.ReadLine
import com.guardpay.shared.ui.components.ScreenScaffold
import com.guardpay.shared.ui.components.SectionTitle
import com.guardpay.shared.ui.components.SimulationBanner
import com.guardpay.shared.ui.components.StateChip
import com.guardpay.shared.ui.components.StellarSeal
import com.guardpay.shared.ui.components.WaitLine
import com.guardpay.shared.ui.components.rememberDisplayNow
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.model.chipFor
import com.guardpay.shared.ui.model.clock
import com.guardpay.shared.ui.model.display
import com.guardpay.shared.ui.model.usdc
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.GpSpace
import com.guardpay.shared.ui.theme.LocalGpType

/** B. Inicio: what is happening with my money now, who looks after it, and where to check it. */
@Composable
fun HomeScreen(session: OwnerSession, onOpen: (PaymentKey) -> Unit) {
    val s by session.ui.collectAsState()
    val cfg = session.config
    val type = LocalGpType.current
    val now = rememberDisplayNow(s.ledgerNow, s.readMark)
    val chain = cfg.source == DataSource.Chain

    ScreenScaffold(
        top = { if (!chain) SimulationBanner() },
        bottom = { GpButton("Pagar", session::newPayment) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(GpSpace.s4)) {
            Note("Saldo")
            GpText(s.balance?.let { "${it.display()} USDC" } ?: "—", type.amount)
            if (s.rules != null) {
                val n = s.waiting.size
                val held = when (n) {
                    0 -> "sin pagos en espera"
                    1 -> "1 pago retenido"
                    else -> "$n pagos retenidos"
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                    Body((if (chain) "Reglas activas en Stellar" else "Reglas activas en la simulación") + " · $held", Modifier.weight(1f))
                    if (chain && s.lastReadOk) StellarSeal(null)
                }
            }
        }
        s.notice?.let { notice ->
            NoticeCard(notice)
            GpLink("Entendido", session::dismissNotice)
        }

        GpCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s12)) {
                Avatar()
                Body("${cfg.guardianName} te acompaña. Puede detener pagos retenidos, no moverlos.")
            }
        }

        if (s.waiting.isNotEmpty()) {
            SectionTitle("En espera")
            s.waiting.forEach { hold ->
                Box(Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(PaymentKey.Hold(hold.id)) }) {
                    GpCard(leftBar = GpColor.Warning) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GpText(session.nameOf(hold.destination), type.cardTitle, Modifier.weight(1f))
                            GpText(hold.amount.usdc(), type.cardTitle)
                        }
                        if (now != null) WaitLine(hold, now, cfg.tz, "${cfg.guardianName} puede detenerlo")
                        Note("Creado a las ${hold.createdAt.clock(cfg.tz)}")
                    }
                }
            }
        }

        val recent = s.recent()
        if (recent.isNotEmpty()) {
            SectionTitle("Recientes")
            recent.forEach { key ->
                val state = s.stateOf(key) ?: return@forEach
                val intent = state.intentOrNull() ?: return@forEach
                Box(Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(key) }) {
                    GpCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                            GpText(session.nameOf(intent.destination), type.body, Modifier.weight(1f))
                            GpText(intent.amount.usdc(), type.body)
                        }
                        chipFor(state)?.let { StateChip(it) }
                    }
                }
            }
        }

        if (s.contacts.isNotEmpty()) {
            SectionTitle("Contactos de confianza")
            GpCard {
                s.contacts.take(3).forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                        Avatar()
                        Body(cfg.contactNames[c.address] ?: c.name)
                    }
                }
                s.capLeft()?.let { Note("Te quedan ${it.usdc()} de tu tope de hoy") }
            }
        }

        Collapsible("Reglas de esta cuenta") {
            AccountRulesSection(session, s)
        }
        ReadLine(s.ledgerNow, s.lastReadOk, cfg.tz)
    }
}

/**
 * The account's shape as read from the chain: rule count, signers, policies, and
 * whether the guardian is a signer. It shows what was read; it decides nothing.
 */
@Composable
private fun AccountRulesSection(session: OwnerSession, s: OwnerUi) {
    val cfg = session.config
    val uri = LocalUriHandler.current
    val shape: AccountShape? = s.shape
    val guardian: StellarAddress? = s.rules?.guardian
    if (shape == null) {
        Body(if (s.shapeFailed) "No se pudieron leer las reglas de esta cuenta." else "Leyendo las reglas…")
        return
    }
    val guardianSigns = guardian != null && guardian in shape.signers
    GpCard(leftBar = if (guardianSigns || shape.contextRuleCount != 1) GpColor.Danger else GpColor.Teal, leftBarWidth = if (guardianSigns) 4.dp else 3.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
            GpText(
                "Esta cuenta tiene ${shape.contextRuleCount} " + if (shape.contextRuleCount == 1) "regla" else "reglas",
                LocalGpType.current.cardTitle, Modifier.weight(1f),
            )
            if (cfg.source == DataSource.Chain && !s.shapeFailed) StellarSeal(null)
        }
        shape.signers.forEach { a ->
            Body(
                "Firmante: " + when (a) {
                    session.ownerId -> "la clave de este teléfono"
                    guardian -> "tu guardián (${a.short()})"
                    else -> a.short()
                },
            )
        }
        shape.policies.forEach { a ->
            Body("Política: " + if (a == cfg.links.policy) "GuardianHold" else a.short())
        }
        if (guardian != null) {
            if (guardianSigns) {
                Body("Tu guardián aparece como firmante. Así podría mover tu dinero. No pagues hasta revisarlo.", color = GpColor.Danger)
            } else {
                Body("Tu guardián: ${cfg.guardianName} (${guardian.short()}). No es firmante de esta cuenta.")
            }
        }
        cfg.links.codeUrl?.let { code ->
            Body("Sin funciones para cambiar reglas, actualizar el código ni ejecutar llamadas arbitrarias.")
            GpLink("Ver el código", { uri.openUri(code) })
        }
        cfg.links.testsUrl?.let { tests -> GpLink("Ver las pruebas", { uri.openUri(tests) }) }
    }
}
