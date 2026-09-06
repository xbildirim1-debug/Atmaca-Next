package com.atmacanext.app.domain.engine

import com.atmacanext.app.data.local.AutomationLogEntity
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.AccountHealthSnapshot
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskStatus

object AccountHealthCalculator {
    fun calculate(account: Account, tasks: List<ScheduledTask>, logs: List<AutomationLogEntity>): AccountHealthSnapshot {
        val ownTasks = tasks.filter { it.accountId == account.id || it.username.equals(account.username, true) }
        val ownLogs = logs.filter { it.username?.equals(account.username, true) == true }
        val completed = ownTasks.count { it.status == TaskStatus.COMPLETED }
        val failed = ownTasks.count { it.status == TaskStatus.FAILED }
        val verified = ownTasks.sumOf { it.progress }
        val warnings = ownLogs.count { it.level.equals("WARN", true) }
        val errors = ownLogs.count { it.level.equals("ERROR", true) }
        val recoveries = ownLogs.count { it.category.contains("RECOVER", true) || it.message.contains("recovery", true) || it.message.contains("kurtar", true) }
        val rateLimits = ownLogs.count { it.category.contains("RATE", true) || it.message.contains("rate", true) || it.message.contains("cooldown", true) }
        val terminal = completed + failed
        val successRate = if (terminal == 0) 100 else ((completed * 100.0) / terminal).toInt().coerceIn(0, 100)
        val penalty = failed * 18 + errors * 8 + rateLimits * 7 + recoveries * 3 + warnings * 2
        val evidenceBonus = (verified / 10).coerceAtMost(8) + completed.coerceAtMost(4)
        val score = (100 - penalty + evidenceBonus).coerceIn(0, 100)
        val grade = when { score >= 90 -> "A"; score >= 80 -> "B"; score >= 65 -> "C"; score >= 50 -> "D"; else -> "E" }
        val status = when { !account.active -> "PASİF"; score < 50 -> "KRİTİK"; score < 65 -> "RİSKLİ"; score < 80 -> "İZLE"; else -> "SAĞLIKLI" }
        return AccountHealthSnapshot(account.id, account.username, score, grade, verified, completed, failed, warnings, errors, recoveries, rateLimits, successRate, status)
    }
}
