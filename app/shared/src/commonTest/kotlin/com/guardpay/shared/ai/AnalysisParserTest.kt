package com.guardpay.shared.ai

import com.guardpay.shared.domain.RiskSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalysisParserTest {
    private val valid = """
        {"whatItAsks": "Pide transferir 150 USDC a una cuenta nueva hoy",
         "signals": ["Dice ser tu banco", "Pide urgencia"],
         "suggestion": "SUGGEST_HOLD"}
    """.trimIndent()

    @Test
    fun parsesValidJson() {
        val a = AnalysisParser.parse(valid)
        assertEquals("Pide transferir 150 USDC a una cuenta nueva hoy", a.whatItAsks)
        assertEquals(listOf(RiskSignal("Dice ser tu banco"), RiskSignal("Pide urgencia")), a.signals)
        assertEquals(Suggestion.SuggestHold, a.suggestion)
    }

    @Test
    fun parsesNoClearSignals() {
        val a = AnalysisParser.parse("""{"whatItAsks":"Saluda","signals":[],"suggestion":"NO_CLEAR_SIGNALS"}""")
        assertEquals(Suggestion.NoClearSignals, a.suggestion)
        assertEquals("Saluda", a.whatItAsks)
    }

    @Test
    fun toleratesTextAndFencesAroundTheObject() {
        val a = AnalysisParser.parse("Claro, aquí está:\n```json\n$valid\n```\nEspero que ayude.")
        assertEquals(Suggestion.SuggestHold, a.suggestion)
        assertEquals(2, a.signals.size)
    }

    @Test
    fun malformedInputIsUnavailable() {
        listOf(
            "",
            "no json at all",
            "{",
            "}{",
            "{not json}",
            """{"whatItAsks": "x", "signals": "Pide urgencia", "suggestion": "SUGGEST_HOLD"}""",
            """{"whatItAsks": "x", "signals": [], "suggestion": "SUGGEST_HOLD",}""",
            """{'whatItAsks': 'x', 'signals': [], 'suggestion': 'SUGGEST_HOLD'}""",
            "[1, 2, 3]",
            "{} trailing {",
        ).forEach { assertEquals(Analysis.Unavailable, AnalysisParser.parse(it), "input: $it") }
    }

    @Test
    fun truncatedInputIsUnavailable() {
        for (cut in 1 until valid.length - 1) {
            val a = AnalysisParser.parse(valid.substring(0, cut))
            assertEquals(Analysis.Unavailable, a, "cut at $cut")
        }
    }

    @Test
    fun missingFieldIsUnavailable() {
        assertEquals(Analysis.Unavailable, AnalysisParser.parse("""{"whatItAsks":"x","suggestion":"SUGGEST_HOLD"}"""))
    }

    @Test
    fun unknownKeysAreRejected() {
        // The model must never be able to hand the app a destination or amount.
        val a = AnalysisParser.parse(
            """{"whatItAsks":"x","signals":[],"suggestion":"SUGGEST_HOLD","destination":"GABC","amount":"150"}"""
        )
        assertEquals(Analysis.Unavailable, a)
    }

    @Test
    fun onlyTheTwoSuggestionValuesAreAccepted() {
        listOf("SAFE", "suggest_hold", "SCAM", "RELEASE", "").forEach {
            val a = AnalysisParser.parse("""{"whatItAsks":"x","signals":[],"suggestion":"$it"}""")
            assertEquals(Analysis.Unavailable, a, "suggestion: $it")
        }
    }

    @Test
    fun keepsAtMostThreeSignals() {
        val a = AnalysisParser.parse("""{"whatItAsks":"x","signals":["a","b","c","d","e"],"suggestion":"SUGGEST_HOLD"}""")
        assertEquals(listOf("a", "b", "c"), a.signals.map { it.text })
    }

    @Test
    fun dropsBlankAndOverlongSignals() {
        val long = "x".repeat(AnalysisParser.MAX_SIGNAL_CHARS + 1)
        val a = AnalysisParser.parse("""{"whatItAsks":"x","signals":["  ","$long","Pide urgencia"],"suggestion":"SUGGEST_HOLD"}""")
        assertEquals(listOf("Pide urgencia"), a.signals.map { it.text })
    }

    @Test
    fun overlongWhatItAsksIsUnavailable() {
        val long = "x".repeat(AnalysisParser.MAX_WHAT_IT_ASKS_CHARS + 1)
        assertEquals(Analysis.Unavailable, AnalysisParser.parse("""{"whatItAsks":"$long","signals":[],"suggestion":"SUGGEST_HOLD"}"""))
    }

    @Test
    fun forbiddenWordsNeverSurvive() {
        // Prompt-injection shape (GH-12): the model is talked into calling the payment safe.
        val a = AnalysisParser.parse(
            """{"whatItAsks":"Este pago es SEGURO","signals":["Cuenta protegida"],"suggestion":"NO_CLEAR_SIGNALS"}"""
        )
        assertEquals(Analysis.Unavailable, a)

        val b = AnalysisParser.parse(
            """{"whatItAsks":"Pide transferir hoy","signals":["Dice que es segura","Pide urgencia"],"suggestion":"SUGGEST_HOLD"}"""
        )
        assertEquals(listOf("Pide urgencia"), b.signals.map { it.text })
    }

    @Test
    fun unavailableIsDetectable() {
        assertTrue(Analysis.Unavailable.isUnavailable)
        assertEquals(Suggestion.NoClearSignals, Analysis.Unavailable.suggestion)
    }
}
