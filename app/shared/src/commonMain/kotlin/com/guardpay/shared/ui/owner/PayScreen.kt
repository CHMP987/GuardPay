package com.guardpay.shared.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guardpay.shared.domain.HoldReason
import com.guardpay.shared.domain.LanePrediction
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.ui.components.AiCard
import com.guardpay.shared.ui.components.Avatar
import com.guardpay.shared.ui.components.Body
import com.guardpay.shared.ui.components.ButtonKind
import com.guardpay.shared.ui.components.Collapsible
import com.guardpay.shared.ui.components.Field
import com.guardpay.shared.ui.components.GpButton
import com.guardpay.shared.ui.components.GpCard
import com.guardpay.shared.ui.components.GpIcon
import com.guardpay.shared.ui.components.GpLink
import com.guardpay.shared.ui.components.GpSheet
import com.guardpay.shared.ui.components.GpText
import com.guardpay.shared.ui.components.GpTextField
import com.guardpay.shared.ui.components.Note
import com.guardpay.shared.ui.components.ScreenHeader
import com.guardpay.shared.ui.components.ScreenScaffold
import com.guardpay.shared.ui.components.SimulationBanner
import com.guardpay.shared.ui.components.StateChip
import com.guardpay.shared.ui.model.ChipModel
import com.guardpay.shared.ui.model.ChipTone
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.model.clock
import com.guardpay.shared.ui.model.spokenAddress
import com.guardpay.shared.ui.model.usdc
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.GpIcons
import com.guardpay.shared.ui.theme.GpSize
import com.guardpay.shared.ui.theme.GpSpace
import com.guardpay.shared.ui.theme.LocalGpType

/** Why the typed address cannot be used, or null. Shape only: the contract is the real check. */
fun addressProblem(text: String, own: StellarAddress): String? = when {
    text.isBlank() -> null
    text.startsWith("M") -> "Las direcciones M… no se aceptan. Pide la dirección G… de la cuenta."
    !StellarAddress.isValid(text) -> "Esta dirección no es válida. Revisa que esté completa."
    text == own.value -> "Es tu propia cuenta."
    else -> null
}

/** When a payment queued now would mature, if the contract keeps the duration it reported. */
internal fun OwnerUi.estimatedReady(): LedgerTime? {
    val now = ledgerNow ?: return null
    val d = rules?.holdDurationSeconds ?: return null
    return LedgerTime(now.epochSeconds + d)
}

/** C. Pagar: "¿A quién le pagas?", the amount, the lane card, and the optional AI reading. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PayScreen(session: OwnerSession, onBack: () -> Unit) {
    val s by session.ui.collectAsState()
    val cfg = session.config
    val form = s.form
    val type = LocalGpType.current
    val other = form.otherAddress
    val otherProblem = if (form.otherOpen) addressProblem(other, cfg.account) else null
    val amount = s.formAmount()
    val overBalance = amount != null && s.balance != null && amount.units > s.balance!!.units
    val prediction = s.prediction()
    val ownAccount = s.formDestination() == cfg.account
    val canReview = prediction != null && !overBalance && !ownAccount && s.phase == SigningPhase.Idle

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            top = { if (cfg.source == DataSource.Simulation) SimulationBanner() },
            bottom = { GpButton("Revisar", session::openReview, enabled = canReview) },
        ) {
            ScreenHeader("¿A quién le pagas?", onBack)
            Note("Este pago aún no se envió")

            FlowRow(horizontalArrangement = Arrangement.spacedBy(GpSpace.s8), verticalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                s.contacts.forEach { c ->
                    ContactChip(cfg.contactNames[c.address] ?: c.name, form.contact == c.address) { session.selectContact(c.address) }
                }
                ContactChip("Otra cuenta", form.otherOpen, avatar = false, onClick = session::openOther)
            }
            if (form.otherOpen) {
                GpTextField(
                    other, session::setOther, "Otra cuenta",
                    mono = true, placeholder = "G…", error = otherProblem,
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
            }

            GpTextField(
                form.amountText, session::setAmount, "Monto (USDC)",
                big = true, placeholder = "0",
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                error = when {
                    overBalance -> "Tu saldo es ${s.balance!!.usdc()}."
                    form.amountText.isNotBlank() && amount == null -> "Escribe un monto como 150 o 150,50."
                    else -> null
                },
            )
            s.balance?.let { Note("Saldo: ${it.usdc()}") }

            if (prediction != null && !ownAccount) LaneCard(s, session, prediction)
            else if (s.formIntent() != null && s.rules == null) Note("Todavía no se pudieron leer las reglas de la cuenta.")

            Collapsible("¿Te pidieron este pago por mensaje? Pégalo y lo revisamos") {
                GpTextField(form.message, session::setMessage, "Mensaje", singleLine = false, placeholder = "Pega aquí el mensaje")
                GpButton(
                    if (form.analyzing) "Revisando…" else "Revisar el mensaje",
                    session::analyzeMessage,
                    kind = ButtonKind.Secondary,
                    enabled = form.message.isNotBlank() && !form.analyzing,
                )
                val a = form.analysis
                if (a != null && a.isUnavailable) Note("El análisis no está disponible ahora. Puedes pagar sin él.")
                if (a != null) {
                    val toContact = prediction is LanePrediction.Immediate
                    AiCard(a, if (toContact) session::chooseHold else null)
                }
            }
        }
        if (form.reviewOpen && prediction != null) ReviewSheet(s, session, prediction)
    }
}

@Composable
private fun ContactChip(name: String, selected: Boolean, avatar: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .heightIn(min = GpSize.touchTarget)
            .background(if (selected) GpColor.Navy8 else GpColor.White, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) GpColor.Navy else GpColor.Navy20, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(start = if (avatar) GpSpace.s4 else GpSpace.s16, end = GpSpace.s16, top = GpSpace.s4, bottom = GpSpace.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GpSpace.s8),
    ) {
        if (avatar) Avatar()
        GpText(name, LocalGpType.current.body)
    }
}

/** The lane card: green "Sale ahora", amber "Con espera". The only color on the screen. */
@Composable
private fun LaneCard(s: OwnerUi, session: OwnerSession, p: LanePrediction) {
    val type = LocalGpType.current
    val guardian = session.config.guardianName
    val tz = session.config.tz
    val readyAt = s.estimatedReady()?.clock(tz)
    val after = readyAt?.let { "saldrá después de las $it" } ?: "saldrá cuando termine la espera"
    when (p) {
        is LanePrediction.Immediate -> GpCard(leftBar = GpColor.Success) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                GpIcon(GpIcons.Check, GpColor.SuccessText)
                GpText("Sale ahora", type.cardTitle, color = GpColor.SuccessText)
            }
            Body("Contacto de confianza · te quedan ${p.remainingAfter.usdc()} de tu tope de hoy")
            GpLink("Retener este pago", session::toggleOwnerHold)
        }
        is LanePrediction.MustHold -> GpCard(leftBar = GpColor.Warning) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                GpIcon(GpIcons.Hourglass, GpColor.WarningText)
                GpText("Con espera", type.cardTitle, color = GpColor.WarningText)
            }
            Body("${after.replaceFirstChar { it.uppercase() }} si $guardian no lo detiene y tú lo confirmas.")
            when (p.reason) {
                HoldReason.NotTrustedContact -> Note("No es un contacto de confianza.")
                HoldReason.OverDailyCap -> Note(
                    "Supera tu tope de hoy con contactos" + (s.capLeft()?.let { ": te quedan ${it.usdc()}." } ?: "."),
                )
                HoldReason.OwnerChoseHold -> {
                    Note("Elegiste retenerlo.")
                    GpLink("Que salga ahora", session::toggleOwnerHold)
                }
            }
        }
    }
}

/** C2. "Revisa tu pago": exactly what the signature creates. Nothing says "retenido" before the record exists. */
@Composable
private fun ReviewSheet(s: OwnerUi, session: OwnerSession, p: LanePrediction) {
    val intent = s.formIntent() ?: return
    val cfg = session.config
    val dest = intent.destination
    val name = cfg.contactNames[dest] ?: s.contacts.firstOrNull { it.address == dest }?.name
    var full by remember(dest) { mutableStateOf(false) }
    val hold = p is LanePrediction.MustHold
    val readyAt = s.estimatedReady()?.clock(cfg.tz)

    GpSheet("Revisa tu pago") {
        if (name != null) Field("Destino", name)
        else {
            Field("Destino", if (full) dest.value else dest.short(), mono = true, spoken = spokenAddress(dest.value))
            if (!full) GpLink("ver completa", { full = true })
        }
        Field("Monto", intent.amount.usdc())
        Field("Activo", "USDC")
        Field("Carril", if (hold) "Con espera" else "Sale ahora")
        Body(
            if (hold) "Esperará en el contrato" + (readyAt?.let { " hasta las $it" } ?: "") +
                ". ${cfg.guardianName} podrá detenerlo. Después tendrás que firmar otra vez para enviarlo."
            else "Sale ahora hacia ${name ?: dest.short()}.",
        )
        if (s.form.networkError) {
            StateChip(ChipModel("Error de red", ChipTone.Neutral, GpIcons.WifiOff))
            Body("No llegó a Stellar. Nada cambió.")
        }
        if (s.notice == OwnerSession.SIGNING_FAILED) Body(OwnerSession.SIGNING_FAILED)
        val label = when (s.phase) {
            SigningPhase.WaitingPasskey -> "Esperando tu huella o PIN…"
            SigningPhase.Sending -> "Enviando a Stellar…"
            SigningPhase.Idle -> when {
                s.form.networkError -> "Reintentar"
                hold -> "Firmar y retener"
                else -> "Firmar y enviar"
            }
        }
        val idle = s.phase == SigningPhase.Idle
        GpButton(label, session::sign, enabled = idle)
        GpButton("Cambiar", session::closeReview, kind = ButtonKind.Secondary, enabled = idle)
    }
}
