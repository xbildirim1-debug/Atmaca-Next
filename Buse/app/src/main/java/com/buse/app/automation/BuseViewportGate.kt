package com.buse.app.automation

/** Gesture completion does not prove that X has finished updating its list. */
class BuseViewportGate {
    enum class Decision { READY, WAIT, TIMED_OUT }

    private var waiting = false
    private var startedAt = 0L
    private var stableSince = 0L
    private var lastSignature: String? = null
    val pending: Boolean get() = waiting

    fun reset() {
        waiting = false
        lastSignature = null
    }

    fun afterScroll(now: Long) {
        waiting = true
        startedAt = now
        stableSince = now
        // Require fresh readings after release; the pre-gesture tree is insufficient.
        lastSignature = null
    }

    fun observe(signature: String?, now: Long): Decision {
        if (!waiting) return Decision.READY
        if (signature.isNullOrBlank()) {
            lastSignature = null
            stableSince = now
        } else if (signature != lastSignature) {
            lastSignature = signature
            stableSince = now
        } else if (now - stableSince >= STABLE_MS) {
            waiting = false
            return Decision.READY
        }
        return if (now - startedAt >= TIMEOUT_MS) Decision.TIMED_OUT else Decision.WAIT
    }

    companion object {
        const val POLL_MS = 16L
        const val STABLE_MS = 32L
        const val TIMEOUT_MS = 2_000L
    }
}
