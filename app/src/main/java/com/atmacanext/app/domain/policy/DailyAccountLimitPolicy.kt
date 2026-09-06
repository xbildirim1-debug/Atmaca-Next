package com.atmacanext.app.domain.policy

data class DailyLimitDecision(
    val allowed: Boolean,
    val remainingToday: Int,
    val taskRemaining: Int,
)

object DailyAccountLimitPolicy {
    fun evaluate(
        taskProgress: Int,
        taskLimit: Int,
        usedToday: Int,
        dailyLimit: Int,
    ): DailyLimitDecision {
        val safeTaskLimit = taskLimit.coerceAtLeast(0)
        val safeProgress = taskProgress.coerceIn(0, safeTaskLimit)
        val safeDailyLimit = dailyLimit.coerceAtLeast(0)
        val safeUsed = usedToday.coerceIn(0, Int.MAX_VALUE)
        val remainingToday = (safeDailyLimit - safeUsed).coerceAtLeast(0)
        val taskRemaining = (safeTaskLimit - safeProgress).coerceAtLeast(0)
        return DailyLimitDecision(
            allowed = taskRemaining > 0 && taskRemaining <= remainingToday,
            remainingToday = remainingToday,
            taskRemaining = taskRemaining,
        )
    }
}
