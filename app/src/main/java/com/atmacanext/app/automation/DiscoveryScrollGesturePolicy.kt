package com.atmacanext.app.automation

/** Safe vertical path outside inline video/GIF/photo players and the compose button. */
internal object DiscoveryScrollGesturePolicy {
    const val X_RATIO = 0.06f
    const val FORWARD_START_Y_RATIO = 0.82f
    const val FORWARD_END_Y_RATIO = 0.28f
    const val BACKWARD_START_Y_RATIO = FORWARD_END_Y_RATIO
    const val BACKWARD_END_Y_RATIO = FORWARD_START_Y_RATIO
}
