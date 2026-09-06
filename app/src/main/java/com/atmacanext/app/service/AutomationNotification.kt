package com.atmacanext.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.atmacanext.app.MainActivity
import com.atmacanext.app.R
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.core.AppServices

object AutomationNotification {
    const val CHANNEL_ID = "atmaca_automation"
    const val SERVICE_NOTIFICATION_ID = 1208
    const val WORKER_NOTIFICATION_ID = 1209

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Atmaca otomasyon",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Aktif ve zamanlanmış Atmaca görevlerinin görünür çalışma bildirimi"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, title: String, text: String, includeActions: Boolean = true): Notification {
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_atmaca)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (includeActions) {
            builder.addAction(
                0,
                "Duraklat",
                serviceAction(context, AutomationForegroundService.ACTION_PAUSE, 20),
            )
            builder.addAction(
                0,
                "Devam",
                serviceAction(context, AutomationForegroundService.ACTION_RESUME, 21),
            )
            builder.addAction(
                0,
                "Durdur",
                serviceAction(context, AutomationForegroundService.ACTION_STOP, 22),
            )
        }
        return builder.build()
    }

    private fun serviceAction(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            Intent(context, AutomationForegroundService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun liveText(): String {
        val queue = runCatching { AppServices.orchestrator.state.value }.getOrNull()
        val runtime = AutomationController.state.value
        return when {
            queue != null && queue.isActive -> "${queue.message} • ${queue.completedCount}/${queue.items.size} tamamlandı"
            runtime.taskId != null -> "${runtime.username.orEmpty()} • ${runtime.message} • ${runtime.verifiedCount}/${runtime.limit}"
            else -> "Otomasyon servisi hazır"
        }
    }
}
