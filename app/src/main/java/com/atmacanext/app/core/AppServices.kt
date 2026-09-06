package com.atmacanext.app.core

import android.content.Context
import com.atmacanext.app.ai.GeminiContentService
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.automation.AutomationRuntimeState
import com.atmacanext.app.automation.AutomationTuning
import com.atmacanext.app.automation.RuntimeStatus
import com.atmacanext.app.automation.TaskOrchestrator
import com.atmacanext.app.data.local.AtmacaDatabase
import com.atmacanext.app.data.report.ErrorReportExporter
import com.atmacanext.app.data.repository.AtmacaRepository
import com.atmacanext.app.data.settings.GeminiKeyStore
import com.atmacanext.app.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AppServices {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AtmacaDatabase
        private set
    lateinit var repository: AtmacaRepository
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var geminiKeyStore: GeminiKeyStore
        private set
    lateinit var contentService: GeminiContentService
        private set
    lateinit var orchestrator: TaskOrchestrator
        private set
    lateinit var reportExporter: ErrorReportExporter
        private set

    @Volatile
    var ready: Boolean = false
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (::database.isInitialized) return
        val appContext = context.applicationContext
        database = AtmacaDatabase.create(appContext)
        repository = AtmacaRepository(database)
        settings = SettingsStore(appContext)
        geminiKeyStore = GeminiKeyStore(appContext)
        contentService = GeminiContentService(geminiKeyStore)
        orchestrator = TaskOrchestrator(repository, contentService, settings.settings, scope)
        reportExporter = ErrorReportExporter(appContext, repository, settings)

        scope.launch {
            // Eski process/session hiçbir koşulda otomatik sürdürülmez.
            repository.discardInterruptedWork()
            val appSettings = settings.settings.first()
            applyTuning(appSettings)
            repository.pruneLogs(appSettings.keepLogDays)
            repository.pruneDailyUsage()
            ready = true
            observeRuntimePersistence()
            observeSettings()
        }
    }

    private fun observeRuntimePersistence() {
        scope.launch {
            var previous: AutomationRuntimeState? = null
            AutomationController.state.collect { current ->
                val prev = previous
                if (current.taskId == null && prev?.taskId != null && prev.status !in setOf(RuntimeStatus.COMPLETED, RuntimeStatus.FAILED)) {
                    repository.persistRuntime(prev.copy(status = RuntimeStatus.PAUSED, message = "Görev durduruldu; doğrulanmış ilerleme kaydedildi"))
                }
                repository.persistRuntime(current)
                if (shouldAudit(prev, current)) {
                    repository.log(
                        level = when (current.status) {
                            RuntimeStatus.FAILED -> "ERROR"
                            RuntimeStatus.PAUSED, RuntimeStatus.RECOVERING, RuntimeStatus.COOLDOWN -> "WARN"
                            else -> "INFO"
                        },
                        category = "RUNTIME",
                        taskId = current.taskId,
                        username = current.username,
                        message = current.message,
                        details = buildString {
                            append("session=${current.sessionId}; status=${current.status}")
                            append("; verified=${current.verifiedCount}/${current.limit}")
                            append("; cycle=${current.cycleIndex + 1}/${current.repeatCount}")
                            append("; screen=${current.activeScreen}; stage=${current.flowStage}")
                            current.lastTarget?.let { append("; lastTarget=$it") }
                        },
                    )
                }
                previous = current
            }
        }
    }

    private fun observeSettings() {
        scope.launch { settings.settings.distinctUntilChanged().collect(::applyTuning) }
    }

    private fun applyTuning(value: com.atmacanext.app.data.settings.AppSettings) {
        AutomationTuning.betweenActionsMs = value.betweenActionsMs
        AutomationTuning.accountSwitchSettleMs = value.accountSwitchSettleMs
        AutomationTuning.rateLimitCooldownMs = value.rateLimitCooldownMinutes * 60_000L
        orchestrator.applySettings(
            betweenTasksMs = value.betweenTasksMs,
            continueAfterFailedTask = value.continueAfterFailedTask,
            dailyFollowLimitPerAccount = value.dailyFollowLimitPerAccount,
            dailyUnfollowLimitPerAccount = value.dailyUnfollowLimitPerAccount,
        )
    }

    private fun shouldAudit(previous: AutomationRuntimeState?, current: AutomationRuntimeState): Boolean {
        if (previous == null) return true
        return previous.status != current.status ||
            previous.message != current.message ||
            previous.verifiedCount != current.verifiedCount ||
            previous.lastTarget != current.lastTarget ||
            previous.unfollowRevertCount != current.unfollowRevertCount ||
            previous.flowStage != current.flowStage
    }
}
