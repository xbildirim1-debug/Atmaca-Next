package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TargetAccount
import com.atmacanext.app.domain.model.TargetKind
import com.atmacanext.app.domain.model.TaskType
import java.util.Locale

/** Snapshot each account's own quote targets; never copy another account's reply checkpoint. */
internal object QuoteTaskSetupPolicy {
    const val MAX_POSTS_PER_TARGET = 20

    fun activeHandles(accountId: String, targets: List<TargetAccount>): List<String> = targets.asSequence()
        .filter { it.ownerAccountId == accountId && it.kind == TargetKind.QUOTE && it.active }
        .map { it.handle.trim().removePrefix("@").lowercase(Locale.ROOT) }
        .filter { it.matches(Regex("[a-z0-9_]{1,15}")) }
        .distinct().take(5).toList()

    fun bindAccount(task: ScheduledTask, targets: List<TargetAccount>, existing: ScheduledTask? = null): ScheduledTask {
        if (task.type != TaskType.COMMENT_QUOTE_TARGETS) return task
        require(task.limit in 1..MAX_POSTS_PER_TARGET) { "Yorum limiti hedef başına 1–20 olmalı" }
        val owned = existing?.takeIf { it.accountId == task.accountId && it.type == task.type }
        val handles = owned?.quoteTargetHandles?.takeIf { it.isNotEmpty() } ?: activeHandles(task.accountId, targets)
        require(handles.isNotEmpty()) { "${task.username} için Hesaplar > Alıntı Hedefleri alanına hedef ekle" }
        val bound = task.copy(quoteTargets = handles.joinToString("\n"),
            quotePostedKeys = owned?.quotePostedKeys, quotePendingKey = owned?.quotePendingKey)
        return bound.copy(progress = (owned?.progress ?: 0).coerceIn(0, bound.totalLimit))
    }
}
