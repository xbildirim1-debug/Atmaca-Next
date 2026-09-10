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
import com.atmacanext.app.automation.AutomationRuntimeState
import com.atmacanext.app.automation.AutomationStallPolicy
import com.atmacanext.app.automation.AutomationWatchdog
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
    private var stallJob: Job? = null
    private var seenActive = false
    private var receiverRegistered = false

    private val stallWatchdog = AutomationWatchdog(AutomationStallPolicy.TIMEOUT_MS)
    private var watchedTaskId: String? = null
    private var watchedSessionId: String? = null
    private var stallRecoveryInProgress = false

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
        startStallWatchdog()
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

    /**
     * Unattended self-healing watchdog. It watches semantic progress, not event volume.
     * A task that sits on the exact same meaningful state for 20 seconds is paused,
     * checkpointed, brought back to Atmaca, stopped, then relaunched from its saved progress.
     * User-configured cycle waits and rate-limit cooldowns are intentionally excluded.
     */
    private fun startStallWatchdog() {
        stallJob?.cancel()
        stallJob = scope.launch {
            while (true) {
                delay(1_000L)
                if (stallRecoveryInProgress) continue

                val queue = AppServices.orchestrator.state.value
                val runtime = AutomationController.state.value
                val sameRunningItem = queue.status == QueueStatus.RUNNING &&
                    queue.currentItem?.taskId == runtime.taskId
                if (!sameRunningItem || !AutomationStallPolicy.shouldWatch(runtime)) {
                    resetStallTracking()
                    continue
                }

                val now = System.currentTimeMillis()
                if (watchedTaskId != runtime.taskId || watchedSessionId != runtime.sessionId) {
                    watchedTaskId = runtime.taskId
                    watchedSessionId = runtime.sessionId
                    stallWatchdog.reset(now)
                }
                stallWatchdog.observe(AutomationStallPolicy.signature(runtime), now)
                if (stallWatchdog.isStuck(now)) recoverStalledTask(runtime)
            }
        }
    }

    private suspend fun recoverStalledTask(runtime: AutomationRuntimeState) {
        if (stallRecoveryInProgress) return
        val queue = AppServices.orchestrator.state.value
        val item = queue.currentItem ?: return
        if (queue.status != QueueStatus.RUNNING || item.taskId != runtime.taskId) return

        stallRecoveryInProgress = true
        val ageMs = stallWatchdog.ageMillis()
        val reason = "20 saniye gerçek ilerleme yok; görev otomatik yeniden başlatılıyor"
        try {
            // Persist the latest verified counter before invalidating the runtime session.
            AppServices.repository.persistRuntime(runtime.copy(status = RuntimeStatus.PAUSED, message = reason))
            AppServices.repository.log(
                "WARN",
                "STALL_RECOVERY",
                item.taskId,
                item.username,
                reason,
                "ageMs=$ageMs; stage=${runtime.flowStage}; screen=${runtime.activeScreen}; verified=${runtime.verifiedCount}/${runtime.limit}",
            )
            updateNotification(reason)

            // Pause the queue first so resume() is guaranteed to relaunch the same queue item.
            AppServices.orchestrator.pauseForSafety(reason)
            var pauseChecks = 0
            while (AppServices.orchestrator.state.value.status != QueueStatus.PAUSED && pauseChecks < 20) {
                delay(100L)
                pauseChecks++
            }
            if (AppServices.orchestrator.state.value.status != QueueStatus.PAUSED) {
                AppServices.repository.log(
                    "ERROR",
                    "STALL_RECOVERY",
                    item.taskId,
                    item.username,
                    "Watchdog kuyruğu güvenli PAUSED durumuna alamadı; runtime durdurulmadı",
                )
                return
            }

            AutomationController.returnToAtmaca()
            delay(700L)
            AutomationController.stop()
            delay(500L)
            updateNotification("Takılan görev aynı ilerlemeden yeniden başlatılıyor")
            AppServices.orchestrator.resume()
        } finally {
            resetStallTracking()
            stallRecoveryInProgress = false
        }
    }

    private fun resetStallTracking() {
        watchedTaskId = null
        watchedSessionId = null
        stallWatchdog.reset(System.currentTimeMillis())
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
        stallJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        if (receiverRegistered) runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun terminalRuntimeStatuses() = setOf(RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED)
}
