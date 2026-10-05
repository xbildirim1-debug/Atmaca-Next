package com.atmacanext.app.automation

/** Each target receives its own limit before the next target or cycle begins. */
internal object QuoteTargetProgressPolicy {
    fun perCycle(perTarget: Int, targets: Int): Int = perTarget.coerceIn(1, 20) * targets.coerceIn(1, 5)
    fun targetIndex(progress: Int, perTarget: Int, targets: Int): Int =
        (progress.coerceAtLeast(0) % perCycle(perTarget, targets)) / perTarget.coerceIn(1, 20)
    fun targetReached(progress: Int, perTarget: Int): Boolean =
        progress > 0 && progress % perTarget.coerceIn(1, 20) == 0
    fun completedTarget(progress: Int, cycleStart: Int, targetIndex: Int, perTarget: Int): Boolean =
        progress - cycleStart >= (targetIndex + 1) * perTarget.coerceIn(1, 20)
}
