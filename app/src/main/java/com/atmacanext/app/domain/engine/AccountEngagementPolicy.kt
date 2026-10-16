package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TaskType
import java.util.Locale
import java.util.UUID

/** One task per actor; each peer is a verified like + repost + bookmark bundle. */
object AccountEngagementPolicy {
    fun handle(raw: String) = raw.trim().removePrefix("@").lowercase(Locale.ROOT)
    fun peers(task: ScheduledTask): List<String> {
        if (task.type != TaskType.ACCOUNT_ENGAGEMENT) return emptyList()
        val values = task.contentPrompt.orEmpty().lineSequence().filter(String::isNotBlank).map(::handle).toList()
        if (values.isEmpty() || values.size > 9 || values.any { !it.matches(Regex("[a-z0-9_]{1,15}")) } ||
            values.distinct().size != values.size || handle(task.username) in values) return emptyList()
        return values
    }
    fun valid(task: ScheduledTask): Boolean = peers(task).let { it.isNotEmpty() && task.limit == it.size && task.repeatCount in 1..100 && task.intervalMinutes in 1..1440 }
    fun peer(task: ScheduledTask, progress: Int): String? = peers(task).let { if (it.isEmpty() || progress < 0) null else it[progress % it.size] }

    fun create(accounts: List<Account>, repeats: Int, interval: Int, group: String,
               originals: List<ScheduledTask> = emptyList()): List<ScheduledTask> {
        require(accounts.size in 2..10) { "En az 2, en fazla 10 hesap seç." }
        require(accounts.all { it.active && handle(it.username).matches(Regex("[a-z0-9_]{1,15}")) }) { "Seçili hesaplar aktif ve geçerli olmalı." }
        require(accounts.map { it.id }.distinct().size == accounts.size) { "Hesap kimlikleri farklı olmalı." }
        require(accounts.map { handle(it.username) }.distinct().size == accounts.size) { "Aynı X hesabı iki kez seçilemez." }
        require(repeats in 1..100 && interval in 1..1440) { "Döngü 1–100, aralık 1–1440 dakika olmalı." }
        return accounts.map { actor ->
            val config = accounts.filter { handle(it.username) != handle(actor.username) }.joinToString("\n") { handle(it.username) }
            val old = originals.firstOrNull { it.accountId == actor.id && it.type == TaskType.ACCOUNT_ENGAGEMENT }
            val progress = if (old?.contentPrompt == config) old.progress.coerceAtMost((accounts.size - 1) * repeats) else 0
            ScheduledTask(id = old?.id ?: UUID.randomUUID().toString(), accountId = actor.id, username = actor.username,
                time = group, type = TaskType.ACCOUNT_ENGAGEMENT, status = TaskStatus.QUEUED, progress = progress,
                limit = accounts.size - 1, repeatCount = repeats, intervalMinutes = interval, contentPrompt = config)
        }
    }
}
