package com.guardpay.shared.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.window.ComposeUIViewController
import com.guardpay.shared.simulation.simulatedSides
import com.guardpay.shared.ui.GuardPayApp
import com.guardpay.shared.ui.Navigator
import com.guardpay.shared.ui.Screen
import com.guardpay.shared.ui.handleBack
import com.guardpay.shared.ui.theme.GpColor
import kotlinx.coroutines.MainScope
import platform.Foundation.NSLog
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi

/**
 * The iOS app's root, called from Swift (`iosApp/`). iOS-A: the in-memory simulation
 * only, under the "Simulación" banner. There is no signer on iOS yet, so nothing
 * here reaches Stellar. iOS has no back button: the edge swipe goes back instead.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalNativeApi::class)
fun MainViewController(): UIViewController {
    // Kotlin's uncaught exceptions go to stderr, which the simulator log does not keep.
    // Log them before the app terminates, as it still does.
    setUnhandledExceptionHook { NSLog("GuardPay uncaught: %@", it.stackTraceToString()) }
    val nav = Navigator()
    val sides = simulatedSides(nav, MainScope(), aiMessageReader())
    return ComposeUIViewController {
        BackHandler(enabled = nav.current != Screen.Entry) {
            handleBack(nav, sides.owner, sides.guardian)
        }
        Box(Modifier.fillMaxSize().background(GpColor.Ice).safeDrawingPadding()) {
            GuardPayApp(nav, sides.owner, sides.guardian, simulation = true)
        }
    }
}
