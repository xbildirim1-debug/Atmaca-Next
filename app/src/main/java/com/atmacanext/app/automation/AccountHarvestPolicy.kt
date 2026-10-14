package com.atmacanext.app.automation

/**
 * Collect every open session on the switcher, whether there are 2 or 10.
 * Count is not a stop signal until the hard cap. The list is walked to the
 * top, then to the bottom, and only then considered complete.
 */
internal object AccountHarvestPolicy {
    const val MAX_ACCOUNTS = 10
    const val EDGE_SCROLLS = 3

    enum class Step { WAIT, SCROLL, REVERSE, FINISH }

    fun step(found: Int, pageStableReads: Int, unchangedScrolls: Int, seekingTop: Boolean): Step = when {
        found >= MAX_ACCOUNTS -> Step.FINISH
        pageStableReads < 1 -> Step.WAIT
        unchangedScrolls >= EDGE_SCROLLS && seekingTop -> Step.REVERSE
        unchangedScrolls >= EDGE_SCROLLS -> Step.FINISH
        else -> Step.SCROLL
    }
}
