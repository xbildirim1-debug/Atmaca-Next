package com.atmacanext.app.data.repository

import androidx.room.withTransaction
import com.atmacanext.app.automation.AutomationRuntimeState
import com.atmacanext.app.data.local.AtmacaDatabase
import com.atmacanext.app.data.local.AutomationLogEntity
import com.atmacanext.app.data.local.DailyAccountUsageEntity
import com.atmacanext.app.domain.engine.AccountHealthCalculator
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.AccountHealthSnapshot
import com.atmacanext.app.domain.model.AutomationQueueState
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TargetAccount
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class AtmacaRepository(private val db: AtmacaDatabase) {
    val accounts: Flow<List<Account>> = db.accountDao().observeAll().map { rows -> rows.map { it.toDomain() } }
    val tasks: Flow<List<ScheduledTask>> = db.taskDao().observeAll().map { rows -> rows.map { it.toDomain() } }
    val recentLogs: Flow<List<AutomationLogEntity>> = db.automationLogDao().observeRecent(1_000)
    val targetAccounts: Flow<List<TargetAccount>> = db.targetAccountDao().observeAll().map { rows -> rows.map { it.toDomain() } }
    val accountHealth: Flow<List<AccountHealthSnapshot>> = combine(accounts, tasks, recentLogs) { accountRows, taskRows, logRows ->
        accountRows.map { AccountHealthCalculator.calculate(it, taskRows, logRows) }.sortedBy { it.score }
    }

    suspend fun upsertAccount(account: Account) = db.withTransaction {
        val existing = db.accountDao().getAll().firstOrNull { it.id == account.id }
        require(existing != null || db.accountDao().count() < 10) { "En fazla 10 hesap eklenebilir" }
        val reactivated = existing?.active == false && account.active
        db.accountDao().upsert(
            account.toEntity().copy(
                lastSyncAt = existing?.lastSyncAt,
                isCurrent = existing?.isCurrent ?: account.isCurrent,
                inactiveReason = if (reactivated) null else account.inactiveReason ?: existing?.inactiveReason,
                unfollowRevertCount = if (reactivated) 0 else existing?.unfollowRevertCount ?: account.unfollowRevertCount,
                aiPersona = account.aiPersona.ifBlank { existing?.aiPersona.orEmpty() },
                aiLanguage = account.aiLanguage.ifBlank { existing?.aiLanguage ?: "tr" },
                aiTone = account.aiTone.ifBlank { existing?.aiTone ?: "doğal" },
            )
        )
        log("INFO", "ACCOUNT", null, account.username, "X hesabı kaydedildi/güncellendi")
    }

    suspend fun upsertSyncedAccount(account: Account, markCurrent: Boolean = true) = db.withTransaction {
        val now = System.currentTimeMillis()
        val normalized = normalizeHandle(account.username)
        val existing = db.accountDao().getAll().firstOrNull { normalizeHandle(it.username) == normalized }
        require(existing != null || db.accountDao().count() < 10) { "En fazla 10 hesap eklenebilir; eski bir kaydı kaldırın" }
        val merged = account.copy(
            id = existing?.id ?: account.id,
            displayName = account.displayName.ifBlank { existing?.displayName.orEmpty() },
            engagement = existing?.engagement ?: account.engagement,
            health = existing?.health ?: account.health,
            accent = existing?.let { runCatching { com.atmacanext.app.domain.model.AccountAccent.valueOf(it.accent) }.getOrNull() } ?: account.accent,
            active = existing?.active ?: account.active,
            isCurrent = markCurrent,
            inactiveReason = existing?.inactiveReason,
            unfollowRevertCount = existing?.unfollowRevertCount ?: 0,
            aiPersona = existing?.aiPersona ?: account.aiPersona,
            aiLanguage = existing?.aiLanguage ?: account.aiLanguage,
            aiTone = existing?.aiTone ?: account.aiTone,
        )
        if (markCurrent) db.accountDao().clearCurrent()
        db.accountDao().upsert(merged.toEntity(now).copy(lastSyncAt = now, isCurrent = markCurrent))
        db.automationLogDao().insert(
            AutomationLogEntity(
                timestamp = now,
                level = "INFO",
                category = "ACCOUNT_SYNC",
                taskId = null,
                username = merged.username,
                message = "X hesabı senkronize edildi",
                details = "followers=${merged.followers}; following=${merged.following}; current=$markCurrent",
            )
        )
    }

    suspend fun markCurrentAccount(accountId: String) = db.withTransaction {
        db.accountDao().clearCurrent()
        db.accountDao().markCurrent(accountId, System.currentTimeMillis())
    }

    suspend fun deleteAccount(id: String) = db.withTransaction {
        val account = db.accountDao().getAll().firstOrNull { it.id == id }
        val taskIds = db.taskDao().getAll().filter { it.accountId == id }.map { it.id }
        if (taskIds.isNotEmpty()) db.taskDao().deleteByIds(taskIds)
        db.targetAccountDao().deleteByOwner(id)
        db.accountDao().deleteById(id)
        account?.let {
            db.automationLogDao().insert(
                AutomationLogEntity(
                    timestamp = System.currentTimeMillis(),
                    level = "WARN",
                    category = "ACCOUNT",
                    taskId = null,
                    username = it.username,
                    message = "X hesabı ve bağlı görevleri silindi",
                )
            )
        }
    }

    suspend fun upsertTask(task: ScheduledTask) {
        val existing = db.taskDao().getById(task.id)
        db.taskDao().upsert(
            task.toEntity().copy(
                lastTarget = existing?.lastTarget,
                lastActionAt = existing?.lastActionAt,
            )
        )
        log(
            "INFO",
            "TASK",
            task.id,
            task.username,
            "Görev kaydedildi: ${task.type.title}",
            "repeat=${task.repeatCount}; interval=${task.intervalMinutes}m; perCycle=${task.limit}",
        )
    }

    suspend fun deleteTask(id: String) = deleteTasks(listOf(id))

    suspend fun deleteTasks(ids: Collection<String>) = db.withTransaction {
        val safeIds = ids.distinct().filter(String::isNotBlank)
        if (safeIds.isEmpty()) return@withTransaction
        val rows = db.taskDao().getAll().filter { it.id in safeIds }
        db.taskDao().deleteByIds(safeIds)
        rows.forEach { task ->
            db.automationLogDao().insert(
                AutomationLogEntity(
                    timestamp = System.currentTimeMillis(),
                    level = "WARN",
                    category = "TASK",
                    taskId = task.id,
                    username = task.username,
                    message = "Görev silindi: ${task.type}",
                )
            )
        }
    }

    suspend fun resetTask(id: String) {
        db.taskDao().resetForScheduledCycle(id, System.currentTimeMillis())
        val task = db.taskDao().getById(id)
        log("INFO", "TASK", id, task?.username, "Görev ilerlemesi sıfırlandı")
    }

    suspend fun persistRuntime(state: AutomationRuntimeState) = db.withTransaction {
        db.runtimeCheckpointDao().upsert(state.toCheckpoint())
        val taskId = state.taskId ?: return@withTransaction
        val existing = db.taskDao().getById(taskId) ?: return@withTransaction
        val boundedProgress = state.verifiedCount.coerceIn(0, state.limit.coerceAtLeast(0))
        val verifiedDelta = (boundedProgress - existing.progress).coerceAtLeast(0)
        db.taskDao().updateRuntime(
            id = taskId,
            progress = boundedProgress,
            status = state.status.toTaskStatus().name,
            lastTarget = state.lastTarget,
            lastActionAt = state.lastActionAt,
            updatedAt = System.currentTimeMillis(),
        )
        if (verifiedDelta > 0 && existing.type in DAILY_LIMITED_TYPES.map(TaskType::name)) {
            val dateKey = todayDateKey()
            val usageDao = db.dailyAccountUsageDao()
            val previous = usageDao.get(dateKey, existing.accountId, existing.type)
            usageDao.upsert(
                DailyAccountUsageEntity(
                    dateKey = dateKey,
                    accountId = existing.accountId,
                    actionType = existing.type,
                    verifiedCount = (previous?.verifiedCount ?: 0) + verifiedDelta,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /** Process ölümü sonrası eski callback'leri canlandırmaz; ilerleme saklanır ama görev durur. */
    suspend fun discardInterruptedWork() = db.withTransaction {
        val checkpoint = db.runtimeCheckpointDao().get()
        if (checkpoint?.taskId != null) {
            db.taskDao().getById(checkpoint.taskId)?.let { task ->
                db.taskDao().updateRuntime(
                    id = task.id,
                    progress = checkpoint.verifiedCount.coerceAtLeast(0),
                    status = TaskStatus.PAUSED.name,
                    lastTarget = checkpoint.lastTarget,
                    lastActionAt = checkpoint.lastActionAt,
                    updatedAt = System.currentTimeMillis(),
                )
                db.automationLogDao().insert(
                    AutomationLogEntity(
                        timestamp = System.currentTimeMillis(),
                        level = "WARN",
                        category = "RESTART_STOP",
                        taskId = task.id,
                        username = task.username,
                        message = "Yarım görev uygulama açılışında güvenli biçimde durduruldu; otomatik devam ettirilmedi",
                    )
                )
            }
        }
        db.runtimeCheckpointDao().clear()
        db.queueItemDao().clear()
        db.queueCheckpointDao().clear()
    }

    suspend fun clearRuntimeCheckpoint() = db.runtimeCheckpointDao().clear()
    suspend fun getTaskById(id: String): ScheduledTask? = db.taskDao().getById(id)?.toDomain()

    suspend fun persistQueue(state: AutomationQueueState) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.queueCheckpointDao().upsert(
                com.atmacanext.app.data.local.QueueCheckpointEntity(
                    sessionId = state.sessionId,
                    status = state.status.name,
                    currentIndex = state.currentIndex,
                    message = state.message,
                    startedAt = state.startedAt,
                    updatedAt = now,
                )
            )
            db.queueItemDao().clear()
            if (state.items.isNotEmpty()) {
                db.queueItemDao().upsertAll(state.items.mapIndexed { index, item ->
                    com.atmacanext.app.data.local.QueueItemEntity(
                        taskId = item.taskId,
                        ordinal = index,
                        accountId = item.accountId,
                        username = item.username,
                        taskType = item.taskType.name,
                        status = item.status.name,
                        note = item.note,
                        updatedAt = now,
                    )
                })
            }
        }
    }

    suspend fun clearQueuePersistence() = db.withTransaction {
        db.queueItemDao().clear()
        db.queueCheckpointDao().clear()
    }

    suspend fun recordUnfollowRevertForTask(taskId: String): Pair<Int, Boolean> = db.withTransaction {
        val task = db.taskDao().getById(taskId) ?: return@withTransaction 0 to false
        val account = db.accountDao().getAll().firstOrNull { it.id == task.accountId } ?: return@withTransaction 0 to false
        val count = (account.unfollowRevertCount + 1).coerceAtMost(5)
        val deactivated = count >= 5
        val now = System.currentTimeMillis()
        if (deactivated) db.accountDao().markInactive(account.id, "5 takipten çıkma sonucu geri döndü; olası X günlük limiti", count, now)
        else db.accountDao().updateUnfollowRevertCount(account.id, count, now)
        db.automationLogDao().insert(
            AutomationLogEntity(
                timestamp = now,
                level = if (deactivated) "ERROR" else "WARN",
                category = "UNFOLLOW_REVERT",
                taskId = task.id,
                username = task.username,
                message = if (deactivated) "Hesap güvenlik nedeniyle pasife alındı" else "Takipten çıkma sonucu geri döndü ($count/5)",
            )
        )
        count to deactivated
    }

    suspend fun isAccountActive(accountId: String): Boolean = db.accountDao().getAll().firstOrNull { it.id == accountId }?.active == true
    suspend fun upsertTarget(target: TargetAccount) = db.targetAccountDao().upsert(target.toEntity())
    suspend fun deleteTarget(id: String) = db.targetAccountDao().deleteById(id)
    suspend fun getActiveTargets(accountId: String): List<TargetAccount> = db.targetAccountDao().getActiveForAccount(accountId).map { it.toDomain() }

    suspend fun getTodayVerifiedUsage(accountId: String, taskType: TaskType): Int =
        db.dailyAccountUsageDao().get(todayDateKey(), accountId, taskType.name)?.verifiedCount ?: 0

    suspend fun snapshotTodayUsage() = db.dailyAccountUsageDao().getForDate(todayDateKey())

    suspend fun pruneDailyUsage(keepDays: Long = 45L) {
        val oldest = LocalDate.now().minusDays(keepDays.coerceIn(7L, 365L)).toString()
        db.dailyAccountUsageDao().deleteOlderThanDate(oldest)
    }

    suspend fun log(level: String, category: String, taskId: String?, username: String?, message: String, details: String? = null) {
        db.automationLogDao().insert(
            AutomationLogEntity(
                timestamp = System.currentTimeMillis(),
                level = level,
                category = category,
                taskId = taskId,
                username = username,
                message = message,
                details = details,
            )
        )
    }

    suspend fun clearLogs() = db.automationLogDao().clearAll()
    suspend fun deleteLog(id: Long) = db.automationLogDao().deleteById(id)
    suspend fun pruneLogs(keepDays: Int) = db.automationLogDao().deleteOlderThan(
        System.currentTimeMillis() - keepDays.coerceIn(1, 365) * 86_400_000L
    )

    suspend fun snapshotAccountDomains(): List<Account> = db.accountDao().getAll().map { it.toDomain() }
    suspend fun snapshotAccounts() = db.accountDao().getAll()
    suspend fun snapshotTasks() = db.taskDao().getAll()
    suspend fun snapshotLogs(limit: Int = 2_000) = db.automationLogDao().getRecent(limit.coerceIn(1, 10_000))
    suspend fun snapshotQueueItems() = db.queueItemDao().getAll()
    suspend fun snapshotQueueCheckpoint() = db.queueCheckpointDao().get()
    suspend fun snapshotTargets() = db.targetAccountDao().getAll()

    private fun todayDateKey(): String = LocalDate.now().toString()
    private fun normalizeHandle(value: String): String = value.trim().lowercase().removePrefix("@")

    companion object {
        private val DAILY_LIMITED_TYPES = setOf(
            TaskType.VERIFIED_FOLLOW,
            TaskType.UNFOLLOW,
            TaskType.FOLLOW,
            TaskType.COMMENTER_FOLLOW,
            TaskType.RETWEETER_FOLLOW,
            TaskType.QUOTER_FOLLOW,
        )
    }
}
