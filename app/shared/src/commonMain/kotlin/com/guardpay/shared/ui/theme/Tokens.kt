package com.guardpay.shared.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Palette of the visual proposal (section 5). Navy is for action, Teal only marks
 * evidence ("En Stellar") and is never a text or fill behind white text. Semantic
 * colors are fills and bars; text in a semantic color uses its derived tone, which
 * passes 4.5:1 on White and Ice (ContrastTest checks every pair).
 */
object GpColor {
    val Navy = Color(0xFF0B1220)
    val White = Color(0xFFFFFFFF)
    val Ice = Color(0xFFF5F9FA)
    val Teal = Color(0xFF00A99D)
    val Mint = Color(0xFF5EEAD4)
    val Warning = Color(0xFFD97706)
    val Success = Color(0xFF16A34A)
    val Danger = Color(0xFFDC2626)

    /** Teal for text: the "En Stellar" seal. */
    val TealText = Color(0xFF007E75)
    val WarningText = Color(0xFFA25904)
    val SuccessText = Color(0xFF107A37)

    /** Secondary text ("leído a las 13:05", what a proof row proves). */
    val Navy64 = Navy.copy(alpha = 0.64f)
    /** Input borders. */
    val Navy50 = Navy.copy(alpha = 0.50f)
    /** Disabled text and borders. */
    val Navy38 = Navy.copy(alpha = 0.38f)
    /** AI card left border. */
    val Navy20 = Navy.copy(alpha = 0.20f)
    /** Card borders and the wait-line rail. */
    val Navy10 = Navy.copy(alpha = 0.10f)
    /** Neutral chip fill. */
    val Navy8 = Navy.copy(alpha = 0.08f)
    /** Scrim behind a sheet. */
    val Scrim = Navy.copy(alpha = 0.40f)

    fun tint12(c: Color) = c.copy(alpha = 0.12f)
}

/** Spacing scale 4/8/12/16/24/32/48. */
object GpSpace {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s48 = 48.dp
}

object GpSize {
    val cardRadius = 12.dp
    val buttonRadius = 12.dp
    val inputRadius = 10.dp
    val sheetRadius = 20.dp
    val buttonHeight = 52.dp
    val inputHeight = 52.dp
    val touchTarget = 44.dp
    val icon = 20.dp
    /** Column width from 768 px; side panel from 1024 px. */
    val column = 480.dp
    val sidePanel = 360.dp
}

/** Composites [fg] (which may be translucent) over an opaque [bg]. */
fun Color.over(bg: Color): Color = Color(
    red = red * alpha + bg.red * (1 - alpha),
    green = green * alpha + bg.green * (1 - alpha),
    blue = blue * alpha + bg.blue * (1 - alpha),
    alpha = 1f,
)

/** WCAG 2.x contrast ratio. A translucent foreground is composited over [bg] first. */
fun contrastRatio(fg: Color, bg: Color): Double {
    val a = fg.over(bg).relativeLuminance()
    val b = bg.relativeLuminance()
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}

private fun Color.relativeLuminance(): Double {
    // Quantize to 8-bit first, as the proposal's table was computed on hex values.
    fun lin(c: Float): Double {
        val s = kotlin.math.round(c * 255) / 255.0
        return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * lin(red) + 0.7152 * lin(green) + 0.0722 * lin(blue)
}
