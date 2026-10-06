package com.guardpay.shared.ai

/**
 * Android slot for the on-device model (P7, Ant). Until LiteRT-LM lands this is a
 * stub: it always answers [Analysis.Unavailable], so the UI hides the analysis card.
 *
 * P7 replaces the body (and may add constructor parameters such as a `Context` or a
 * model path). Rules that do not change: the model only ever receives [text], its
 * output goes through [AnalysisParser.parse], and this class never gets a `Signer`,
 * a `StellarGateway` or any account data.
 */
class AndroidGuardPayAI : GuardPayAI {
    override suspend fun analyzeMessage(text: String): Analysis = Analysis.Unavailable
}
