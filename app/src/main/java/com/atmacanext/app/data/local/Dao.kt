package com.atmacanext.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isCurrent DESC, id") fun observeAll(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts ORDER BY isCurrent DESC, id") suspend fun getAll(): List<AccountEntity>
    @Query("SELECT COUNT(*) FROM accounts") suspend fun count(): Int
    @Upsert suspend fun upsertAll(items: List<AccountEntity>)
    @Upsert suspend fun upsert(item: AccountEntity)
    @Query("DELETE FROM accounts WHERE id = :id") suspend fun deleteById(id: String)
    @Query("UPDATE accounts SET isCurrent = 0") suspend fun clearCurrent()
    @Query("UPDATE accounts SET isCurrent = 1, updatedAt = :updatedAt WHERE id = :id") suspend fun markCurrent(id: String, updatedAt: Long)
    @Query("UPDATE accounts SET active = 0, inactiveReason = :reason, unfollowRevertCount = :count, updatedAt = :updatedAt WHERE id = :id") suspend fun markInactive(id: String, reason: String, count: Int, updatedAt: Long)
    @Query("UPDATE accounts SET unfollowRevertCount = :count, updatedAt = :updatedAt WHERE id = :id") suspend fun updateUnfollowRevertCount(id: String, count: Int, updatedAt: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY updatedAt DESC, id") fun observeAll(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM scheduled_tasks ORDER BY updatedAt DESC, id") suspend fun getAll(): List<TaskEntity>
    @Query("SELECT * FROM scheduled_tasks WHERE id = :id LIMIT 1") suspend fun getById(id: String): TaskEntity?
    @Query("SELECT COUNT(*) FROM scheduled_tasks") suspend fun count(): Int
    @Upsert suspend fun upsertAll(items: List<TaskEntity>)
    @Upsert suspend fun upsert(item: TaskEntity)
    @Query("UPDATE scheduled_tasks SET progress = 0, status = 'QUEUED', lastTarget = NULL, lastActionAt = NULL, updatedAt = :updatedAt WHERE id = :id") suspend fun resetForScheduledCycle(id: String, updatedAt: Long)
    @Query("DELETE FROM scheduled_tasks WHERE id = :id") suspend fun deleteById(id: String)
    @Query("DELETE FROM scheduled_tasks WHERE id IN (:ids)") suspend fun deleteByIds(ids: List<String>)
    @Query("UPDATE scheduled_tasks SET progress = :progress, status = :status, lastTarget = :lastTarget, lastActionAt = :lastActionAt, updatedAt = :updatedAt WHERE id = :id") suspend fun updateRuntime(id: String, progress: Int, status: String, lastTarget: String?, lastActionAt: Long?, updatedAt: Long)
}

@Dao interface RuntimeCheckpointDao {
    @Query("SELECT * FROM runtime_checkpoint WHERE id = 1 LIMIT 1") suspend fun get(): RuntimeCheckpointEntity?
    @Upsert suspend fun upsert(checkpoint: RuntimeCheckpointEntity)
    @Query("DELETE FROM runtime_checkpoint") suspend fun clear()
}

@Dao interface AutomationLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: AutomationLogEntity): Long
    @Query("SELECT * FROM automation_logs ORDER BY timestamp DESC LIMIT :limit") fun observeRecent(limit: Int): Flow<List<AutomationLogEntity>>
    @Query("SELECT * FROM automation_logs ORDER BY timestamp DESC LIMIT :limit") suspend fun getRecent(limit: Int): List<AutomationLogEntity>
    @Query("DELETE FROM automation_logs WHERE timestamp < :cutoff") suspend fun deleteOlderThan(cutoff: Long)
    @Query("DELETE FROM automation_logs") suspend fun clearAll()
    @Query("DELETE FROM automation_logs WHERE id = :id") suspend fun deleteById(id: Long)
}

@Dao
interface TargetAccountDao {
    @Query("SELECT * FROM target_accounts ORDER BY ownerAccountId, kind, handle") fun observeAll(): Flow<List<TargetAccountEntity>>
    @Query("SELECT * FROM target_accounts ORDER BY ownerAccountId, kind, handle") suspend fun getAll(): List<TargetAccountEntity>
    @Query("SELECT * FROM target_accounts WHERE ownerAccountId = :ownerAccountId AND active = 1 AND kind = 'STANDARD' ORDER BY handle") suspend fun getActiveForAccount(ownerAccountId: String): List<TargetAccountEntity>
    @Query("SELECT * FROM target_accounts WHERE ownerAccountId = :ownerAccountId AND active = 1 AND kind = 'QUOTE' ORDER BY handle") suspend fun getActiveQuoteForAccount(ownerAccountId: String): List<TargetAccountEntity>
    @Upsert suspend fun upsert(item: TargetAccountEntity)
    @Query("DELETE FROM target_accounts WHERE id = :id") suspend fun deleteById(id: String)
    @Query("DELETE FROM target_accounts WHERE ownerAccountId = :ownerAccountId") suspend fun deleteByOwner(ownerAccountId: String)
    @Query("DELETE FROM target_accounts WHERE ownerAccountId = :ownerAccountId AND kind = :kind") suspend fun deleteByOwnerAndKind(ownerAccountId: String, kind: String)
}

@Dao interface QueueCheckpointDao {
    @Query("SELECT * FROM queue_checkpoint WHERE id = 1 LIMIT 1") suspend fun get(): QueueCheckpointEntity?
    @Upsert suspend fun upsert(checkpoint: QueueCheckpointEntity)
    @Query("DELETE FROM queue_checkpoint") suspend fun clear()
}

@Dao interface QueueItemDao {
    @Query("SELECT * FROM queue_items ORDER BY ordinal") suspend fun getAll(): List<QueueItemEntity>
    @Upsert suspend fun upsertAll(items: List<QueueItemEntity>)
    @Query("DELETE FROM queue_items") suspend fun clear()
}

@Dao interface DailyAccountUsageDao {
    @Query("SELECT * FROM daily_account_usage WHERE dateKey = :dateKey AND accountId = :accountId AND actionType = :actionType LIMIT 1") suspend fun get(dateKey: String, accountId: String, actionType: String): DailyAccountUsageEntity?
    @Query("SELECT * FROM daily_account_usage WHERE dateKey = :dateKey ORDER BY accountId, actionType") suspend fun getForDate(dateKey: String): List<DailyAccountUsageEntity>
    @Upsert suspend fun upsert(item: DailyAccountUsageEntity)
    @Query("DELETE FROM daily_account_usage WHERE dateKey < :oldestDateKey") suspend fun deleteOlderThanDate(oldestDateKey: String)
}
