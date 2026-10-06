package com.guardpay.shared.ui

import androidx.compose.runtime.Composable
import com.guardpay.shared.ui.components.GpTheme
import com.guardpay.shared.ui.entry.EntryScreen
import com.guardpay.shared.ui.guardian.GuardianDetailScreen
import com.guardpay.shared.ui.guardian.GuardianListScreen
import com.guardpay.shared.ui.guardian.GuardianSession
import com.guardpay.shared.ui.owner.DetailScreen
import com.guardpay.shared.ui.owner.HomeScreen
import com.guardpay.shared.ui.owner.OwnerSession
import com.guardpay.shared.ui.owner.PayScreen

/**
 * The app root. The platform builds the sessions (or passes null when this build
 * has nothing to connect to) and forwards the system back button to [Navigator.back].
 * Entrada stays under both sides, so back from Inicio or the guardian list returns to it.
 */
@Composable
fun GuardPayApp(nav: Navigator, owner: OwnerSession?, guardian: GuardianSession?, simulation: Boolean) {
    GpTheme {
        when (val screen = nav.current) {
            Screen.Entry -> EntryScreen(
                simulation,
                onOwner = owner?.let { o -> { o.start(); nav.go(Screen.Home) } },
                onGuardian = guardian?.let { g -> { g.start(); nav.go(Screen.GuardianList) } },
            )
            Screen.Home -> owner?.let { HomeScreen(it) { key -> nav.go(Screen.Detail(key)) } }
            Screen.Pay -> owner?.let { PayScreen(it) { nav.back() } }
            is Screen.Detail -> owner?.let { DetailScreen(it, screen.key) { nav.back() } }
            Screen.GuardianList -> guardian?.let { GuardianListScreen(it) { id -> nav.go(Screen.GuardianDetail(id)) } }
            is Screen.GuardianDetail -> guardian?.let { GuardianDetailScreen(it, screen.holdId) { nav.back() } }
        }
    }
}

/**
 * The system back button: an open sheet closes first, then the screen pops. False
 * when there was nothing to undo, so the platform closes the app.
 */
fun handleBack(nav: Navigator, owner: OwnerSession?, guardian: GuardianSession?): Boolean {
    val screen = nav.current
    val ownerScreen = screen == Screen.Pay || screen is Screen.Detail
    if (ownerScreen && owner?.closeSheet() == true) return true
    if (screen is Screen.GuardianDetail && guardian?.closeSheet() == true) return true
    return nav.back()
}
