package com.atmacanext.app.automation

import android.content.Context
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.notifications.AccountNotification

/** In-app inbox only. Phone banners are not posted; the Bildirimler page is the record. */
internal object TaskAlerts {
    private var app: Context? = null
    private var lastKey = ""
    private var lastAt = 0L

    fun attach(context: Context) {
        app = context.applicationContext
    }

    fun onLog(level: String, category: String, message: String) {
        val alert = TaskAlertPolicy.fromLog(level, category, message) ?: return
        val account = AutomationController.state.value.username?.removePrefix("@").orEmpty().ifBlank { "Atmaca" }
        show(alert, account)
    }

    fun show(alert: TaskAlertPolicy.Alert, username: String = "Atmaca") {
        val now = System.currentTimeMillis()
        if (alert.dedupeKey == lastKey && now - lastAt < 60_000L) return
        lastKey = alert.dedupeKey
        lastAt = now
        if (app == null || !AppServices.ready) return
        runCatching {
            AppServices.notifications.add(
                AccountNotification(
                    id = "${alert.dedupeKey}:${now / 60_000L}",
                    username = username.ifBlank { "Atmaca" },
                    message = alert.text,
                    createdAt = now,
                ),
            )
        }
    }
}
