package com.atmacanext.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.automation.RuntimeStatus
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.QueueStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Visible host for user-started automation.
 *
 * Safety rule: screen-off or device-lock pauses the active queue/runtime. It never wakes or unlocks
 * the device and never auto-resumes after unlock. The user explicitly resumes from the app or
 * notification after verifying the screen is ready.
 */
class AutomationForegroundService : Service() {
    companion object {
        const val ACTION_START = "com.atmacanext.app.action.AUTOMATION_START"
        const val ACTION_PAUSE = "com.atmacanext.app.action.AUTOMATION_PAUSE"
        const val ACTION_RESUME = "com.atmacanext.app.action.AUTOMATION_RESUME"
        const val ACTION_STOP = "com.atmacanext.app.action.AUTOMATION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, AutomationForegroundService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutomationForegroundService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var seenActive = false
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> pauseForDevice("Ekran kapandı; güvenli devam için görev duraklatıldı")
                Intent.ACTION_SCREEN_ON -> updateNotification("Ekran açıldı; görev duraklatıldıysa doğrulayıp Devam'a bas")
                Intent.ACTION_USER_PRESENT -> updateNotification("Cihaz açıldı; otomatik devam yok, güvenli şekilde Devam'a basabilirsin")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppServices.initialize(applicationContext)
        AutomationNotification.ensureChannel(this)
        val foregroundType = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(
            this,
            AutomationNotification.SERVICE_NOTIFICATION_ID,
            AutomationNotification.build(this, "Atmaca Next çalışıyor", "Otomasyon motoru hazırlanıyor"),
            foregroundType,
        )
        registerScreenReceiver()
        observeRuntime()
        scope.launch {
            delay(15_000L)
            if (!seenActive) stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_PAUSE -> pauseForDevice("Bildirimden duraklatıldı")
            ACTION_RESUME -> resumeSafely()
            ACTION_STOP -> stopAutomation()
            else -> updateNotification()
        }
        return START_NOT_STICKY
    }

    private fun observeRuntime() {
        observerJob?.cancel()
        observerJob = scope.launch {
            combine(AppServices.orchestrator.state, AutomationController.state) { queue, runtime -> queue to runtime }
                .collect { (queue, runtime) ->
                    val active = queue.isActive || (runtime.taskId != null && runtime.status !in terminalRuntimeStatuses())
                    if (active) seenActive = true
                    updateNotification()
                    if (seenActive && !active && queue.status in setOf(QueueStatus.IDLE, QueueStatus.COMPLETED, QueueStatus.PARTIAL, QueueStatus.FAILED, QueueStatus.STOPPED)) {
                        delay(1_000L)
                        stopSelf()
                    }
                }
        }
    }

    private fun pauseForDevice(reason: String) {
        val queue = AppServices.orchestrator.state.value
        if (queue.isActive && queue.status != QueueStatus.PAUSED) {
            AppServices.orchestrator.pauseForSafety(reason)
        } else {
            val runtime = AutomationController.state.value
            if (runtime.taskId != null && runtime.status !in terminalRuntimeStatuses() && runtime.status != RuntimeStatus.PAUSED) {
                AutomationController.pause(reason)
            }
        }
        updateNotification(reason)
    }

    private fun resumeSafely() {
        val readiness = com.atmacanext.app.scheduling.DeviceReadinessChecker.read(this)
        if (!readiness.readyForUiAutomation) {
            updateNotification("Devam ettirilemedi: ${readiness.blockingReason}")
            return
        }
        val queue = AppServices.orchestrator.state.value
        if (queue.status == QueueStatus.PAUSED) AppServices.orchestrator.resume()
        else if (AutomationController.state.value.status == RuntimeStatus.PAUSED) AutomationController.resume()
        updateNotification("Güvenli yeniden doğrulama başlatıldı")
    }

    private fun stopAutomation() {
        val queue = AppServices.orchestrator.state.value
        if (queue.isActive) AppServices.orchestrator.stop() else AutomationController.stop()
        stopSelf()
    }

    private fun updateNotification(overrideText: String? = null) {
        val manager = getSystemService(android.app.NotificationManager::class.java) ?: return
        manager.notify(
            AutomationNotification.SERVICE_NOTIFICATION_ID,
            AutomationNotification.build(
                this,
                "Atmaca Next otomasyon",
                overrideText ?: AutomationNotification.liveText(),
            ),
        )
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION") registerReceiver(screenReceiver, filter)
        }
        receiverRegistered = true
    }

    override fun onDestroy() {
        observerJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        if (receiverRegistered) runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun terminalRuntimeStatuses() = setOf(RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED)
}
