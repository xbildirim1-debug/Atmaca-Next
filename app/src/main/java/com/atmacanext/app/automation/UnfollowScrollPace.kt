package com.atmacanext.app.automation

/** A list swipe must finish before "the page did not move" can mean the list ended. */
internal object UnfollowScrollPace {
    const val SETTLE_MS = 420L

    fun inFlight(armedAt: Long, now: Long): Boolean = armedAt > 0L && now - armedAt < SETTLE_MS
}
