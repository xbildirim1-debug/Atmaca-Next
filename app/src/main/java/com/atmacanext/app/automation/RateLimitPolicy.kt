package com.atmacanext.app.automation

/** Conservative policy: one automatic cooldown/retry per task, then human review. */
object RateLimitPolicy {
    const val DEFAULT_COOLDOWN_MS: Long = 30L * 60L * 1_000L
    const val MAX_AUTOMATIC_RETRIES: Int = 1

    fun canAutoRetry(previousRetries: Int): Boolean = previousRetries < MAX_AUTOMATIC_RETRIES

    fun remainingMillis(now: Long, cooldownUntil: Long?): Long =
        ((cooldownUntil ?: now) - now).coerceAtLeast(0L)
}
