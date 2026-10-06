package com.guardpay.shared.ui

import androidx.compose.runtime.mutableStateListOf
import com.guardpay.shared.ai.Analysis

/** A payment the owner can open: a registry record, or something only this app saw. */
sealed interface PaymentKey {
    data class Hold(val id: Long) : PaymentKey

    /** A direct transfer, or an attempt the contract refused. Lives only in this session. */
    data class Local(val n: Long) : PaymentKey
}

/** The four screens of the proposal and their sub-views. No others. */
sealed interface Screen {
    data object Entry : Screen
    data object Home : Screen
    data object Pay : Screen
    data class Detail(val key: PaymentKey) : Screen
    data object GuardianList : Screen
    data class GuardianDetail(val holdId: Long) : Screen
}

/** A screen stack driven by the platform back button. */
class Navigator(start: Screen = Screen.Entry) {
    private val stack = mutableStateListOf(start)

    val current: Screen get() = stack.last()
    val depth: Int get() = stack.size

    fun go(screen: Screen) {
        stack.add(screen)
    }

    /** Replaces the top screen, e.g. Pagar becomes the payment's Detalle after signing. */
    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    /** Back to [root], dropping everything above it. */
    fun reset(root: Screen) {
        stack.clear()
        stack.add(root)
    }

    /** Back down to [root] if it is on the stack, else [reset] to it. */
    fun popTo(root: Screen) {
        val i = stack.lastIndexOf(root)
        if (i < 0) return reset(root)
        while (stack.lastIndex > i) stack.removeAt(stack.lastIndex)
    }

    /** False when there is nothing to go back to (the platform then closes the app). */
    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}

/**
 * What the owner's screens may ask of the AI: read one pasted message. The platform
 * wires it to `GuardPayAI.analyzeMessage` in a file that sees neither the signer nor
 * the chain, so the AI never shares a graph with them.
 */
fun interface MessageReader {
    suspend fun read(text: String): Analysis
}
