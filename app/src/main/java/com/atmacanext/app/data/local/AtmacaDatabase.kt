package com.atmacanext.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AccountEntity::class, TaskEntity::class, RuntimeCheckpointEntity::class, AutomationLogEntity::class,
        QueueCheckpointEntity::class, QueueItemEntity::class, DailyAccountUsageEntity::class, TargetAccountEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AtmacaDatabase : RoomDatabase() {
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
                db.execSQL("DELETE FROM accounts WHERE id IN ('1','2','3','4','5') AND username LIKE '@atmaca_%'")
            }
        }
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE target_accounts ADD COLUMN kind TEXT NOT NULL DEFAULT 'STANDARD'")
            }
        }
        fun create(context: Context): AtmacaDatabase = Room.databaseBuilder(context.applicationContext, AtmacaDatabase::class.java, "atmaca_next.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }
}
