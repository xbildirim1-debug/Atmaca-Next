package com.buse.app.automation

import com.buse.app.ai.GeminiContentService
import com.buse.app.data.repository.BuseRepository
import com.buse.app.domain.engine.BatchCyclePolicy
import com.buse.app.domain.engine.TaskQueuePlanner
import com.buse.app.domain.engine.QueueResultPolicy
import com.buse.app.domain.model.Account
import com.buse.app.domain.model.AutomationQueueState
import com.buse.app.domain.model.QueueItemStatus
import com.buse.app.domain.model.QueueStatus
import com.buse.app.domain.model.ScheduledTask
import com.buse.app.domain.model.TaskStatus
import com.buse.app.domain.model.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
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
    private val repository: BuseRepository,
    private val contentService: GeminiContentService,
    private val settingsFlow: kotlinx.coroutines.flow.Flow<com.buse.app.data.settings.AppSettings>,
    private val scope: CoroutineScope,
    private val notifications: com.buse.app.data.notifications.NotificationStore,
) {
    private val mutex = Mutex()
    private val startMutex = Mutex()
    private val _state = MutableStateFlow(AutomationQueueState())
    val state = _state.asStateFlow()
    private var transitionJob: Job? = null
    private var betweenTasksMs: Long = AutomationSpeedPreset.TASK_MS
    private var continueAfterFailedTask: Boolean = true
    private var dailyFollowLimitPerAccount: Int = 35
    private var dailyUnfollowLimitPerAccount: Int = 35
    private var lastVerifiedAccountSession: String? = null
    private val deferredCycleTasks = LinkedHashSet<String>()
    private var roundIntervalMs = 0L
    private var roundWaitUntil: Long? = null

    init {
        scope.launch { AutomationController.state.collect(::handleRuntime) }
    }

    fun applySettings(
        betweenTasksMs: Long,
        continueAfterFailedTask: Boolean,
        dailyFollowLimitPerAccount: Int,
        dailyUnfollowLimitPerAccount: Int,
    ) {
        this.betweenTasksMs = betweenTasksMs.coerceIn(100L, 60_000L)
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
            if (roundWaitUntil != null) {
                mutate { it.copy(status = QueueStatus.WAITING_INTERVAL, message = "Toplu döngü beklemesi sürdürülüyor") }
                scheduleRoundResume()
                return@launch
            }
            if (current.currentItem?.taskId in deferredCycleTasks) {
                mutate { it.copy(status = QueueStatus.BETWEEN_TASKS, message = "Diğer hesapların turu sürdürülüyor") }
                scheduleAdvance(300L)
                return@launch
            }
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

    private suspend fun startSelectionInternal(accounts: List<Account>, tasks: List<ScheduledTask>, source: String): Boolean = startMutex.withLock {
        if (_state.value.isActive || AccountSyncController.isActive) {
            repository.log("WARN", "QUEUE_LOCK", null, null, "Yeni görev başlatılmadı; başka hesap/görev işlemi aktif")
            return@withLock false
        }
        val runtime = AutomationController.state.value
        if (runtime.taskId != null && runtime.status !in TERMINAL_RUNTIME) return@withLock false
        transitionJob?.cancel()
        if (runtime.taskId != null) AutomationController.stop()

        deferredCycleTasks.clear()
        roundIntervalMs = 0L
        roundWaitUntil = null
        val items = TaskQueuePlanner.build(accounts, tasks.filter(com.buse.app.domain.policy.BuseTaskPolicy::accepts))
        if (items.isEmpty()) {
            mutate { AutomationQueueState(status = QueueStatus.IDLE, message = "Seçimde çalıştırılabilir aktif görev yok") }
            return@withLock false
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
        return@withLock true
    }

    private suspend fun launchCurrent() {
        val queue = _state.value
        val item = queue.currentItem ?: return finishQueue("Seçili görevlerin tamamı işlendi")
        if (item.status !in RUNNABLE_ITEM_STATUSES) return advanceNow()
        val original = repository.getTaskById(item.taskId) ?: return skipAndAdvance("Görev veritabanında bulunamadı")
        if (!com.buse.app.domain.policy.BuseTaskPolicy.accepts(original)) return skipAndAdvance("Buse bu görev türünü desteklemiyor")
        val task = if (original.type == TaskType.COMMENT_QUOTE_TARGETS && original.quoteTargets.isNullOrBlank()) {
            val handles = repository.getActiveQuoteTargets(original.accountId).map { it.handle }
            if (handles.isEmpty()) return skipAndAdvance("Bu hesap için aktif Alıntı Hedefleri yok")
            original.copy(quoteTargets = handles.joinToString("\n")).also { repository.upsertTask(it) }
        } else original
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
            if (used >= dailyLimit) return skipAndAdvance("Hesabın günlük ${com.buse.app.domain.policy.BuseTaskPolicy.mode(task).title} limiti dolu: $used/$dailyLimit")
        }

        val targets = when {
            task.type == TaskType.COMMENT_QUOTE_TARGETS -> task.quoteTargetHandles
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
                message = "${task.username} • ${com.buse.app.domain.policy.BuseTaskPolicy.mode(task).title} başlatılıyor (${state.currentIndex + 1}/${state.items.size})",
            )
        }
        lastVerifiedAccountSession = null
        val started = AutomationController.start(
            task = task,
            targets = targets,
            contents = contents,
            initialUnfollowReverts = account.unfollowRevertCount,
            delegateCycleWait = true,
        )
        if (!started) failAndAdvance("Yeni görev motoru görevi kabul etmedi; alanları kontrol et")
    }

    private suspend fun handleRuntime(runtime: AutomationRuntimeState) {
        val queue = _state.value
        val item = queue.currentItem ?: return
        if (runtime.taskId != item.taskId || queue.status in TERMINAL_QUEUE) return
        // Late snapshots of a terminal item cannot cancel its scheduled hand-off.
        if (item.status !in RUNNABLE_ITEM_STATUSES) return

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
                val saved = notifications.add(com.buse.app.data.notifications.AccountNotification(
                    id = "verified-reverts:${queue.sessionId}:${item.accountId}", username = item.username,
                    message = message, createdAt = System.currentTimeMillis()))
                repository.log(if (saved) "WARN" else "ERROR", "ACCOUNT_NOTIFICATION", item.taskId, item.username,
                    message, if (saved) "Bildirim kaydedildi" else "Bildirim gösterildi; kalıcı kayıt başarısız")
                skipAndAdvance(runtime.message, skipAccount = true)
            }
            return
        }

        when (runtime.status) {
            RuntimeStatus.WAITING -> {
                if (runtime.flowStage != XFlowStage.WAIT_INTERVAL) return
                if (!deferredCycleTasks.add(item.taskId)) return
                repository.persistRuntime(runtime)
                roundIntervalMs = maxOf(roundIntervalMs, runtime.intervalMinutes * 60_000L)
                mutate { state -> state.copy(
                    status = QueueStatus.BETWEEN_TASKS,
                    items = state.items.mapIndexed { index, row ->
                        if (index == state.currentIndex) row.copy(status = QueueItemStatus.PAUSED, note = "Tur tamamlandı; diğer hesaplar çalışacak") else row
                    },
                    message = "${item.username} turu tamamlandı; sıradaki hesap hazırlanıyor",
                ) }
                AutomationController.stop()
                repository.log("INFO", "QUEUE_CYCLE_DONE", item.taskId, item.username,
                    "Hesabın turu bitti; ortak beklemeden önce diğer hesaplara geçiliyor", "progress=${runtime.verifiedCount}/${runtime.limit}")
                scheduleAdvance(betweenTasksMs)
            }
            RuntimeStatus.COMPLETED -> {
                if (item.status == QueueItemStatus.COMPLETED) return
                // The independent StateFlow persistence observer can conflate this
                // final snapshot with the next task. Save it before stop/start.
                repository.persistRuntime(runtime)
                mutate { state ->
                    state.copy(
                        status = QueueStatus.BETWEEN_TASKS,
                        items = state.items.mapIndexed { index, row -> if (index == state.currentIndex) row.copy(status = QueueItemStatus.COMPLETED, note = runtime.message) else row },
                        message = "${item.username} görevi tamamlandı; sonraki görev hazırlanıyor",
                    )
                }
                repository.log("INFO", "QUEUE_TASK_DONE", item.taskId, item.username, runtime.message)
                // Stay in X until the final selected account is finished.
                scheduleAdvance(betweenTasksMs)
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
                if (continueAfterFailedTask) scheduleAdvance(betweenTasksMs)
                else AutomationController.returnToBuse()
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
        val session = _state.value.sessionId
        val index = _state.value.currentIndex
        transitionJob = scope.launch {
            delay(delayMs)
            val current = _state.value
            if (current.sessionId != session || current.currentIndex != index || current.status != QueueStatus.BETWEEN_TASKS) return@launch
            advanceNow()
        }
    }

    private suspend fun advanceNow() {
        val queue = _state.value
        val next = TaskQueuePlanner.nextRunnableIndex(queue.items, queue.currentIndex)
        repository.log("INFO", "QUEUE_DECISION", queue.currentItem?.taskId, queue.currentItem?.username,
            "Sıradaki seçili görev değerlendirildi", "current=" + queue.currentIndex + "; next=" + next + "; items=" + queue.items.size)
        if (next < 0) {
            if (BatchCyclePolicy.nextRoundIndices(queue.items, deferredCycleTasks).isNotEmpty()) {
                AutomationController.stop()
                if (roundWaitUntil == null) roundWaitUntil = System.currentTimeMillis() + roundIntervalMs
                mutate { it.copy(status = QueueStatus.WAITING_INTERVAL,
                    message = "Tüm hesapların turu tamamlandı; sonraki tur için ${roundIntervalMs / 60_000L} dakika bekleniyor") }
                repository.log("INFO", "QUEUE_CYCLE_WAIT", queue.currentItem?.taskId, queue.currentItem?.username,
                    "Tüm seçili hesaplardan sonra ortak döngü beklemesi", "until=$roundWaitUntil")
                AutomationController.returnToBuse()
                scheduleRoundResume()
                return
            }
            return finishQueue("Seçili görev kuyruğu tamamlandı")
        }
        AutomationController.stop()
        val item = queue.items[next]
        repository.log("INFO", "QUEUE_HANDOFF", item.taskId, item.username, "Sıradaki görev X içinde başlatılıyor")
        mutate { state -> state.copy(status = QueueStatus.PREPARING, currentIndex = next, message = "Sıradaki: ${item.username} • ${item.taskType.title}") }
        launchCurrent()
    }

    private suspend fun scheduleRoundResume() {
        val currentJob = currentCoroutineContext()[Job]
        transitionJob?.takeIf { it != currentJob }?.cancel()
        val session = _state.value.sessionId
        val deadline = roundWaitUntil ?: return
        transitionJob = scope.launch {
            while (System.currentTimeMillis() < deadline) {
                val stillWaiting = mutex.withLock {
                    val current = _state.value
                    if (current.sessionId != session || current.status != QueueStatus.WAITING_INTERVAL || roundWaitUntil != deadline) false
                    else {
                        // Display-only tick: do not write a database checkpoint every second.
                        _state.value = current.copy(message = BatchCyclePolicy.remainingLabel(deadline, System.currentTimeMillis()))
                        true
                    }
                }
                if (!stillWaiting) return@launch
                delay(minOf(1_000L, (deadline - System.currentTimeMillis()).coerceAtLeast(1L)))
            }
            val queue = _state.value
            if (queue.sessionId != session || queue.status != QueueStatus.WAITING_INTERVAL || roundWaitUntil != deadline) return@launch
            val indices = BatchCyclePolicy.nextRoundIndices(queue.items, deferredCycleTasks)
            roundWaitUntil = null
            roundIntervalMs = 0L
            deferredCycleTasks.clear()
            if (indices.isEmpty()) return@launch finishQueue("Seçili görev kuyruğu tamamlandı")
            mutate { state -> state.copy(status = QueueStatus.PREPARING, currentIndex = indices.first(),
                items = state.items.mapIndexed { index, row ->
                    if (index in indices) row.copy(status = QueueItemStatus.PENDING, note = null) else row
                }, message = "Yeni toplu tur başlıyor; ilk kalan hesap hazırlanıyor") }
            launchCurrent()
        }
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
        val currentJob = currentCoroutineContext()[Job]
        transitionJob?.takeIf { it != currentJob }?.cancel()
        transitionJob = null
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
        AutomationController.returnToBuse { returned ->
            scope.launch {
                if (_state.value.sessionId != final.sessionId || _state.value.isActive) return@launch
                repository.log(if (returned) "INFO" else "WARN", "QUEUE_RETURN", final.currentItem?.taskId,
                    final.currentItem?.username, if (returned) "Seçili görevler bitti; Buse'e dönüş doğrulandı"
                    else "Görevler bitti; Buse dönüşü doğrulanamadı, dönüş bildirimi kontrol edilmeli")
            }
        }
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
