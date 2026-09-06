package com.atmacanext.app.domain.model

data class AccountHealthSnapshot(
    val accountId: String,
    val username: String,
    val score: Int,
    val grade: String,
    val verifiedActions: Int,
    val completedTasks: Int,
    val failedTasks: Int,
    val warningEvents: Int,
    val errorEvents: Int,
    val recoveryEvents: Int,
    val rateLimitEvents: Int,
    val successRate: Int,
    val status: String,
)
