package com.atmacanext.app.automation

/**
 * Tiny pure watchdog used by the runtime.  A heartbeat only counts as progress when a meaningful
 * signature changes; repeated accessibility events alone cannot keep a stuck task alive forever.
 */
class AutomationWatchdog(private val timeoutMs: Long) {
    private var lastSignature: String? = null
    private var lastProgressAt: Long = 0L

    fun reset(now: Long = System.currentTimeMillis()) {
        lastSignature = null
        lastProgressAt = now
    }

    fun observe(signature: String, now: Long = System.currentTimeMillis()): Boolean {
        if (lastSignature != signature) {
            lastSignature = signature
            lastProgressAt = now
            return true
        }
        if (lastProgressAt == 0L) lastProgressAt = now
        return false
    }

    fun isStuck(now: Long = System.currentTimeMillis()): Boolean =
        lastProgressAt > 0L && now - lastProgressAt >= timeoutMs

    fun ageMillis(now: Long = System.currentTimeMillis()): Long =
        if (lastProgressAt == 0L) 0L else (now - lastProgressAt).coerceAtLeast(0L)
}
