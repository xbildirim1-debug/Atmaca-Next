package com.atmacanext.app.automation

/** Fast polling may observe an unchanged drawer/sheet while the first tap animates. */
internal class AccountNavigationGate {
    private var issuedScreen: XScreen? = null
    private var issuedAt = 0L
    private var expectedScreen: XScreen? = null
    fun issued(screen: XScreen, now: Long, expected: XScreen? = null) { issuedScreen = screen; issuedAt = now; expectedScreen = expected }
    fun wait(screen: XScreen, now: Long): Boolean {
        expectedScreen?.let { expected ->
            if (screen == expected) { clear(); return false }
            if (issuedAt > 0L && now - issuedAt < 3_000L) return true
            clear(); return false
        }
        if (screen == XScreen.UNKNOWN && issuedScreen != null) return issuedAt > 0L && now - issuedAt < 1_500L
        if (screen != issuedScreen) { clear(); return false }
        return issuedAt > 0L && now - issuedAt < 1_500L
    }
    fun clear() { issuedScreen = null; issuedAt = 0L; expectedScreen = null }
}
