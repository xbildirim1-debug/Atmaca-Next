package com.buse.app.automation

internal object AccountSelectionReturnPolicy {
    enum class Decision { WAIT, OPEN_DRAWER, VERIFY_DRAWER, CLOSE_SWITCHER, RETURN }
    fun decide(screen: XScreen, elapsedMs: Long, settleMs: Long): Decision = when {
        screen == XScreen.HOME -> Decision.OPEN_DRAWER
        screen == XScreen.ACCOUNT_DRAWER -> Decision.VERIFY_DRAWER
        elapsedMs < settleMs.coerceIn(100L, 15_000L) -> Decision.WAIT
        screen == XScreen.ACCOUNT_SWITCHER -> Decision.CLOSE_SWITCHER
        else -> Decision.RETURN
    }

    /** Only a current proven sheet can receive the sheet's global dismiss action. */
    fun mayDismiss(currentPackage: String?, freshScreen: XScreen): Boolean =
        currentPackage == BuseAccessibilityService.X_PACKAGE && freshScreen == XScreen.ACCOUNT_SWITCHER
}
