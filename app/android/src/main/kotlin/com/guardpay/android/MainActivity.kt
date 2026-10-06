package com.guardpay.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardpay.shared.ui.GuardPayApp
import com.guardpay.shared.ui.Navigator
import com.guardpay.shared.ui.handleBack

/** Keeps the navigator and both sessions across rotation; their coroutines die with it. */
class AppModel : ViewModel() {
    val nav = Navigator()
    val sessions: Sessions = Wiring.sessions(nav, viewModelScope, aiMessageReader())
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
        setContent { GuardPayApp(model.nav, s.owner, s.guardian, s.simulation) }
    }
}
