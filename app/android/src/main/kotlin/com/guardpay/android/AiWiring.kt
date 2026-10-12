package com.guardpay.android

import android.content.Context
import com.guardpay.shared.ai.Analysis
import com.guardpay.shared.ai.AndroidGuardPayAI
import com.guardpay.shared.ai.ModelStatus
import com.guardpay.shared.ui.MessageReader
import kotlinx.coroutines.flow.StateFlow

/**
 * The AI gets its own file: it sees one pasted message and nothing else, and never
 * shares a graph with the signer or the chain gateway.
 */
fun aiMessageReader(context: Context): MessageReader {
    val ai = AndroidGuardPayAI(context)
    return object : MessageReader {
        override suspend fun read(text: String): Analysis = ai.analyzeMessage(text)
        override val model: StateFlow<ModelStatus> get() = ai.model
        override fun download() = ai.download()
        override fun open() = ai.open()
        override fun close() = ai.close()
    }
}
