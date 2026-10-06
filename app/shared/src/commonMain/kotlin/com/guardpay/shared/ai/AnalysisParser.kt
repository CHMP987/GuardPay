package com.guardpay.shared.ai

import com.guardpay.shared.domain.RiskSignal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Strict parser for the model's JSON:
 * `{ "whatItAsks": "...", "signals": ["..."], "suggestion": "SUGGEST_HOLD" | "NO_CLEAR_SIGNALS" }`
 *
 * Anything else (malformed, truncated, unknown keys, unknown suggestion, over-long
 * text) becomes [Analysis.Unavailable]. Raw model text never reaches the screen.
 * Text around the object (e.g. a ```json fence) is tolerated: only the span from
 * the first `{` to the last `}` is parsed.
 */
object AnalysisParser {
    const val MAX_WHAT_IT_ASKS_CHARS = 200
    const val MAX_SIGNAL_CHARS = 120

    // UI strings must never say "seguro" or "protegido", whatever the model writes.
    private val forbiddenWords = Regex("""\b(segur[oa]s?|protegid[oa]s?)\b""", RegexOption.IGNORE_CASE)

    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    @Serializable
    private data class Dto(
        @SerialName("whatItAsks") val whatItAsks: String,
        @SerialName("signals") val signals: List<String>,
        @SerialName("suggestion") val suggestion: String,
    )

    fun parse(raw: String): Analysis {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return Analysis.Unavailable

        val dto = try {
            json.decodeFromString(Dto.serializer(), raw.substring(start, end + 1))
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException.
            return Analysis.Unavailable
        }

        val suggestion = when (dto.suggestion) {
            "SUGGEST_HOLD" -> Suggestion.SuggestHold
            "NO_CLEAR_SIGNALS" -> Suggestion.NoClearSignals
            else -> return Analysis.Unavailable
        }

        val whatItAsks = dto.whatItAsks.trim()
        if (whatItAsks.length > MAX_WHAT_IT_ASKS_CHARS || forbiddenWords.containsMatchIn(whatItAsks)) {
            return Analysis.Unavailable
        }

        val signals = dto.signals
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length <= MAX_SIGNAL_CHARS && !forbiddenWords.containsMatchIn(it) }
            .take(Analysis.MAX_SIGNALS)
            .map(::RiskSignal)

        return Analysis(whatItAsks, signals, suggestion)
    }
}
