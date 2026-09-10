package com.atmacanext.app.automation

/**
 * Drain every still-visible, unprocessed commenter before advancing the thread.
 * Scrolling is a pagination step, never a side effect of returning from one profile.
 *
 * X/Compose can briefly publish an incomplete accessibility tree just after Back.
 * One empty snapshot is therefore not proof that the viewport is exhausted. The
 * post-return check arms a short guard; the gesture layer may advance only after
 * the same viewport has remained empty for several observations.
 */
internal object CommenterViewportPolicy {
    private const val REQUIRED_STABLE_EMPTY_OBSERVATIONS = 3
    private const val GUARD_TTL_MS = 4_000L

    private data class ScrollGuard(
        val excludedHandles: Set<String>,
        val armedAt: Long,
        var viewportSignature: String = "",
        var emptyObservations: Int = 0,
    )

    private var scrollGuard: ScrollGuard? = null

    fun nextVisible(visibleAuthors: List<String>, excludedHandles: Set<String>): String? =
        visibleAuthors.firstOrNull { it !in excludedHandles }

    /**
     * Called immediately after returning from one commenter. Never scroll from
     * this single snapshot. If a candidate is already visible, normal processing
     * consumes it. Otherwise arm the stable-empty guard for the gesture fallback.
     */
    @Synchronized
    fun shouldScroll(
        visibleAuthors: List<String>,
        excludedHandles: Set<String>,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (nextVisible(visibleAuthors, excludedHandles) != null) {
            scrollGuard = null
            return false
        }
        scrollGuard = ScrollGuard(excludedHandles.toSet(), nowMillis)
        return false
    }

    /**
     * Returns true only when an armed post-return viewport is stably exhausted.
     * With no active guard this is transparent, so profile/retweeter discovery is
     * unaffected. A changed accessibility signature restarts confirmation.
     */
    @Synchronized
    fun allowScrollAfterStableEmpty(
        visibleAuthors: List<String>,
        viewportSignature: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        val guard = scrollGuard ?: return true
        if (nowMillis - guard.armedAt >= GUARD_TTL_MS) {
            scrollGuard = null
            return true
        }
        if (nextVisible(visibleAuthors, guard.excludedHandles) != null) {
            scrollGuard = null
            return false
        }
        if (viewportSignature.isBlank()) {
            guard.emptyObservations = 0
            return false
        }
        if (guard.viewportSignature != viewportSignature) {
            guard.viewportSignature = viewportSignature
            guard.emptyObservations = 1
            return false
        }
        guard.emptyObservations++
        if (guard.emptyObservations < REQUIRED_STABLE_EMPTY_OBSERVATIONS) return false
        scrollGuard = null
        return true
    }

    @Synchronized
    fun clearScrollGuard() {
        scrollGuard = null
    }
}
