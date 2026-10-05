package com.buse.app.automation

/** Return through the current X back stack without leaving a reached target. */
internal object DiscoveryReturnPolicy {
    enum class Decision { SCAN, BACK, WAIT, PAUSE }

    fun decide(targetVisible: Boolean, screen: XScreen, backAttempts: Int,
               elapsedMs: Long, sinceLastBackMs: Long): Decision {
        if (targetVisible) return Decision.SCAN
        if (elapsedMs >= 8_000L) return Decision.PAUSE
        if (backAttempts >= 3) return if (sinceLastBackMs < 1_000L) Decision.WAIT else Decision.PAUSE
        if (screen in setOf(XScreen.TWEET_DETAIL, XScreen.ENGAGEMENT_LIST) &&
            (backAttempts == 0 || sinceLastBackMs >= 1_000L)) return Decision.BACK
        return Decision.WAIT
    }
}
