package com.guardpay.android.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.guardpay.android.MainActivity
import com.guardpay.shared.ui.guardian.HoldAlert

/** P8: the guardian's local notifications. No push, no server: [HoldWatchService] posts them. */
object HoldNotifications {
    const val EXTRA_HOLD_ID = "com.guardpay.android.HOLD_ID"
    const val EXTRA_HOLD_LIST = "com.guardpay.android.HOLD_LIST"
    const val WATCHING_ID = 1

    private const val HOLDS = "holds"
    private const val WATCHING = "watching"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(HOLDS, "Pagos retenidos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Un pago de la persona que acompañas está esperando"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(WATCHING, "Revisión de pagos", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Aviso fijo mientras la app revisa Stellar cada 30 segundos"
            },
        )
    }

    /** The ongoing notice a foreground service must show. */
    fun watching(context: Context): Notification =
        Notification.Builder(context, WATCHING)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Revisando pagos retenidos")
            .setContentText("Lee Stellar cada 30 segundos")
            .setOngoing(true)
            .build()

    /** One held payment. Title and text come from the record the watcher just read. */
    fun post(context: Context, alert: HoldAlert) {
        val n = Notification.Builder(context, HOLDS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(alert.title)
            .setContentText(alert.text)
            .setStyle(Notification.BigTextStyle().bigText(alert.text))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(open(context, alert.holdId.toInt(), Intent().putExtra(EXTRA_HOLD_ID, alert.holdId)))
            .setGroup(GROUP)
            .setAutoCancel(true)
            .build()
        // Android bundles several alerts; tapping the bundle opens the guardian's list
        // instead of the app's first screen.
        val summary = Notification.Builder(context, HOLDS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Pagos retenidos")
            .setContentIntent(open(context, SUMMARY_REQUEST, Intent().putExtra(EXTRA_HOLD_LIST, true)))
            .setGroup(GROUP)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(TAG_HOLD, alert.holdId.toInt(), n)
        nm.notify(TAG_SUMMARY, 0, summary)
    }

    private fun open(context: Context, request: Int, extras: Intent): PendingIntent =
        PendingIntent.getActivity(
            context, request,
            Intent(context, MainActivity::class.java)
                .putExtras(extras)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private const val TAG_HOLD = "hold"
    private const val TAG_SUMMARY = "hold-summary"
    private const val GROUP = "com.guardpay.android.HOLDS"
    private const val SUMMARY_REQUEST = -1
}
