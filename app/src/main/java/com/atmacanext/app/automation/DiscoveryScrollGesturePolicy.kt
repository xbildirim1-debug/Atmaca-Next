package com.atmacanext.app.automation

/** Safe vertical path outside inline video/GIF/photo players and the compose button. */
internal object DiscoveryScrollGesturePolicy {
    private val X_RATIOS = floatArrayOf(0.06f, 0.12f)
    const val FORWARD_START_Y_RATIO = 0.88f
    const val FORWARD_END_Y_RATIO = 0.20f
    const val BACKWARD_START_Y_RATIO = FORWARD_END_Y_RATIO
    const val BACKWARD_END_Y_RATIO = FORWARD_START_Y_RATIO

    /** Retry in a second media-free gutter when X accepts but swallows a swipe. */
    fun xRatio(recoveryAttempt: Int): Float = X_RATIOS[recoveryAttempt.coerceAtLeast(0) % X_RATIOS.size]
}
