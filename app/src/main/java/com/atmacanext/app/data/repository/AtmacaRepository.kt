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
import com.atmacanext.app.domain.model.TargetKind
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class AtmacaRepository(private val db: AtmacaDatabase) {
    val accounts: Flow<List<Account>> = db.accountDao().observeAll().map { rows -> rows.map { it.toDomain() } }
    val tasks: Flow<List<ScheduledTask>> = db.taskDao().observeAll().map { rows -> rows.map { it.toDomain() } }
    val recentLogs: Flow<List<AutomationLogEntity>> = db.automationLogDao().observeRecent(1_000)
    val targetAccounts: Flow<List<TargetAccount>> = db.targetAccountDao().observeAll().map { rows -> rows.map { it.toDomain() } }
    val accountHealth: Flow<List<AccountHealthSnapshot>> = combine(accounts, tasks, recentLogs) { accountRows, taskRows, logRows -> accountRows.map { AccountHealthCalculator.calculate(it, taskRows, logRows) }.sortedBy { it.score } }

    suspend fun upsertAccount(account: Account) = db.withTransaction {
        val existing = db.accountDao().getAll().firstOrNull { it.id == account.id }
        require(existing != null || db.accountDao().count() < 10) { "En fazla 10 hesap eklenebilir" }
        val reactivated = existing?.active == false && account.active
        db.accountDao().upsert(account.toEntity().copy(
            lastSyncAt = existing?.lastSyncAt,
            isCurrent = existing?.isCurrent ?: account.isCurrent,
            inactiveReason = if (reactivated) null else account.inactiveReason ?: existing?.inactiveReason,
            unfollowRevertCount = if (reactivated) 0 else existing?.unfollowRevertCount ?: account.unfollowRevertCount,
            aiPersona = account.aiPersona.ifBlank { existing?.aiPersona.orEmpty() },
            aiLanguage = account.aiLanguage.ifBlank { existing?.aiLanguage ?: "tr" },
            aiTone = account.aiTone.ifBlank { existing?.aiTone ?: "doğal" },
        ))
        log("INFO", "ACCOUNT", null, account.username, "X hesabı kaydedildi/güncellendi")
    }

    suspend fun upsertSyncedAccount(account: Account, markCurrent: Boolean = true) = db.withTransaction {
        val now = System.currentTimeMillis()
        val normalized = normalizeHandle(account.username)
        val existing = db.accountDao().getAll().firstOrNull { normalizeHandle(it.username) == normalized }
        require(existing != null || db.accountDao().count() < 10) { "En fazla 10 hesap eklenebilir; eski bir kaydı kaldırın" }
        val merged = account.copy(
            id = existing?.id ?: account.id, displayName = account.displayName.ifBlank { existing?.displayName.orEmpty() },
            engagement = existing?.engagement ?: account.engagement, health = existing?.health ?: account.health,
            accent = existing?.let { runCatching { com.atmacanext.app.domain.model.AccountAccent.valueOf(it.accent) }.getOrNull() } ?: account.accent,
            active = existing?.active ?: account.active, isCurrent = markCurrent, inactiveReason = existing?.inactiveReason,
            unfollowRevertCount = existing?.unfollowRevertCount ?: 0, aiPersona = existing?.aiPersona ?: account.aiPersona,
            aiLanguage = existing?.aiLanguage ?: account.aiLanguage, aiTone = existing?.aiTone ?: account.aiTone,
        )
        if (markCurrent) db.accountDao().clearCurrent()
        db.accountDao().upsert(merged.toEntity(now).copy(lastSyncAt = now, isCurrent = markCurrent))
        db.automationLogDao().insert(AutomationLogEntity(now, "INFO", "ACCOUNT_SYNC", null, merged.username, "X hesabı senkronize edildi", "followers=${merged.followers}; following=${merged.following}; current=$markCurrent"))
    }

    suspend fun markCurrentAccount(accountId: String) = db.withTransaction { db.accountDao().clearCurrent(); db.accountDao().markCurrent(accountId, System.currentTimeMillis()) }

    suspend fun deleteAccount(id: String) = db.withTransaction {
        val account = db.accountDao().getAll().firstOrNull { it.id == id }
        val taskIds = db.taskDao().getAll().filter { it.accountId == id }.map { it.id }
        if (taskIds.isNotEmpty()) db.taskDao().deleteByIds(taskIds)
        db.targetAccountDao().deleteByOwner(id)
        db.accountDao().deleteById(id)
        account?.let { db.automationLogDao().insert(AutomationLogEntity(System.currentTimeMillis(), "WARN", "ACCOUNT", null, it.username, "X hesabı ve bağlı görevleri silindi")) }
    }

    suspend fun upsertTask(task: ScheduledTask) {
        val existing = db.taskDao().getById(task.id)
        db.taskDao().upsert(task.toEntity().copy(lastTarget = existing?.lastTarget, lastActionAt = existing?.lastActionAt))
        log("INFO", "TASK", task.id, task.username, "Görev kaydedildi: ${task.type.title}", "repeat=${task.repeatCount}; interval=${task.intervalMinutes}m; perCycle=${task.limit}")
    }
    suspend fun deleteTask(id: String) = deleteTasks(listOf(id))
    suspend fun deleteTasks(ids: Collection<String>) = db.withTransaction {
        val safeIds = ids.distinct().filter(String::isNotBlank); if (safeIds.isEmpty()) return@withTransaction
        val rows = db.taskDao().getAll().filter { it.id in safeIds }; db.taskDao().deleteByIds(safeIds)
        rows.forEach { task -> db.automationLogDao().insert(AutomationLogEntity(System.currentTimeMillis(), "WARN", "TASK", task.id, task.username, "Görev silindi: ${task.type}")) }
    }
    suspend fun resetTask(id: String) { db.taskDao().resetForScheduledCycle(id, System.currentTimeMillis()); val task = db.taskDao().getById(id); log("INFO", "TASK", id, task?.username, "Görev ilerlemesi sıfırlandı") }

    suspend fun persistRuntime(state: AutomationRuntimeState) = db.withTransaction {
        db.runtimeCheckpointDao().upsert(state.toCheckpoint())
        val taskId = state.taskId ?: return@withTransaction
        val existing = db.taskDao().getById(taskId) ?: return@withTransaction
        val boundedProgress = state.verifiedCount.coerceIn(0, state.limit.coerceAtLeast(0))
        val verifiedDelta = (boundedProgress - existing.progress).coerceAtLeast(0)
        db.taskDao().updateRuntime(taskId, boundedProgress, state.status.toTaskStatus().name, state.lastTarget, state.lastActionAt, System.currentTimeMillis())
        if (verifiedDelta > 0 && existing.type in DAILY_LIMITED_TYPES.map(TaskType::name)) {
            val dateKey = todayDateKey(); val usageDao = db.dailyAccountUsageDao(); val previous = usageDao.get(dateKey, existing.accountId, existing.type)
            usageDao.upsert(DailyAccountUsageEntity(dateKey, existing.accountId, existing.type, (previous?.verifiedCount ?: 0) + verifiedDelta, System.currentTimeMillis()))
        }
    }

    suspend fun discardInterruptedWork() = db.withTransaction {
        val checkpoint = db.runtimeCheckpointDao().get()
        if (checkpoint?.taskId != null) db.taskDao().getById(checkpoint.taskId)?.let { task ->
            db.taskDao().updateRuntime(task.id, checkpoint.verifiedCount.coerceAtLeast(0), TaskStatus.PAUSED.name, checkpoint.lastTarget, checkpoint.lastActionAt, System.currentTimeMillis())
            db.automationLogDao().insert(AutomationLogEntity(System.currentTimeMillis(), "WARN", "RESTART_STOP", task.id, task.username, "Yarım görev uygulama açılışında güvenli biçimde durduruldu; otomatik devam ettirilmedi"))
        }
        db.runtimeCheckpointDao().clear(); db.queueItemDao().clear(); db.queueCheckpointDao().clear()
    }
    suspend fun clearRuntimeCheckpoint() = db.runtimeCheckpointDao().clear()
    suspend fun getTaskById(id: String): ScheduledTask? = db.taskDao().getById(id)?.toDomain()

    suspend fun persistQueue(state: AutomationQueueState) { val now = System.currentTimeMillis(); db.withTransaction {
        db.queueCheckpointDao().upsert(com.atmacanext.app.data.local.QueueCheckpointEntity(
            sessionId = state.sessionId,
            status = state.status.name,
            currentIndex = state.currentIndex,
            message = state.message,
            startedAt = state.startedAt,
            updatedAt = now,
        ))
        db.queueItemDao().clear()
        if (state.items.isNotEmpty()) db.queueItemDao().upsertAll(state.items.mapIndexed { index, item -> com.atmacanext.app.data.local.QueueItemEntity(item.taskId, index, item.accountId, item.username, item.taskType.name, item.status.name, item.note, now) })
    } }
    suspend fun clearQueuePersistence() = db.withTransaction { db.queueItemDao().clear(); db.queueCheckpointDao().clear() }

    suspend fun recordUnfollowRevertForTask(taskId: String): Pair<Int, Boolean> = db.withTransaction {
        val task = db.taskDao().getById(taskId) ?: return@withTransaction 0 to false
        val account = db.accountDao().getAll().firstOrNull { it.id == task.accountId } ?: return@withTransaction 0 to false
        val count = (account.unfollowRevertCount + 1).coerceAtMost(5); val deactivated = count >= 5; val now = System.currentTimeMillis()
        if (deactivated) db.accountDao().markInactive(account.id, "5 takipten çıkma sonucu geri döndü; olası X günlük limiti", count, now) else db.accountDao().updateUnfollowRevertCount(account.id, count, now)
        db.automationLogDao().insert(AutomationLogEntity(now, if (deactivated) "ERROR" else "WARN", "UNFOLLOW_REVERT", task.id, task.username, if (deactivated) "Hesap güvenlik nedeniyle pasife alındı" else "Takipten çıkma sonucu geri döndü ($count/5)"))
        count to deactivated
    }
    suspend fun isAccountActive(accountId: String): Boolean = db.accountDao().getAll().firstOrNull { it.id == accountId }?.active == true

    private fun normalizeTarget(value: String) = requireNotNull(com.atmacanext.app.domain.policy.TargetPagePolicy.normalize(value)) { "Geçerli bir kullanıcı adı gir" }
    private suspend fun owner(ownerId: String) = requireNotNull(db.accountDao().getAll().firstOrNull { it.id == ownerId }) { "Hesap bulunamadı" }

    suspend fun upsertTarget(target: TargetAccount) = db.withTransaction {
        val handle = normalizeTarget(target.handle); val account = owner(target.ownerAccountId)
        require(handle != com.atmacanext.app.domain.policy.TargetPagePolicy.normalize(account.username)) { "Kendi hesabını hedef seçme" }
        val existing = db.targetAccountDao().getAll().filter { it.ownerAccountId == target.ownerAccountId && it.kind == TargetKind.STANDARD.name }
        require(existing.any { it.handle.equals(handle, true) } || existing.isEmpty()) { "Her hesaba en fazla 1 hedef eklenebilir" }
        val id = existing.firstOrNull { it.handle.equals(handle, true) }?.id ?: UUID.nameUUIDFromBytes("${target.ownerAccountId}:STANDARD:$handle".toByteArray()).toString()
        db.targetAccountDao().upsert(target.copy(id = id, handle = handle, kind = TargetKind.STANDARD).toEntity())
    }

    suspend fun replaceTargets(ownerId: String, handles: List<String>) = db.withTransaction {
        require(handles.size <= 1) { "En fazla 1 hedef eklenebilir" }
        val names = handles.map(::normalizeTarget).distinct(); val account = owner(ownerId)
        require(names.none { it == com.atmacanext.app.domain.policy.TargetPagePolicy.normalize(account.username) }) { "Kendi hesabını hedef seçme" }
        db.targetAccountDao().getAll().filter { it.ownerAccountId == ownerId && it.kind == TargetKind.STANDARD.name && it.handle !in names }.forEach { db.targetAccountDao().deleteById(it.id) }
        names.forEach { handle -> db.targetAccountDao().upsert(TargetAccount(UUID.nameUUIDFromBytes("$ownerId:STANDARD:$handle".toByteArray()).toString(), ownerId, handle, true, TargetKind.STANDARD).toEntity()) }
    }

    suspend fun replaceQuoteTargets(ownerId: String, handles: List<String>) = db.withTransaction {
        require(handles.size <= 5) { "Her hesaba en fazla 5 alıntı hedefi eklenebilir" }
        val names = handles.map(::normalizeTarget).distinct(); require(names.size == handles.size) { "Aynı alıntı hedefi iki kez eklenemez" }
        val account = owner(ownerId); require(names.none { it == com.atmacanext.app.domain.policy.TargetPagePolicy.normalize(account.username) }) { "Kendi hesabını alıntı hedefi seçme" }
        db.targetAccountDao().getAll().filter { it.ownerAccountId == ownerId && it.kind == TargetKind.QUOTE.name && it.handle !in names }.forEach { db.targetAccountDao().deleteById(it.id) }
        names.forEach { handle -> db.targetAccountDao().upsert(TargetAccount(UUID.nameUUIDFromBytes("$ownerId:QUOTE:$handle".toByteArray()).toString(), ownerId, handle, true, TargetKind.QUOTE).toEntity()) }
    }

    suspend fun getActiveTargets(accountId: String): List<TargetAccount> = db.targetAccountDao().getActiveForAccount(accountId).map { it.toDomain() }
    suspend fun getActiveQuoteTargets(accountId: String): List<TargetAccount> = db.targetAccountDao().getActiveQuoteForAccount(accountId).map { it.toDomain() }
    suspend fun deleteTarget(id: String) = db.targetAccountDao().deleteById(id)
    suspend fun getTodayVerifiedUsage(accountId: String, taskType: TaskType): Int = db.dailyAccountUsageDao().get(todayDateKey(), accountId, taskType.name)?.verifiedCount ?: 0
    suspend fun snapshotTodayUsage() = db.dailyAccountUsageDao().getForDate(todayDateKey())
    suspend fun pruneDailyUsage(keepDays: Long = 45L) { db.dailyAccountUsageDao().deleteOlderThanDate(LocalDate.now().minusDays(keepDays.coerceIn(7L, 365L)).toString()) }
    suspend fun log(level: String, category: String, taskId: String?, username: String?, message: String, details: String? = null) { db.automationLogDao().insert(AutomationLogEntity(System.currentTimeMillis(), level, category, taskId, username, message, details)) }
    suspend fun clearLogs() = db.automationLogDao().clearAll()
    suspend fun deleteLog(id: Long) = db.automationLogDao().deleteById(id)
    suspend fun pruneLogs(keepDays: Int) = db.automationLogDao().deleteOlderThan(System.currentTimeMillis() - keepDays.coerceIn(1, 365) * 86_400_000L)
    suspend fun snapshotAccountDomains(): List<Account> = db.accountDao().getAll().map { it.toDomain() }
    suspend fun snapshotAccounts() = db.accountDao().getAll()
    suspend fun snapshotTasks() = db.taskDao().getAll()
    suspend fun snapshotLogs(limit: Int = 2_000) = db.automationLogDao().getRecent(limit.coerceIn(1, 10_000))
    suspend fun snapshotQueueItems() = db.queueItemDao().getAll()
    suspend fun snapshotQueueCheckpoint() = db.queueCheckpointDao().get()
    suspend fun snapshotTargets() = db.targetAccountDao().getAll()
    private fun todayDateKey(): String = LocalDate.now().toString()
    private fun normalizeHandle(value: String): String = value.trim().lowercase().removePrefix("@")
    companion object { private val DAILY_LIMITED_TYPES = setOf(TaskType.VERIFIED_FOLLOW, TaskType.UNFOLLOW, TaskType.FOLLOW, TaskType.COMMENTER_FOLLOW, TaskType.RETWEETER_FOLLOW, TaskType.QUOTER_FOLLOW) }
}
