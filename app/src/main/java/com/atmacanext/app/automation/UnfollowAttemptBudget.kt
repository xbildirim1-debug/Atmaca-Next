package com.atmacanext.app.automation

/** Independent of success detection: an unread result cannot create extra actions. */
internal object UnfollowAttemptBudget {
    fun mayIssue(issued: Int, totalLimit: Int, cycleIndex: Int, perCycleLimit: Int): Boolean =
        issued >= 0 && totalLimit > 0 && perCycleLimit > 0 && cycleIndex >= 0 &&
            issued < minOf(totalLimit.toLong(), (cycleIndex.toLong() + 1L) * perCycleLimit)
}
