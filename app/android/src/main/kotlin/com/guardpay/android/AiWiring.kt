package com.guardpay.android

import com.guardpay.shared.ai.AndroidGuardPayAI
import com.guardpay.shared.ui.MessageReader

/**
 * The AI gets its own file: it sees one pasted message and nothing else, and never
 * shares a graph with the signer or the chain gateway.
 */
fun aiMessageReader(): MessageReader {
    val ai = AndroidGuardPayAI()
    return MessageReader { text -> ai.analyzeMessage(text) }
}
