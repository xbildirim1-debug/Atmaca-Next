package com.atmacanext.app.automation

/**
 * Leave a finished account's tweet, profile, or list inside X.
 * A home intent only reorders the existing task and stays on that screen,
 * so it is never treated as a successful step.
 */
internal object DiscoveryHandoffPolicy {
    enum class Action { DONE, TAP_BOTTOM_HOME, TAP_BACK, WAIT, FAILED }

    fun action(
        screen: XScreen,
        bottomHomeVisible: Boolean,
        backVisible: Boolean,
        unknownSettled: Boolean,
    ): Action {
        if (screen == XScreen.HOME) return Action.DONE
        if (screen == XScreen.ACCOUNT_DRAWER || screen == XScreen.ACCOUNT_SWITCHER) return Action.DONE
        if (bottomHomeVisible) return Action.TAP_BOTTOM_HOME
        // A settling tree after a back is often UNKNOWN. Do not tap until it settles,
        // and never a global back: that finishes X onto the phone launcher.
        if (screen == XScreen.UNKNOWN && !unknownSettled) return Action.WAIT
        if (backVisible) return Action.TAP_BACK
        if (screen == XScreen.UNKNOWN) return Action.WAIT
        return Action.FAILED
    }
}
