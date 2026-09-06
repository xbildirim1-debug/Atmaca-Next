package com.atmacanext.app.automation

/** Pure helper retained for unit tests/documentation; the runtime owns the full X flow stages. */
object NavigationPlanner {
    fun decide(
        screen: XScreen,
        action: AutomationAction,
        targetUsername: String,
        identity: OwnProfileIdentity,
        accountVerified: Boolean,
        listLoaded: Boolean,
    ): NavigationDecision {
        if (screen == XScreen.PROFILE && identity.isOwnProfile && identity.expectedAccountVisible) {
            return NavigationDecision(
                when (action) {
                    AutomationAction.FOLLOW_VERIFIED -> NavigationCommand.OPEN_FOLLOWERS
                    AutomationAction.UNFOLLOW -> NavigationCommand.OPEN_FOLLOWING
                    else -> NavigationCommand.OPEN_X_HOME
                },
                "$targetUsername X hesabı doğrulandı",
            )
        }
        if (screen == XScreen.ACCOUNT_SWITCHER) {
            return NavigationDecision(NavigationCommand.SELECT_TARGET_ACCOUNT, "$targetUsername X hesap seçicide aranıyor")
        }
        if (screen == XScreen.ACCOUNT_DRAWER) {
            return NavigationDecision(NavigationCommand.OPEN_PROFILE_FROM_DRAWER, "Aktif X profili açılıyor")
        }
        if (screen == XScreen.HOME) {
            return NavigationDecision(NavigationCommand.OPEN_ACCOUNT_DRAWER, "X gezinme/hesap menüsü açılıyor")
        }
        if (accountVerified && listLoaded) {
            val expected = if (action == AutomationAction.UNFOLLOW) XScreen.FOLLOWING_LIST else XScreen.VERIFIED_FOLLOWERS_LIST
            if (screen == expected) return NavigationDecision(NavigationCommand.READY, "X işlem listesi doğrulandı")
        }
        return NavigationDecision(NavigationCommand.OPEN_X_HOME, "X ana sayfasından güvenli yeniden doğrulama")
    }
}
