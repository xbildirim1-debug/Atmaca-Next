package com.atmacanext.app.automation

/** Return from the previous task's X surface before opening the account drawer. */
internal object AccountVerificationReturnPolicy {
    enum class Decision { BACK, WAIT, RECOVER }

    fun decide(screen: XScreen, hasBack: Boolean, attempts: Int, elapsedMs: Long, sinceBackMs: Long): Decision {
        if (sinceBackMs < 1_000L) return Decision.WAIT
        if (attempts >= 6 || elapsedMs >= 12_000L) return Decision.RECOVER
        val childSurface = screen in setOf(XScreen.PROFILE, XScreen.TWEET_DETAIL,
            XScreen.FOLLOWERS_LIST, XScreen.FOLLOWING_LIST, XScreen.VERIFIED_FOLLOWERS_LIST,
            XScreen.ENGAGEMENT_LIST, XScreen.COMPOSER)
        // UNKNOWN is allowed only with a real visible X Back control.
        if (childSurface || (screen == XScreen.UNKNOWN && hasBack)) return Decision.BACK
        return Decision.WAIT
    }
}
