package com.guardpay.android

import com.guardpay.shared.ui.guardian.GuardianSession
import com.guardpay.shared.ui.owner.OwnerSession

/**
 * What `Wiring` hands the activity. Each build type has its own `Wiring`: debug
 * talks to testnet when `testnet.json` is on the phone and otherwise runs an
 * in-memory simulation; release has no contract addresses yet and returns null
 * sessions, so Entrada says it is not connected to Stellar.
 *
 * [watchable]: the guardian's registry can be watched in the background (P8).
 */
class Sessions(
    val owner: OwnerSession?,
    val guardian: GuardianSession?,
    val simulation: Boolean,
    val watchable: Boolean = false,
)
