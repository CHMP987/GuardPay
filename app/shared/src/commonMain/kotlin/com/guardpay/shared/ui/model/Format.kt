package com.guardpay.shared.ui.model

import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.UsdcAmount
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** "1.250,5" (Spanish separators). Shows every significant decimal, never rounds. */
fun UsdcAmount.display(): String {
    val whole = units / UsdcAmount.UNITS_PER_USDC
    val frac = (units % UsdcAmount.UNITS_PER_USDC).toString().padStart(7, '0').trimEnd('0')
    val grouped = whole.toString().reversed().chunked(3).joinToString(".").reversed()
    return if (frac.isEmpty()) grouped else "$grouped,$frac"
}

fun UsdcAmount.usdc(): String = "${display()} USDC"

/**
 * Parses what the owner typed. Accepts "150", "150,5" or "150.5", up to 7 decimals
 * (USDC on Stellar). Null for anything else, for zero and for overflow: the form
 * shows no lane until the amount is valid.
 */
fun parseUsdc(text: String): UsdcAmount? {
    val t = text.trim()
    val m = Regex("""^(\d{1,12})(?:[.,](\d{1,7}))?$""").matchEntire(t) ?: return null
    val whole = m.groupValues[1].toLong()
    val frac = m.groupValues[2].padEnd(7, '0').ifEmpty { "0" }.toLong()
    val units = whole * UsdcAmount.UNITS_PER_USDC + frac
    return if (units > 0) UsdcAmount(units) else null
}

/** "14:32" in the phone's zone. The instant itself is the ledger's, never the phone's. */
@OptIn(ExperimentalTime::class)
fun LedgerTime.clock(tz: TimeZone): String {
    val t = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(tz)
    return "${t.hour.pad()}:${t.minute.pad()}"
}

/** "2026-10-05 14:32:10", for the proof rows. */
@OptIn(ExperimentalTime::class)
fun LedgerTime.stamp(tz: TimeZone): String {
    val t = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(tz)
    return "${t.year}-${t.month.ordinal.plus(1).pad()}-${t.day.pad()} ${t.hour.pad()}:${t.minute.pad()}:${t.second.pad()}"
}

/** "Faltan 1 h 05 min", "Faltan 12 min", "Falta menos de 1 min". */
fun remaining(from: LedgerTime, to: LedgerTime): String {
    val s = to.epochSeconds - from.epochSeconds
    if (s < 60) return "Falta menos de 1 min"
    val h = s / 3600
    val m = (s % 3600) / 60
    return if (h > 0) "Faltan $h h ${m.toInt().pad()} min" else "Faltan $m min"
}

/** "termina en C91E": what a screen reader hears instead of 56 characters. */
fun spokenAddress(value: String) = "dirección que termina en ${value.takeLast(4)}"

private fun Int.pad() = toString().padStart(2, '0')
