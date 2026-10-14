package com.atmacanext.app.automation

/** Gesture completion does not prove that X has finished updating its list. */
class NonFollowerViewportGate {
    enum class Decision { READY, WAIT, TIMED_OUT, CANCELLED }

    private var waiting = false
    private var startedAt = 0L
    private var stableSince = 0L
    private var lastSignature: String? = null
    private var gesturePending = false
    private var gestureCancelled = false
    private var releaseAt = 0L
    val pending: Boolean get() = waiting

    fun reset() {
        waiting = false
        lastSignature = null
        gesturePending = false
        gestureCancelled = false
    }

    fun afterScroll(now: Long, settleMs: Long = 0L) {
        waiting = true
        startedAt = now
        stableSince = now
        // Require fresh readings after release; the pre-gesture tree is insufficient.
        lastSignature = null
        gesturePending = false
        gestureCancelled = false
        releaseAt = now + settleMs - STABLE_MS
    }

    fun gestureStarted(now: Long) {
        afterScroll(now)
        gesturePending = true
    }

    fun gestureFinished(completed: Boolean, now: Long) {
        if (!waiting || !gesturePending) return
        gesturePending = false
        gestureCancelled = !completed
        releaseAt = now
        lastSignature = null
        stableSince = now
    }

    fun observe(signature: String?, now: Long): Decision {
        if (!waiting) return Decision.READY
        if (gestureCancelled) {
            reset()
            return Decision.CANCELLED
        }
        // Accessibility events can arrive during the drag. Stable old rows are
        // not proof of completion and must not start a cancelling second drag.
        if (gesturePending) return if (now - startedAt >= TIMEOUT_MS) Decision.TIMED_OUT else Decision.WAIT
        if (now - releaseAt < STABLE_MS) return Decision.WAIT
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
