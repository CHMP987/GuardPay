package com.guardpay.android

import com.guardpay.shared.ui.MessageReader
import com.guardpay.shared.ui.Navigator
import kotlinx.coroutines.CoroutineScope

/**
 * Release: there is no INTERFACES.md and there are no testnet contract addresses
 * yet (Sync 2), so there is nothing to connect to. Entrada shows its buttons
 * disabled and says so. When the addresses land, this builds sessions backed by
 * KmpStellarGateway.
 */
object Wiring {
    @Suppress("UNUSED_PARAMETER")
    fun sessions(nav: Navigator, scope: CoroutineScope, reader: MessageReader): Sessions =
        Sessions(owner = null, guardian = null, simulation = false)
}
