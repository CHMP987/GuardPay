package com.guardpay.android.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.guardpay.android.Wiring
import com.guardpay.shared.ui.guardian.HoldWatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * P8 on Android: a foreground service that reads the registry every 30 s and posts
 * a local notification for each new held payment. There is no backend: if the
 * system stops this service (battery saver, the dataSync time limit on Android 15),
 * the alert arrives late, when the app is next opened or "Actualizar" is tapped.
 *
 * Alerted ids are kept per account in SharedPreferences, so a restart does not
 * repeat them. Logs carry hold ids and delays only: no keys, no addresses.
 */
class HoldWatchService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val watched = Wiring.watchedChain(applicationContext)
        if (watched == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        HoldNotifications.ensureChannels(this)
        val notice = HoldNotifications.watching(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(HoldNotifications.WATCHING_ID, notice, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(HoldNotifications.WATCHING_ID, notice)
        }
        if (loop?.isActive != true) loop = scope.launch { watch(watched) }
        return START_STICKY
    }

    private suspend fun watch(watched: Wiring.Watched) {
        val prefs = getSharedPreferences("hold_watch", MODE_PRIVATE)
        val key = "alerted:${watched.account}"
        val seen = prefs.getStringSet(key, emptySet())!!.mapNotNull(String::toLongOrNull).toSet()
        val watcher = HoldWatcher(watched.chain, seen)
        while (scope.isActive) {
            try {
                for (alert in watcher.check()) {
                    HoldNotifications.post(this, alert)
                    val late = System.currentTimeMillis() / 1000 - alert.createdAt.epochSeconds
                    Log.i(TAG, "hold ${alert.holdId} notified ${late}s after the ledger recorded it")
                }
                prefs.edit().putStringSet(key, watcher.alertedIds.map(Long::toString).toSet()).apply()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never "nothing new" on a failed read: try again on the next tick.
                Log.w(TAG, "registry read failed: ${e::class.simpleName}")
            }
            delay(INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "GuardPayWatch"
        const val INTERVAL_MS = 30_000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, HoldWatchService::class.java))
        }
    }
}
