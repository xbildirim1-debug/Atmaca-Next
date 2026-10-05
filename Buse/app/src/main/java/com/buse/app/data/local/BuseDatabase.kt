package com.buse.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AccountEntity::class, TaskEntity::class, RuntimeCheckpointEntity::class, AutomationLogEntity::class,
        QueueCheckpointEntity::class, QueueItemEntity::class, DailyAccountUsageEntity::class, TargetAccountEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class BuseDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun taskDao(): TaskDao
    abstract fun runtimeCheckpointDao(): RuntimeCheckpointDao
    abstract fun automationLogDao(): AutomationLogDao
    abstract fun queueCheckpointDao(): QueueCheckpointDao
    abstract fun queueItemDao(): QueueItemDao
    abstract fun dailyAccountUsageDao(): DailyAccountUsageDao
    abstract fun targetAccountDao(): TargetAccountDao

    companion object {
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS queue_checkpoint (id INTEGER NOT NULL, sessionId TEXT, status TEXT NOT NULL, currentIndex INTEGER NOT NULL, message TEXT NOT NULL, startedAt INTEGER, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS queue_items (taskId TEXT NOT NULL, ordinal INTEGER NOT NULL, accountId TEXT NOT NULL, username TEXT NOT NULL, taskType TEXT NOT NULL, status TEXT NOT NULL, note TEXT, updatedAt INTEGER NOT NULL, PRIMARY KEY(taskId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_account_usage (dateKey TEXT NOT NULL, accountId TEXT NOT NULL, actionType TEXT NOT NULL, verifiedCount INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(dateKey, accountId, actionType))")
            }
        }
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS task_schedules (taskId TEXT NOT NULL, enabled INTEGER NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, nextRunAt INTEGER, lastTriggeredAt INTEGER, status TEXT NOT NULL, message TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(taskId))")
            }
        }
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN isCurrent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN inactiveReason TEXT")
                db.execSQL("ALTER TABLE accounts ADD COLUMN unfollowRevertCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN aiPersona TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE accounts ADD COLUMN aiLanguage TEXT NOT NULL DEFAULT 'tr'")
                db.execSQL("ALTER TABLE accounts ADD COLUMN aiTone TEXT NOT NULL DEFAULT 'doğal'")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN repeatCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN intervalMinutes INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN targetUrl TEXT")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN contentPrompt TEXT")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN contentText TEXT")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN mediaUri TEXT")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN useGemini INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE runtime_checkpoint ADD COLUMN repeatCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE runtime_checkpoint ADD COLUMN perCycleLimit INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE runtime_checkpoint ADD COLUMN intervalMinutes INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE runtime_checkpoint ADD COLUMN cycleIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE runtime_checkpoint ADD COLUMN cycleWaitUntil INTEGER")
                db.execSQL("ALTER TABLE runtime_checkpoint ADD COLUMN unfollowRevertCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS target_accounts (id TEXT NOT NULL, ownerAccountId TEXT NOT NULL, handle TEXT NOT NULL, active INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("DROP TABLE IF EXISTS task_schedules")
                db.execSQL("DELETE FROM scheduled_tasks WHERE id IN ('t1','t2','t3','t4','t5','t6','t7')")
                db.execSQL("DELETE FROM accounts WHERE id IN ('1','2','3','4','5') AND username LIKE '@buse_%'")
            }
        }
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE target_accounts ADD COLUMN kind TEXT NOT NULL DEFAULT 'STANDARD'")
            }
        }
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN quoteTargets TEXT")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN quotePostedKeys TEXT")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN quotePendingKey TEXT")
                // 26.50's mapper wrote QUOTE rows as STANDARD. The original UUID
                // proves which kind was intended, without touching standard targets.
                db.query("SELECT id, ownerAccountId, handle FROM target_accounts WHERE kind = 'STANDARD'").use { cursor ->
                    while (cursor.moveToNext()) {
                        val rowId = cursor.getString(0)
                        val seed = cursor.getString(1) + ":QUOTE:" + cursor.getString(2)
                        if (java.util.UUID.nameUUIDFromBytes(seed.toByteArray()).toString() == rowId) {
                            db.execSQL("UPDATE target_accounts SET kind = 'QUOTE' WHERE id = ?", arrayOf(rowId))
                        }
                    }
                }
                // Normalize the SQL default for databases created by Room v5
                // (DEFAULT STANDARD) and by MIGRATION_4_5 (DEFAULT 'STANDARD').
                db.execSQL("CREATE TABLE target_accounts_v6 (id TEXT NOT NULL, ownerAccountId TEXT NOT NULL, handle TEXT NOT NULL, active INTEGER NOT NULL, updatedAt INTEGER NOT NULL, kind TEXT NOT NULL DEFAULT 'STANDARD', PRIMARY KEY(id))")
                db.execSQL("INSERT INTO target_accounts_v6 (id, ownerAccountId, handle, active, updatedAt, kind) SELECT id, ownerAccountId, handle, active, updatedAt, kind FROM target_accounts")
                db.execSQL("DROP TABLE target_accounts")
                db.execSQL("ALTER TABLE target_accounts_v6 RENAME TO target_accounts")
            }
        }
        fun create(context: Context): BuseDatabase = Room.databaseBuilder(context.applicationContext, BuseDatabase::class.java, "buse_next.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
    }
}
