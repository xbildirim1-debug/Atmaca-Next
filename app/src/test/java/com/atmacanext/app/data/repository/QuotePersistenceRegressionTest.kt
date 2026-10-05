package com.atmacanext.app.data.repository

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.atmacanext.app.data.local.AtmacaDatabase
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TargetAccount
import com.atmacanext.app.domain.model.TargetKind
import com.atmacanext.app.domain.model.TaskType
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.UUID

class QuotePersistenceRegressionTest {
    @Test fun quoteAndStandardTargetKindsSurviveDatabaseMapping() {
        for (kind in TargetKind.entries) {
            val target = TargetAccount("id", "owner", "hedef", kind = kind)
            assertEquals(kind.name, target.toEntity().kind)
            assertEquals(target, target.toEntity().toDomain())
        }
    }
    @Test fun taskSnapshotAndReplyCheckpointSurviveMapping() {
        val task = ScheduledTask("q", "1", "hesabim", type = TaskType.COMMENT_QUOTE_TARGETS,
            limit = 2, repeatCount = 3, quoteTargets = "hedef\nhedef2", quotePostedKeys = "key", quotePendingKey = "uncertain")
        assertEquals(12, task.totalLimit)
        assertEquals(task, task.toEntity().toDomain())
    }
    @Test fun migrationRepairsOnlyRowsWithTheOriginalQuoteUuid() {
        val owner = "account"
        val quoteId = UUID.nameUUIDFromBytes((owner + ":QUOTE:hedef").toByteArray()).toString()
        val standardId = UUID.nameUUIDFromBytes((owner + ":STANDARD:hedef").toByteArray()).toString()
        val rows = listOf(listOf(quoteId, owner, "hedef"), listOf(standardId, owner, "hedef"), listOf("user-id", owner, "other"))
        var position = -1
        val cursor = Proxy.newProxyInstance(Cursor::class.java.classLoader, arrayOf(Cursor::class.java)) { _, method, args ->
            when (method.name) {
                "moveToNext" -> ++position < rows.size
                "getString" -> rows[position][args!![0] as Int]
                "close" -> null
                else -> null
            }
        } as Cursor
        val repaired = mutableListOf<String>()
        val statements = mutableListOf<String>()
        val db = Proxy.newProxyInstance(SupportSQLiteDatabase::class.java.classLoader, arrayOf(SupportSQLiteDatabase::class.java)) { _, method, args ->
            when (method.name) {
                "query" -> cursor
                "execSQL" -> {
                    val sql = args!![0] as String
                    statements += sql
                    if (sql.startsWith("UPDATE target_accounts")) repaired += (args[1] as Array<*>)[0] as String
                    null
                }
                else -> null
            }
        } as SupportSQLiteDatabase
        AtmacaDatabase.MIGRATION_5_6.migrate(db)
        assertEquals(listOf(quoteId), repaired)
        assertEquals(3, statements.count { it.startsWith("ALTER TABLE scheduled_tasks") })
    }
}
