package com.guardpay.aibench

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Same acceptance rule as app/shared AnalysisParser (copied: the spike stays outside
 * the MVP build). Returns the suggestion, or null when the MVP would show nothing.
 */
object Parser {
    private val forbiddenWords = Regex("""\b(segur[oa]s?|protegid[oa]s?)\b""", RegexOption.IGNORE_CASE)
    private val json = Json { ignoreUnknownKeys = false; isLenient = false }

    @Serializable
    private data class Dto(
        @SerialName("whatItAsks") val whatItAsks: String,
        @SerialName("signals") val signals: List<String>,
        @SerialName("suggestion") val suggestion: String,
    )

    fun parse(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val dto = try {
            json.decodeFromString(Dto.serializer(), raw.substring(start, end + 1))
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (dto.suggestion != "SUGGEST_HOLD" && dto.suggestion != "NO_CLEAR_SIGNALS") return null
        val what = dto.whatItAsks.trim()
        if (what.length > 200 || forbiddenWords.containsMatchIn(what)) return null
        return dto.suggestion
    }
}
