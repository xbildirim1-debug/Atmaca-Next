package com.atmacanext.app.automation

import com.atmacanext.app.ai.GeminiContentService
import com.atmacanext.app.data.repository.AtmacaRepository
import com.atmacanext.app.domain.engine.TaskQueuePlanner
import com.atmacanext.app.domain.engine.QueueResultPolicy
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.AutomationQueueState
import com.atmacanext.app.domain.model.QueueItemStatus
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Serializes every manual/batch task so account changes and clicks can never overlap. */
class TaskOrchestrator(
    private val repository: AtmacaRepository,
    private val contentService: GeminiContentService,
    private val settingsFlow: kotlinx.coroutines.flow.Flow<com.atmacanext.app.data.settings.AppSettings>,
    private val scope: CoroutineScope,
    private val notifications: com.atmacanext.app.data.notifications.NotificationStore,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(AutomationQueueState())
    val state = _state.asStateFlow()
    private var transitionJob: Job? = null
    private var betweenTasksMs: Long = 3_000L
    private var continueAfterFailedTask: Boolean = true
    private var dailyFollowLimitPerAccount: Int = 35
    private var dailyUnfollowLimitPerAccount: Int = 35
    private var lastVerifiedAccountSession: String? = null

    init {
        scope.launch { AutomationController.state.collect(::handleRuntime) }
    }

    fun applySettings(
        betweenTasksMs: Long,
        continueAfterFailedTask: Boolean,
        dailyFollowLimitPerAccount: Int,
        dailyUnfollowLimitPerAccount: Int,
    ) {
        this.betweenTasksMs = betweenTasksMs.coerceIn(250L, 60_000L)
        this.continueAfterFailedTask = continueAfterFailedTask
        this.dailyFollowLimitPerAccount = dailyFollowLimitPerAccount.coerceIn(1, 100)
        this.dailyUnfollowLimitPerAccount = dailyUnfollowLimitPerAccount.coerceIn(1, 100)
    }

    fun startSingle(task: ScheduledTask) {
        scope.launch {
            val accounts = repository.snapshotAccountDomains()
            startSelectionInternal(accounts, listOf(task), "Manuel görev")
        }
    }

    fun startSelection(tasks: List<ScheduledTask>) {
        scope.launch {
            val accounts = repository.snapshotAccountDomains()
            startSelectionInternal(accounts, tasks, "Kullanıcının seçtiği toplu görevler")
        }
    }

    /** Kept for callers outside the UI; the new UI never exposes an implicit run-all button. */
    fun startAll(accounts: List<Account>, tasks: List<ScheduledTask>) {
        scope.launch { startSelectionInternal(accounts, tasks, "Açıkça verilen görev seçimi") }
    }

    fun pause() {
        pauseWithReason("Kullanıcı görev kuyruğunu duraklattı")
    }

    fun pauseForSafety(reason: String) {
        pauseWithReason(reason)
    }

    private fun pauseWithReason(reason: String) {
        scope.launch {
            val current = _state.value
            if (!current.isActive || current.status == QueueStatus.PAUSED) return@launch
            transitionJob?.cancel()
            mutate { state ->
                state.copy(
                    status = QueueStatus.PAUSED,
                    items = state.items.mapIndexed { index, item ->
                        if (index == state.currentIndex && item.status == QueueItemStatus.RUNNING) item.copy(status = QueueItemStatus.PAUSED, note = reason) else item
                    },
                    message = reason,
                )
            }
            AutomationController.pause(reason)
        }
    }

    fun resume() {
        scope.launch {
            if (AccountSyncController.isActive) {
                OperationLog.w("QUEUE", "Hesap taraması sürerken görev devam ettirilemez")
                return@launch
            }
            val current = _state.value
            if (current.status != QueueStatus.PAUSED) return@launch
            val runtime = AutomationController.state.value
            if (runtime.taskId == current.currentItem?.taskId && runtime.status == RuntimeStatus.PAUSED) {
                mutate { state -> state.copy(status = QueueStatus.RUNNING, message = "Görev ve hesap yeniden doğrulanıyor") }
                AutomationController.resume()
            } else {
                mutate { state -> state.copy(status = QueueStatus.PREPARING, message = "Görev sıfırdan güvenli biçimde hazırlanıyor") }
                launchCurrent()
            }
        }
    }

    fun stop() {
        scope.launch {
            transitionJob?.cancel()
            val current = _state.value
            if (current.status in TERMINAL_QUEUE) return@launch
            AutomationController.stop()
            mutate { state ->
                state.copy(
                    status = QueueStatus.STOPPED,
                    items = state.items.mapIndexed { index, item ->
                        if (index == state.currentIndex && item.status == QueueItemStatus.RUNNING) item.copy(status = QueueItemStatus.PAUSED, note = "Kullanıcı durdurdu") else item
                    },
                    message = "Görev durduruldu; gecikmiş hiçbir callback yeni işlem yapamaz",
                )
            }
            repository.log("WARN", "QUEUE_STOP", current.currentItem?.taskId, current.currentItem?.username, "Görev kuyruğu kullanıcı tarafından durduruldu")
        }
    }

    fun skipCurrentTask() {
        scope.launch { skipCurrent(accountWide = false) }
    }

    fun skipCurrentAccount() {
        scope.launch { skipCurrent(accountWide = true) }
    }

    private suspend fun skipCurrent(accountWide: Boolean) {
        val current = _state.value
        val item = current.currentItem ?: return
        transitionJob?.cancel()
        AutomationController.stop()
        mutate { state ->
            state.copy(
                status = QueueStatus.BETWEEN_TASKS,
                items = state.items.mapIndexed { index, row ->
                    val skip = if (accountWide) row.accountId == item.accountId && row.status in RUNNABLE_ITEM_STATUSES else index == state.currentIndex
                    if (skip) row.copy(status = QueueItemStatus.SKIPPED, note = if (accountWide) "Hesap atlandı" else "Görev atlandı") else row
                },
                message = if (accountWide) "${item.username} hesabının kalan görevleri atlandı" else "${item.taskType.title} görevi atlandı",
            )
        }
        scheduleAdvance(500L)
    }

    private suspend fun startSelectionInternal(accounts: List<Account>, tasks: List<ScheduledTask>, source: String): Boolean {
        transitionJob?.cancel()
        if (_state.value.isActive || AccountSyncController.isActive) {
            repository.log("WARN", "QUEUE_LOCK", null, null, "Yeni görev başlatılmadı; başka hesap/görev işlemi aktif")
            return false
        }
        val runtime = AutomationController.state.value
        if (runtime.taskId != null && runtime.status !in TERMINAL_RUNTIME) return false
        if (runtime.taskId != null) AutomationController.stop()

        val items = TaskQueuePlanner.build(accounts, tasks)
        if (items.isEmpty()) {
            mutate { AutomationQueueState(status = QueueStatus.IDLE, message = "Seçimde çalıştırılabilir aktif görev yok") }
            return false
        }
        val now = System.currentTimeMillis()
        mutate {
            AutomationQueueState(
                sessionId = UUID.randomUUID().toString(),
                status = QueueStatus.PREPARING,
                items = items,
                currentIndex = 0,
                message = "${items.size} açıkça seçilmiş görev seri kuyruğa alındı",
                startedAt = now,
                updatedAt = now,
            )
        }
        repository.log("INFO", "QUEUE_START", items.first().taskId, items.first().username, source, "tasks=${items.size}")
        launchCurrent()
        return true
    }

    private suspend fun launchCurrent() {
        val queue = _state.value
        val item = queue.currentItem ?: return finishQueue("Seçili görevlerin tamamı işlendi")
        if (item.status !in RUNNABLE_ITEM_STATUSES) return advanceNow()
        val task = repository.getTaskById(item.taskId)
        if (task == null) return skipAndAdvance("Görev veritabanında bulunamadı")
        if (task.status == TaskStatus.COMPLETED || task.progress >= task.totalLimit) return completeAndAdvance("Görev zaten tamamlanmış")
        val account = repository.snapshotAccountDomains().firstOrNull { it.id == task.accountId }
            ?: return skipAndAdvance("Görev hesabı bulunamadı")
        if (!account.active) return skipAndAdvance("Hesap pasif: ${account.inactiveReason ?: "kullanıcı ayarı"}", skipAccount = true)

        val dailyLimit = when (task.type) {
            // The visible task Limit is authoritative for manual unfollow work.
            // X's own daily ceiling is detected from a confirmed button reversion.
            TaskType.UNFOLLOW -> Int.MAX_VALUE
            TaskType.FOLLOW, TaskType.VERIFIED_FOLLOW, TaskType.COMMENTER_FOLLOW, TaskType.RETWEETER_FOLLOW, TaskType.QUOTER_FOLLOW -> dailyFollowLimitPerAccount
            else -> Int.MAX_VALUE
        }
        if (dailyLimit != Int.MAX_VALUE) {
            val used = repository.getTodayVerifiedUsage(task.accountId, task.type)
            if (used >= dailyLimit) return skipAndAdvance("Hesabın günlük ${task.type.title} limiti dolu: $used/$dailyLimit")
        }

        val targets = when {
            task.type == TaskType.COMMENT_QUOTE_TARGETS -> repository.getActiveQuoteTargets(task.accountId).map { it.handle }
            task.type.isDiscoveryFollow -> repository.getActiveTargets(task.accountId).map { it.handle }
            else -> emptyList()
        }
        if (task.type == TaskType.COMMENT_QUOTE_TARGETS && targets.isEmpty()) {
            return skipAndAdvance("Bu hesap için Hesaplar > Alıntı Hedefleri listesinde aktif hedef yok")
        }
        if (task.type.isDiscoveryFollow && targets.isEmpty()) return skipAndAdvance("Bu hesap için Ayarlar'da hedef kullanıcı tanımlanmamış")

        if (task.useGemini) return failAndAdvance("Bu sürüm API kullanmaz. Görevi düzenleyip metni elle gir.")
        val contents = if (task.type.supportsGemini) {
            val appSettings = settingsFlow.first()
            runCatching { contentService.prepare(task, account, appSettings.geminiModel) }.getOrElse { error ->
                repository.log("ERROR", "CONTENT_PREPARE", task.id, task.username, "İçerik hazırlanamadı", error.message)
                return failAndAdvance("İçerik hazırlanamadı: ${error.message ?: error.javaClass.simpleName}")
            }
        } else emptyList()

        mutate { state ->
            state.copy(
                status = QueueStatus.RUNNING,
                items = state.items.mapIndexed { index, row -> if (index == state.currentIndex) row.copy(status = QueueItemStatus.RUNNING, note = null) else row },
                message = "${task.username} • ${task.type.title} başlatılıyor (${state.currentIndex + 1}/${state.items.size})",
            )
        }
        lastVerifiedAccountSession = null
        val started = AutomationController.start(
            task = task,
            targets = targets,
            contents = contents,
            initialUnfollowReverts = account.unfollowRevertCount,
        )
        if (!started) failAndAdvance("Yeni görev motoru görevi kabul etmedi; alanları kontrol et")
    }

    private suspend fun handleRuntime(runtime: AutomationRuntimeState) {
        val queue = _state.value
        val item = queue.currentItem ?: return
        if (runtime.taskId != item.taskId || queue.status in TERMINAL_QUEUE) return

        if (runtime.accountVerified && lastVerifiedAccountSession != runtime.sessionId) {
            repository.markCurrentAccount(item.accountId)
            lastVerifiedAccountSession = runtime.sessionId
            repository.log("INFO", "ACCOUNT_VERIFIED", item.taskId, item.username, "Görev öncesi aktif X hesabı doğrulandı")
        }

        if (runtime.verifiedFollowAccountStopped) {
            if (item.status in RUNNABLE_ITEM_STATUSES) {
                val nextAccount = queue.items.drop(queue.currentIndex + 1).firstOrNull {
                    it.accountId != item.accountId && it.status in RUNNABLE_ITEM_STATUSES
                }?.username
                val continuation = nextAccount?.let { "Sıradaki hesap: $it." } ?: "Kuyrukta başka hesap kalmadı."
                val message = "Üç farklı kullanıcı art arda Takip ediliyor durumundan Takip et durumuna döndü. " +
                    "Bu hesabın onaylı takibi durduruldu. Doğrulanan: ${runtime.verifiedCount}/${runtime.limit}. $continuation"
                val saved = notifications.add(com.atmacanext.app.data.notifications.AccountNotification(
                    id = "verified-reverts:${queue.sessionId}:${item.accountId}", username = item.username,
                    message = message, createdAt = System.currentTimeMillis()))
                repository.log(if (saved) "WARN" else "ERROR", "ACCOUNT_NOTIFICATION", item.taskId, item.username,
                    message, if (saved) "Bildirim kaydedildi" else "Bildirim gösterildi; kalıcı kayıt başarısız")
                skipAndAdvance(runtime.message, skipAccount = true)
            }
            return
        }

        when (runtime.status) {
            RuntimeStatus.COMPLETED -> {
                if (item.status == QueueItemStatus.COMPLETED) return
                mutate { state ->
                    state.copy(
                        status = QueueStatus.BETWEEN_TASKS,
                        items = state.items.mapIndexed { index, row -> if (index == state.currentIndex) row.copy(status = QueueItemStatus.COMPLETED, note = runtime.message) else row },
                        message = "${item.username} görevi tamamlandı; sonraki görev hazırlanıyor",
                    )
                }
                repository.log("INFO", "QUEUE_TASK_DONE", item.taskId, item.username, runtime.message)
                // Her hesap işinden sonra Atmaca'yı öne alıp X'i kapat. Sırada
                // başka hesap varsa yeni görev X'i temiz şekilde yeniden açar.
                AutomationController.returnToAtmaca()
                scheduleAdvance(maxOf(betweenTasksMs, 3_000L))
            }
            RuntimeStatus.FAILED -> {
                if (item.status == QueueItemStatus.FAILED) return
                mutate { state ->
                    state.copy(
                        status = if (continueAfterFailedTask) QueueStatus.BETWEEN_TASKS else QueueStatus.PAUSED,
                        items = state.items.mapIndexed { index, row ->
                            if (index == state.currentIndex) row.copy(status = QueueItemStatus.FAILED, note = runtime.message) else row
                        },
                        message = runtime.message,
                    )
                }
                repository.log("ERROR", "QUEUE_TASK_FAILED", item.taskId, item.username, runtime.message)
                AutomationController.returnToAtmaca()
                if (continueAfterFailedTask) scheduleAdvance(maxOf(betweenTasksMs, 3_000L))
            }
            RuntimeStatus.PAUSED -> {
                transitionJob?.cancel()
                mutate { state ->
                    state.copy(
                        status = QueueStatus.PAUSED,
                        items = state.items.mapIndexed { index, row -> if (index == state.currentIndex) row.copy(status = QueueItemStatus.PAUSED, note = runtime.message) else row },
                        message = runtime.message,
                    )
                }
            }
            else -> {
                if (queue.status == QueueStatus.RUNNING && item.status == QueueItemStatus.RUNNING) return
                mutate { state -> state.copy(status = QueueStatus.RUNNING, message = runtime.message) }
            }
        }
    }

    private fun scheduleAdvance(delayMs: Long = betweenTasksMs) {
        transitionJob?.cancel()
        transitionJob = scope.launch {
            delay(delayMs)
            advanceNow()
        }
    }

    private suspend fun advanceNow() {
        AutomationController.stop()
        val queue = _state.value
        val next = TaskQueuePlanner.nextRunnableIndex(queue.items, queue.currentIndex)
        if (next < 0) return finishQueue("Seçili görev kuyruğu tamamlandı")
        val item = queue.items[next]
        mutate { state -> state.copy(status = QueueStatus.PREPARING, currentIndex = next, message = "Sıradaki: ${item.username} • ${item.taskType.title}") }
        launchCurrent()
    }

    private suspend fun skipAndAdvance(reason: String, skipAccount: Boolean = false) {
        val item = _state.value.currentItem ?: return
        mutate { state ->
            state.copy(
                status = QueueStatus.BETWEEN_TASKS,
                items = state.items.mapIndexed { index, row ->
                    val skip = if (skipAccount) row.accountId == item.accountId && row.status in RUNNABLE_ITEM_STATUSES else index == state.currentIndex
                    if (skip) row.copy(status = QueueItemStatus.SKIPPED, note = reason) else row
                },
                message = reason,
            )
        }
        repository.log("WARN", "QUEUE_SKIP", item.taskId, item.username, reason)
        scheduleAdvance(300L)
    }

    private suspend fun completeAndAdvance(reason: String) {
        mutate { state ->
            state.copy(
                status = QueueStatus.BETWEEN_TASKS,
                items = state.items.mapIndexed { index, row -> if (index == state.currentIndex) row.copy(status = QueueItemStatus.COMPLETED, note = reason) else row },
                message = reason,
            )
        }
        scheduleAdvance(300L)
    }

    private suspend fun failAndAdvance(reason: String) {
        val item = _state.value.currentItem ?: return
        mutate { state ->
            state.copy(
                status = if (continueAfterFailedTask) QueueStatus.BETWEEN_TASKS else QueueStatus.PAUSED,
                items = state.items.mapIndexed { index, row -> if (index == state.currentIndex) row.copy(status = QueueItemStatus.FAILED, note = reason) else row },
                message = reason,
            )
        }
        repository.log("ERROR", "QUEUE_PREPARE", item.taskId, item.username, reason)
        if (continueAfterFailedTask) scheduleAdvance(300L)
    }

    private suspend fun finishQueue(message: String) {
        transitionJob?.cancel()
        AutomationController.stop()
        val status = QueueResultPolicy.finalStatus(_state.value.items)
        val finalMessage = when (status) {
            QueueStatus.FAILED -> "Görev kuyruğu başarısız tamamlandı"
            QueueStatus.PARTIAL -> "Görev kuyruğu kısmen tamamlandı"
            else -> message
        }
        val final = mutate { it.copy(status = status, message = finalMessage) }
        val level = if (status == QueueStatus.FAILED) "ERROR" else if (status == QueueStatus.PARTIAL) "WARN" else "INFO"
        repository.log(level, "QUEUE_DONE", final.currentItem?.taskId, final.currentItem?.username, finalMessage, "completed=${final.completedCount}; skipped=${final.skippedCount}; failed=${final.failedCount}")
        AutomationController.returnToAtmaca()
    }

    private suspend fun mutate(transform: (AutomationQueueState) -> AutomationQueueState): AutomationQueueState = mutex.withLock {
        val next = transform(_state.value).copy(updatedAt = System.currentTimeMillis())
        _state.value = next
        repository.persistQueue(next)
        next
    }

    private companion object {
        val TERMINAL_RUNTIME = setOf(RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED)
        val TERMINAL_QUEUE = setOf(QueueStatus.IDLE, QueueStatus.COMPLETED, QueueStatus.PARTIAL, QueueStatus.FAILED, QueueStatus.STOPPED)
        val RUNNABLE_ITEM_STATUSES = setOf(QueueItemStatus.PENDING, QueueItemStatus.PAUSED, QueueItemStatus.RUNNING)
    }
}
