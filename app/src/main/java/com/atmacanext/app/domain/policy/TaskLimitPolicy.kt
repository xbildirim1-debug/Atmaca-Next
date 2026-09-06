package com.atmacanext.app.domain.policy

object TaskLimitPolicy {
    const val MAX_VERIFIED_ACTIONS = 35

    fun sanitizeLimit(requested: Int): Int = requested.coerceIn(1, MAX_VERIFIED_ACTIONS)

    fun sanitizeProgress(progress: Int, limit: Int): Int = progress.coerceIn(0, sanitizeLimit(limit))

    fun nextVerifiedCount(current: Int, limit: Int): Int =
        (sanitizeProgress(current, limit) + 1).coerceAtMost(sanitizeLimit(limit))
}
