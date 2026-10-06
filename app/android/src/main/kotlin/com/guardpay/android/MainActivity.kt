package com.guardpay.android

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardpay.android.notifications.HoldNotifications
import com.guardpay.android.notifications.HoldWatchService
import com.guardpay.shared.ui.GuardPayApp
import com.guardpay.shared.ui.Navigator
import com.guardpay.shared.ui.Screen
import com.guardpay.shared.ui.handleBack
import com.guardpay.shared.ui.theme.GpColor

/** Keeps the navigator and both sessions across rotation; their coroutines die with it. */
class AppModel(app: Application) : AndroidViewModel(app) {
    val nav = Navigator()
    val sessions: Sessions = Wiring.sessions(app, nav, viewModelScope, aiMessageReader())
}

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val s = model.sessions
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!handleBack(model.nav, s.owner, s.guardian)) finish()
            }
        })
        // Android 15 draws edge to edge: keep the screens clear of the system bars and,
        // on Pagar, of the keyboard (its bottom button would sit under it).
        setContent {
            Box(Modifier.fillMaxSize().background(GpColor.Ice).safeDrawingPadding()) {
                GuardPayApp(model.nav, s.owner, s.guardian, s.simulation)
            }
        }
        if (s.watchable) startWatching()
        openHold(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openHold(intent)
    }

    override fun onResume() {
        super.onResume()
        VisibleActivity.current = this
    }

    override fun onPause() {
        if (VisibleActivity.current === this) VisibleActivity.current = null
        super.onPause()
    }

    private fun startWatching() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        HoldWatchService.start(this)
    }

    /**
     * A tapped notification opens the guardian's Detalle for that hold (the bundle
     * of several opens their list). The
     * notification only carries the id: Detalle shows what it reads from the chain
     * now ([com.guardpay.shared.ui.guardian.GuardianSession.start] reads again).
     */
    private fun openHold(intent: Intent?) {
        val id = intent?.getLongExtra(HoldNotifications.EXTRA_HOLD_ID, -1L) ?: -1L
        val list = intent?.getBooleanExtra(HoldNotifications.EXTRA_HOLD_LIST, false) == true
        if (id < 0 && !list) return
        intent?.removeExtra(HoldNotifications.EXTRA_HOLD_ID)
        intent?.removeExtra(HoldNotifications.EXTRA_HOLD_LIST)
        val g = model.sessions.guardian ?: return
        g.start()
        model.nav.reset(Screen.Entry)
        model.nav.go(Screen.GuardianList)
        if (id >= 0) model.nav.go(Screen.GuardianDetail(id))
    }
}
