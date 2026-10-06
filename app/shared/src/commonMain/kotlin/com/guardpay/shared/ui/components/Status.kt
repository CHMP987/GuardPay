package com.guardpay.shared.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.ui.model.ChipModel
import com.guardpay.shared.ui.model.ChipTone
import com.guardpay.shared.ui.model.clock
import com.guardpay.shared.ui.model.remaining
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.GpIcons
import com.guardpay.shared.ui.theme.GpSpace
import com.guardpay.shared.ui.theme.LocalGpType
import com.guardpay.shared.ui.theme.over
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlin.time.TimeMark

/** Pill: semantic color at 12 % + derived text + icon. Detenido is the only filled one. */
@Composable
fun StateChip(chip: ChipModel, modifier: Modifier = Modifier) {
    val (fill, fg, border) = when (chip.tone) {
        ChipTone.Warning -> Triple(GpColor.tint12(GpColor.Warning), GpColor.WarningText, null)
        ChipTone.Success -> Triple(GpColor.tint12(GpColor.Success), GpColor.SuccessText, null)
        ChipTone.DangerFilled -> Triple(GpColor.Danger, GpColor.White, null)
        ChipTone.DangerOutline -> Triple(GpColor.White, GpColor.Danger, GpColor.Danger)
        ChipTone.Neutral -> Triple(GpColor.Navy8, GpColor.Navy64, null)
    }
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .background(fill, shape)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .padding(horizontal = GpSpace.s12, vertical = GpSpace.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GpSpace.s4),
    ) {
        GpIcon(chip.icon, fg, 16.dp)
        GpText(chip.label, LocalGpType.current.label, color = fg)
    }
}

/** "En Stellar": TealText + external-link. Only for chain data whose last read worked. */
@Composable
fun StellarSeal(onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    if (onClick != null) {
        GpLink("En Stellar", onClick, modifier, color = GpColor.TealText, icon = GpIcons.ExternalLink)
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s4)) {
            GpText("En Stellar", LocalGpType.current.label, color = GpColor.TealText)
            GpIcon(GpIcons.ExternalLink, GpColor.TealText, 16.dp)
        }
    }
}

/** "leído a las 13:05", or "sin actualizar desde las 13:05" after a failed read. */
@Composable
fun ReadLine(readAt: LedgerTime?, lastReadOk: Boolean, tz: TimeZone) {
    val text = when {
        readAt == null -> "Todavía no se pudo leer"
        lastReadOk -> "leído a las ${readAt.clock(tz)}"
        else -> "sin actualizar desde las ${readAt.clock(tz)}"
    }
    Note(text)
}

/**
 * Ledger time extrapolated with a monotonic clock, for the wait-line fill and the
 * remaining time only. States are never derived from it: they use the last ledger read.
 */
@Composable
fun rememberDisplayNow(ledgerNow: LedgerTime?, readMark: TimeMark?): LedgerTime? {
    var now by remember(ledgerNow, readMark) { mutableStateOf(ledgerNow) }
    LaunchedEffect(ledgerNow, readMark) {
        if (ledgerNow == null || readMark == null) return@LaunchedEffect
        while (true) {
            now = LedgerTime(ledgerNow.epochSeconds + readMark.elapsedNow().inWholeSeconds)
            delay(1_000)
        }
    }
    return now
}

/**
 * Navy 10 % rail with an amber fill and three dots Retenido → Listo → Enviado, the
 * release time at the end, and the guardian with "puede detenerlo".
 */
@Composable
fun WaitLine(
    hold: HeldPayment,
    now: LedgerTime,
    tz: TimeZone,
    guardianLine: String?,
    reachedSent: Boolean = false,
) {
    val total = (hold.readyAt.epochSeconds - hold.createdAt.epochSeconds).toFloat()
    val done = ((now.epochSeconds - hold.createdAt.epochSeconds) / total).coerceIn(0f, 1f)
    val matured = now >= hold.readyAt
    val type = LocalGpType.current
    val spoken = if (matured) "Espera terminada a las ${hold.readyAt.clock(tz)}"
    else "Retenido hasta las ${hold.readyAt.clock(tz)}. ${remaining(now, hold.readyAt)}"
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(GpSpace.s8),
    ) {
        Canvas(Modifier.fillMaxWidth().height(16.dp)) {
            val y = size.height / 2
            val r = 6.dp.toPx()
            val h = 4.dp.toPx()
            val left = r
            val right = size.width - r
            val mid = (left + right) / 2
            drawRoundRect(GpColor.Navy10, Offset(left, y - h / 2), Size(right - left, h), CornerRadius(h / 2))
            // Hold fills the first half; the second half is the owner's release signature.
            val fillTo = if (reachedSent) right else left + (mid - left) * done
            drawRoundRect(GpColor.Warning, Offset(left, y - h / 2), Size(fillTo - left, h), CornerRadius(h / 2))
            fun dot(x: Float, on: Boolean, c: Color) {
                drawCircle(if (on) c else GpColor.White, r, Offset(x, y))
                if (!on) drawCircle(GpColor.Navy50, r, Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            }
            dot(left, true, GpColor.Warning)
            dot(mid, matured || reachedSent, GpColor.Warning)
            dot(right, reachedSent, GpColor.Success)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GpText("Retenido", type.label, color = GpColor.Navy64)
            GpText("Listo ${hold.readyAt.clock(tz)}", type.label, color = GpColor.Navy64)
            GpText("Enviado", type.label, color = GpColor.Navy64)
        }
        if (!matured && !reachedSent) {
            GpText(remaining(now, hold.readyAt), type.body.copy(fontWeight = FontWeight.Medium))
        }
        if (guardianLine != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
                Avatar()
                Body(guardianLine)
            }
        }
    }
}

/** A neutral person icon in a Navy 8 % circle: the app has no photos. */
@Composable
fun Avatar(tint: Color = GpColor.Navy, fill: Color = GpColor.Navy8) {
    Box(Modifier.size(32.dp).background(fill, CircleShape), contentAlignment = Alignment.Center) {
        GpIcon(GpIcons.User, tint, 18.dp)
    }
}

/** Persistent while the debug build runs on the in-memory simulation. */
@Composable
fun SimulationBanner() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(GpColor.tint12(GpColor.Warning).over(GpColor.White))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { contentDescription = "Simulación: nada de esto está en Stellar" },
    ) {
        GpText("Simulación: nada de esto está en Stellar", LocalGpType.current.label, color = GpColor.Navy)
    }
}

/** Fixed Navy band of the guardian mode, with Mint details. */
@Composable
fun GuardianBand(ownerName: String) {
    val type = LocalGpType.current
    Column(
        Modifier.fillMaxWidth().background(GpColor.Navy).padding(horizontal = LocalGpLayout.current.margin, vertical = GpSpace.s12),
        verticalArrangement = Arrangement.spacedBy(GpSpace.s4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
            Box(Modifier.size(8.dp).background(GpColor.Mint, CircleShape))
            GpText("Modo guardián · Proteges a $ownerName", type.cardTitle, color = GpColor.White)
        }
        GpText("Puedes detener sus pagos retenidos. No puedes mover su dinero.", type.label, color = GpColor.Mint)
    }
}

/** A labeled value: "Monto" / "150 USDC". */
@Composable
fun Field(label: String, value: String, mono: Boolean = false, spoken: String? = null) {
    val type = LocalGpType.current
    Column(
        Modifier.fillMaxWidth().then(if (spoken != null) Modifier.clearAndSetSemantics { contentDescription = "$label: $spoken" } else Modifier),
        verticalArrangement = Arrangement.spacedBy(GpSpace.s4),
    ) {
        GpText(label, type.label, color = GpColor.Navy64)
        GpText(value, if (mono) type.mono.copy(fontSize = type.body.fontSize, lineHeight = type.body.lineHeight) else type.body)
    }
}

/** A plain notice card, e.g. after an unknown submit outcome. */
@Composable
fun NoticeCard(text: String, bar: Color = GpColor.Warning) {
    GpCard(leftBar = bar) { Body(text) }
}
