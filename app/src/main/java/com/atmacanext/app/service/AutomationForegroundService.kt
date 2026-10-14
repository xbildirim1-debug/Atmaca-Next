package com.atmacanext.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.automation.AutomationRuntimeState
import com.atmacanext.app.automation.AutomationStallPolicy
import com.atmacanext.app.automation.AutomationWatchdog
import com.atmacanext.app.automation.InternetWatchPolicy
import com.atmacanext.app.automation.NetworkStatus
import com.atmacanext.app.automation.RuntimeStatus
import com.atmacanext.app.automation.ScreenStayPolicy
import com.atmacanext.app.automation.TaskAlertPolicy
import com.atmacanext.app.automation.TaskAlerts
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
        const val ACTION_HOLD = "com.atmacanext.app.action.ACCESSIBILITY_HOLD"
        const val ACTION_RELEASE = "com.atmacanext.app.action.ACCESSIBILITY_RELEASE"

        @Volatile var accessibilityHeld = false

        fun hold(context: Context) {
            accessibilityHeld = true
            val intent = Intent(context, AutomationForegroundService::class.java).setAction(ACTION_HOLD)
            ContextCompat.startForegroundService(context, intent)
        }

        fun release(context: Context) {
            accessibilityHeld = false
            runCatching {
                context.startService(Intent(context, AutomationForegroundService::class.java).setAction(ACTION_RELEASE))
            }
        }

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
    private var selfPauseSince = 0L
    private var watchedTaskId: String? = null
    private var watchedSessionId: String? = null
    private var stallRecoveryInProgress = false
    private var internetJob: Job? = null
    private var wasOnline = true
    private var pausedForInternet = false
    private var blockedNoticeAt = 0L
    private var resumeAttemptAt = 0L
    private var reportedResumeProblem = false
    private var userPaused = false
    private var screenLock: PowerManager.WakeLock? = null

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
        startInternetWatch()
        scope.launch {
            delay(15_000L)
            if (!seenActive && !accessibilityHeld) stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_HOLD -> {
                accessibilityHeld = true
                updateNotification()
            }
            ACTION_RELEASE -> {
                accessibilityHeld = false
                if (!isTaskRunning()) stopSelf()
            }
            ACTION_PAUSE -> pauseForDevice("Bildirimden duraklatıldı")
            ACTION_RESUME -> resumeSafely()
            ACTION_STOP -> stopAutomation()
            else -> updateNotification()
        }
        return if (accessibilityHeld) START_STICKY else START_NOT_STICKY
    }

    private fun observeRuntime() {
        observerJob?.cancel()
        observerJob = scope.launch {
            combine(AppServices.orchestrator.state, AutomationController.state) { queue, runtime -> queue to runtime }
                .collect { (queue, runtime) ->
                    val active = queue.isActive || (runtime.taskId != null && runtime.status !in terminalRuntimeStatuses())
                    if (active) seenActive = true
                    if (ScreenStayPolicy.hold(active, userPaused)) holdScreen() else releaseScreen()
                    updateNotification()
                    if (seenActive && !active && !accessibilityHeld && queue.status in setOf(QueueStatus.IDLE, QueueStatus.COMPLETED, QueueStatus.PARTIAL, QueueStatus.FAILED, QueueStatus.STOPPED)) {
                        delay(1_000L)
                        if (!accessibilityHeld) stopSelf()
                    }
                }
        }
    }

    /**
     * Unattended self-healing watchdog. It watches semantic progress, not event volume.
     * After ten seconds without meaningful progress, navigation returns through Atmaca
     * and restarts the same item. An outstanding action is only re-read, never reissued.
     * User-configured cycle waits and rate-limit cooldowns are intentionally excluded.
     */
    private fun startStallWatchdog() {
        stallJob?.cancel()
        stallJob = scope.launch {
            while (true) {
                delay(1_000L)
                if (!stallRecoveryInProgress) pilotContinuePaused()
                if (stallRecoveryInProgress) continue

                val queue = AppServices.orchestrator.state.value
                val runtime = AutomationController.state.value
                val sameRunningItem = queue.status == QueueStatus.RUNNING &&
                    queue.currentItem?.taskId == runtime.taskId
                if (!sameRunningItem || !AutomationStallPolicy.shouldWatch(runtime) || pausedForInternet) {
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
        try {
            val reason = "Pilot takılmayı gördü; aynı ilerlemeden devam ediyor"
            AppServices.repository.log("WARN", "STALL_RECOVERY", item.taskId, item.username, reason,
                "ageMs=${stallWatchdog.ageMillis()}; stage=${runtime.flowStage}; screen=${runtime.activeScreen}; verified=${runtime.verifiedCount}/${runtime.limit}")
            updateNotification(reason)
            // The accessibility actor validates the same session and preserves pending actions.
            // Navigation stalls return through Atmaca and verify the account again.
            // Pending actions remain in result verification and cannot be issued twice.
            if (!AutomationController.requestStallRecovery(runtime.taskId, runtime.sessionId, AutomationStallPolicy.signature(runtime))) {
                AppServices.orchestrator.pauseForSafety("Ekran okuma bağlantısı olmadan kurtarma yapılamadı; ilerleme korundu")
            }
        } finally {
            resetStallTracking()
            stallRecoveryInProgress = false
        }
    }

    private fun pilotContinuePaused() {
        if (userPaused || pausedForInternet) {
            selfPauseSince = 0L
            return
        }
        val queue = AppServices.orchestrator.state.value
        val runtime = AutomationController.state.value
        val paused = queue.status == QueueStatus.PAUSED || runtime.status == RuntimeStatus.PAUSED
        if (!paused) {
            selfPauseSince = 0L
            return
        }
        val now = System.currentTimeMillis()
        if (selfPauseSince == 0L) selfPauseSince = now
        if (now - selfPauseSince < 2_000L) return
        val message = runtime.message.ifBlank { queue.message }
        if (!com.atmacanext.app.ai.PilotDuty.shouldAutoResume(message)) return
        val readiness = com.atmacanext.app.scheduling.DeviceReadinessChecker.read(this)
        if (!readiness.readyForUiAutomation) return
        if (com.atmacanext.app.automation.AtmacaAccessibilityService.peek() == null &&
            !com.atmacanext.app.automation.AccessibilityServiceState.health.value.connected
        ) return
        selfPauseSince = 0L
        if (queue.status == QueueStatus.PAUSED) AppServices.orchestrator.resume()
        else AutomationController.resume()
        updateNotification("Pilot duraklamayı açtı ve göreve devam ediyor")
    }

    private fun resetStallTracking() {
        watchedTaskId = null
        watchedSessionId = null
        stallWatchdog.reset(System.currentTimeMillis())
    }

    private fun pauseForDevice(reason: String) {
        userPaused = true
        releaseScreen()
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
        if (!NetworkStatus.online(this)) {
            pausedForInternet = true
            val alert = TaskAlertPolicy.internetResumeBlocked("İnternet hâlâ yok. Bağlantı gelince görev kendi devam edecek.")
            TaskAlerts.show(alert, currentAccount())
            updateNotification(alert.text)
            return
        }
        pausedForInternet = false
        val readiness = com.atmacanext.app.scheduling.DeviceReadinessChecker.read(this)
        if (!readiness.readyForUiAutomation) {
            updateNotification("Devam ettirilemedi: ${readiness.blockingReason}")
            return
        }
        userPaused = false
        val queue = AppServices.orchestrator.state.value
        if (queue.status == QueueStatus.PAUSED) AppServices.orchestrator.resume()
        else if (AutomationController.state.value.status == RuntimeStatus.PAUSED) AutomationController.resume()
        updateNotification("Güvenli yeniden doğrulama başlatıldı")
    }

    private fun stopAutomation() {
        userPaused = true
        releaseScreen()
        val queue = AppServices.orchestrator.state.value
        if (queue.isActive) AppServices.orchestrator.stop() else AutomationController.stop()
        stopSelf()
    }

    private fun startInternetWatch() {
        wasOnline = NetworkStatus.online(this)
        internetJob?.cancel()
        internetJob = scope.launch {
            while (true) {
                delay(InternetWatchPolicy.CHECK_MS)
                val online = NetworkStatus.online(this@AutomationForegroundService)
                when (InternetWatchPolicy.decide(wasOnline, online, isTaskRunning(), pausedForInternet)) {
                    InternetWatchPolicy.Event.LOST -> onInternetLost()
                    InternetWatchPolicy.Event.RESTORED -> onInternetRestored()
                    null -> Unit
                }
                noteResumeProblem(online)
                wasOnline = online
            }
        }
    }

    private fun isTaskRunning(): Boolean {
        val queue = AppServices.orchestrator.state.value
        val runtime = AutomationController.state.value
        return queue.status == QueueStatus.RUNNING ||
            (runtime.taskId != null && runtime.status !in terminalRuntimeStatuses() && runtime.status != RuntimeStatus.PAUSED)
    }

    private fun onInternetLost() {
        if (!isTaskRunning()) return
        pausedForInternet = true
        val alert = TaskAlertPolicy.internetLost()
        TaskAlerts.show(alert, currentAccount())
        AppServices.orchestrator.pauseForSafety("İnternet kesildi. Atmaca'ya dönüldü. Bağlantı gelince görev kaldığı yerden devam edecek.")
        AutomationController.returnToAtmaca()
        updateNotification(alert.text)
    }

    private fun onInternetRestored() {
        val readiness = com.atmacanext.app.scheduling.DeviceReadinessChecker.read(this)
        if (!readiness.readyForUiAutomation) {
            val now = System.currentTimeMillis()
            if (now - blockedNoticeAt > 5 * 60_000L) {
                blockedNoticeAt = now
                val alert = TaskAlertPolicy.internetResumeBlocked(readiness.blockingReason ?: "ekran hazır değil")
                TaskAlerts.show(alert, currentAccount())
                updateNotification(alert.text)
            }
            return
        }
        val queue = AppServices.orchestrator.state.value
        val runtime = AutomationController.state.value
        if (queue.status != QueueStatus.PAUSED && runtime.status != RuntimeStatus.PAUSED) {
            pausedForInternet = false
            return
        }
        pausedForInternet = false
        blockedNoticeAt = 0L
        resumeAttemptAt = System.currentTimeMillis()
        reportedResumeProblem = false
        val alert = TaskAlertPolicy.internetRestored()
        TaskAlerts.show(alert, currentAccount())
        if (queue.status == QueueStatus.PAUSED) AppServices.orchestrator.resume()
        else AutomationController.resume()
        updateNotification(alert.text)
    }

    private fun noteResumeProblem(online: Boolean) {
        if (resumeAttemptAt == 0L || reportedResumeProblem || !online) return
        val queue = AppServices.orchestrator.state.value
        val runtime = AutomationController.state.value
        val stillPaused = queue.status == QueueStatus.PAUSED || runtime.status == RuntimeStatus.PAUSED
        if (!stillPaused) {
            resumeAttemptAt = 0L
            return
        }
        if (System.currentTimeMillis() - resumeAttemptAt < 15_000L) return
        reportedResumeProblem = true
        val reason = runtime.message.ifBlank { queue.message }.ifBlank { "görev duraklatılmış kaldı" }
        TaskAlerts.show(TaskAlertPolicy.internetResumeBlocked(reason), currentAccount())
    }

    private fun currentAccount(): String =
        AutomationController.state.value.username?.removePrefix("@").orEmpty().ifBlank { "Atmaca" }

    @Suppress("DEPRECATION")
    private fun holdScreen() {
        val power = getSystemService(PowerManager::class.java) ?: return
        val lock = screenLock ?: power.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
            "AtmacaNext:task-screen",
        ).also {
            it.setReferenceCounted(false)
            screenLock = it
        }
        if (!lock.isHeld) {
            lock.acquire(ScreenStayPolicy.MAX_HOLD_MS)
            com.atmacanext.app.automation.OperationLog.i("SCREEN", "Görev bitene kadar ekran açık kalacak")
        }
    }

    private fun releaseScreen() {
        screenLock?.let { lock -> if (lock.isHeld) lock.release() }
    }

    private fun updateNotification(overrideText: String? = null) {
        val manager = getSystemService(android.app.NotificationManager::class.java) ?: return
        val running = isTaskRunning()
        val text = overrideText ?: if (!running && accessibilityHeld) {
            "Erişilebilirlik açık. Uygulamayı son uygulamalardan kaydırma."
        } else AutomationNotification.liveText()
        val title = if (!running && accessibilityHeld) "Atmaca Next açık" else "Atmaca Next otomasyon"
        manager.notify(
            AutomationNotification.SERVICE_NOTIFICATION_ID,
            AutomationNotification.build(this, title, text, includeActions = running),
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
        internetJob?.cancel()
        releaseScreen()
        scope.coroutineContext[Job]?.cancel()
        if (receiverRegistered) runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun terminalRuntimeStatuses() = setOf(RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED)
}
