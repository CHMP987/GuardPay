package com.guardpay.shared.ai

import com.guardpay.shared.domain.RiskSignal

/**
 * Reads a message the owner was sent and says what it asks for. That is all.
 *
 * It has no financial authority: no tool calling, no access to balances, contacts,
 * policy, config or keys, and it never drafts a payment or fills in the destination
 * or amount. Nothing in `ai/` may depend on, or share a class with, `Signer` or
 * `StellarGateway` (enforced by `checkCommonMainArchitecture`).
 *
 * This is a plain common interface rather than `expect`: the Android implementation
 * (LiteRT-LM, P7) needs a `Context` and a model path, which an `expect` declaration
 * cannot express. Each platform provides its own class in its `ai/` package.
 */
interface GuardPayAI {
    /** Never throws for bad model output: anything unusable comes back as [Analysis.Unavailable]. */
    suspend fun analyzeMessage(text: String): Analysis
}

/** Exactly two values. Do not add more. */
sealed interface Suggestion {
    data object SuggestHold : Suggestion
    data object NoClearSignals : Suggestion
}

data class Analysis(
    /** One line, e.g. "Pide transferir 150 USDC a una cuenta nueva hoy". Blank when unavailable. */
    val whatItAsks: String,
    /** At most [MAX_SIGNALS], shown as a neutral list. */
    val signals: List<RiskSignal>,
    val suggestion: Suggestion,
) {
    init {
        require(signals.size <= MAX_SIGNALS) { "at most $MAX_SIGNALS signals" }
    }

    /** True when there is nothing to show; the UI hides the analysis card. */
    val isUnavailable: Boolean get() = whatItAsks.isBlank() && signals.isEmpty()

    companion object {
        const val MAX_SIGNALS = 3

        /** What the stub returns and what every parse failure becomes. */
        val Unavailable = Analysis(whatItAsks = "", signals = emptyList(), suggestion = Suggestion.NoClearSignals)
    }
}
