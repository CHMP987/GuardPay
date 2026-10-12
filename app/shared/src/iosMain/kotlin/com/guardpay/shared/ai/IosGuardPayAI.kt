package com.guardpay.shared.ai

/**
 * iOS has no on-device model in this MVP.
 * Always [Analysis.Unavailable]; the UI says the analysis is not available here.
 */
class IosGuardPayAI : GuardPayAI {
    override suspend fun analyzeMessage(text: String): Analysis = Analysis.Unavailable
}
