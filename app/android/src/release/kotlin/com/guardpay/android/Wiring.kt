package com.guardpay.android

import android.content.Context
import com.guardpay.shared.ui.MessageReader
import com.guardpay.shared.ui.Navigator
import com.guardpay.shared.ui.guardian.GuardianChain
import kotlinx.coroutines.CoroutineScope

/**
 * Release: no contract addresses are shipped yet (only debug reads a provisioned
 * `testnet.json`), so there is nothing to connect to. Entrada shows its buttons
 * disabled and says so, and nothing is watched in the background.
 */
object Wiring {
    class Watched(val account: String, val chain: GuardianChain)

    @Suppress("UNUSED_PARAMETER")
    fun sessions(context: Context, nav: Navigator, scope: CoroutineScope, reader: MessageReader): Sessions =
        Sessions(owner = null, guardian = null, simulation = false)

    @Suppress("UNUSED_PARAMETER")
    fun watchedChain(context: Context): Watched? = null
}
