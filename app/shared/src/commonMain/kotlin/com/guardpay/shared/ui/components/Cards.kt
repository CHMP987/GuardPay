package com.guardpay.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardpay.shared.ai.Analysis
import com.guardpay.shared.ai.Suggestion
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.GpIcons
import com.guardpay.shared.ui.theme.GpSize
import com.guardpay.shared.ui.theme.GpSpace
import com.guardpay.shared.ui.theme.LocalGpType

/**
 * Navy on White, left border Navy 20 %. What the message asks, up to 3 signals and
 * one suggestion. Its only action is "Retener este pago", and only when the payment
 * goes to a trusted contact ([onHold] non-null). It never fills the form.
 */
@Composable
fun AiCard(analysis: Analysis, onHold: (() -> Unit)?) {
    if (analysis.isUnavailable) return
    GpCard(leftBar = GpColor.Navy20, leftBarWidth = 3.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
            GpIcon(GpIcons.MessageSquareText, GpColor.Navy)
            GpText("Análisis de IA · puede equivocarse", LocalGpType.current.label)
        }
        if (analysis.whatItAsks.isNotBlank()) Body(analysis.whatItAsks)
        analysis.signals.forEach { Body("· ${it.text}") }
        val suggest = analysis.suggestion == Suggestion.SuggestHold
        GpText(
            if (suggest) "Te sugerimos retener este pago" else "No vemos señales claras",
            LocalGpType.current.body.copy(fontWeight = FontWeight.SemiBold),
        )
        if (suggest && onHold != null) GpButton("Retener este pago", onHold, kind = ButtonKind.Secondary)
    }
}

/** A heading that opens and closes its content. 44 dp target; screen readers hear the state. */
@Composable
fun Collapsible(
    title: String,
    initiallyOpen: Boolean = false,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = GpSize.touchTarget)
                .clickable(role = Role.Button) { open = !open }
                .semantics { stateDescription = if (open) "abierto" else "cerrado" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GpSpace.s8),
        ) {
            GpText(title, LocalGpType.current.cardTitle, Modifier.weight(1f))
            trailing()
            Image(
                GpIcons.ChevronDown, null,
                Modifier.size(GpSize.icon).rotate(if (open) 180f else 0f),
                colorFilter = ColorFilter.tint(GpColor.Navy),
            )
        }
        if (open) Column(verticalArrangement = Arrangement.spacedBy(GpSpace.s12), content = content)
    }
}

/** One row of "Ver en Stellar": the value in Mono and, in gray, what it proves. */
data class ProofRow(
    val label: String,
    val value: String,
    val proves: String,
    /** Full value for copying, when [value] is shortened. */
    val copyValue: String = value,
    /** Explorer link, only for chain data and only when an explorer is configured. */
    val url: String? = null,
)

/**
 * White card, 3 px Teal left border, title with the seal. Each row can be copied and,
 * with an explorer, opened. [seal] is false when the data did not come from a read
 * that worked; then the rows still show but without "En Stellar".
 */
@Composable
fun ProofCard(rows: List<ProofRow>, seal: Boolean) {
    val clipboard = LocalClipboardManager.current
    val uri = LocalUriHandler.current
    val type = LocalGpType.current
    GpCard(leftBar = GpColor.Teal, leftBarWidth = 3.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
            GpText("Lo que dice el contrato", type.cardTitle, Modifier.weight(1f))
            if (seal) StellarSeal(null)
        }
        rows.forEach { row ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GpSpace.s4)) {
                GpText(row.label, type.label)
                GpText(row.value, type.mono)
                GpText(row.proves, type.label.copy(fontWeight = FontWeight.Normal), color = GpColor.Navy64)
                Row(horizontalArrangement = Arrangement.spacedBy(GpSpace.s16)) {
                    GpLink(
                        "Copiar", { clipboard.setText(AnnotatedString(row.copyValue)) },
                        icon = GpIcons.Copy, description = "Copiar ${row.label}",
                    )
                    if (row.url != null) {
                        GpLink(
                            "Abrir", { uri.openUri(row.url) },
                            icon = GpIcons.ExternalLink, description = "Abrir ${row.label} en el explorador",
                        )
                    }
                }
            }
        }
    }
}
