package com.atmacanext.app.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val followers: String,
    val following: String,
    val engagement: String,
    val health: Int,
    val accent: String,
    val active: Boolean,
    @ColumnInfo(defaultValue = "0") val isCurrent: Boolean = false,
    val inactiveReason: String? = null,
    @ColumnInfo(defaultValue = "0") val unfollowRevertCount: Int = 0,
    @ColumnInfo(defaultValue = "''") val aiPersona: String = "",
    @ColumnInfo(defaultValue = "'tr'") val aiLanguage: String = "tr",
    @ColumnInfo(defaultValue = "'doğal'") val aiTone: String = "doğal",
    val lastSyncAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "scheduled_tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val username: String,
    val time: String,
    val type: String,
    val status: String,
    val progress: Int,
    val taskLimit: Int,
    @ColumnInfo(defaultValue = "1") val repeatCount: Int = 1,
    @ColumnInfo(defaultValue = "1") val intervalMinutes: Int = 1,
    val targetUrl: String? = null,
    val contentPrompt: String? = null,
    val contentText: String? = null,
    val mediaUri: String? = null,
    @ColumnInfo(defaultValue = "0") val useGemini: Boolean = false,
    val lastTarget: String? = null,
    val lastActionAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "runtime_checkpoint")
data class RuntimeCheckpointEntity(
    @PrimaryKey val id: Int = 1,
    val taskId: String?, val username: String?, val taskType: String?, val action: String?,
    val status: String, val verifiedCount: Int, val taskLimit: Int, val message: String,
    val lastTarget: String?, val lastActionAt: Long?, val detectedAccount: String?, val accountVerified: Boolean,
    val cooldownUntil: Long?, val rateLimitRetries: Int,
    @ColumnInfo(defaultValue = "1") val repeatCount: Int = 1,
    @ColumnInfo(defaultValue = "1") val perCycleLimit: Int = 1,
    @ColumnInfo(defaultValue = "1") val intervalMinutes: Int = 1,
    @ColumnInfo(defaultValue = "0") val cycleIndex: Int = 0,
    val cycleWaitUntil: Long? = null,
    @ColumnInfo(defaultValue = "0") val unfollowRevertCount: Int = 0,
    val savedAt: Long, val safeResumeRequired: Boolean,
) { companion object { const val SINGLETON_ID = 1 } }

@Entity(tableName = "target_accounts")
data class TargetAccountEntity(
    @PrimaryKey val id: String,
    val ownerAccountId: String,
    val handle: String,
    val active: Boolean = true,
    @ColumnInfo(defaultValue = "STANDARD") val kind: String = "STANDARD",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "automation_logs")
data class AutomationLogEntity(
    val timestamp: Long,
    val level: String,
    val category: String,
    val taskId: String?,
    val username: String?,
    val message: String,
    val details: String? = null,
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
)

@Entity(tableName = "queue_checkpoint")
data class QueueCheckpointEntity(
    @PrimaryKey val id: Int = 1, val sessionId: String?, val status: String, val currentIndex: Int,
    val message: String, val startedAt: Long?, val updatedAt: Long,
) { companion object { const val SINGLETON_ID = 1 } }

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey val taskId: String, val ordinal: Int, val accountId: String, val username: String,
    val taskType: String, val status: String, val note: String?, val updatedAt: Long,
)

@Entity(tableName = "daily_account_usage", primaryKeys = ["dateKey", "accountId", "actionType"])
data class DailyAccountUsageEntity(
    val dateKey: String, val accountId: String, val actionType: String, val verifiedCount: Int, val updatedAt: Long,
)
