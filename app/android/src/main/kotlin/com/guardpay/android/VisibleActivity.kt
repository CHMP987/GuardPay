package com.guardpay.android

import android.app.Activity

/**
 * The activity on screen, for the system's signing prompt (it needs one to draw
 * on). Null in the background: then nothing can be signed, by design.
 */
object VisibleActivity {
    @Volatile
    var current: Activity? = null
}
