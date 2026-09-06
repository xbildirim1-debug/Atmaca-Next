package com.atmacanext.app.automation

enum class NavigationCommand {
    OPEN_X_HOME,
    OPEN_ACCOUNT_DRAWER,
    OPEN_PROFILE_FROM_DRAWER,
    OPEN_ACCOUNT_SWITCHER,
    SELECT_TARGET_ACCOUNT,
    OPEN_FOLLOWERS,
    OPEN_FOLLOWING,
    OPEN_VERIFIED_FOLLOWERS,
    BACK,
    WAIT_FOR_UI,
    READY,
}

data class OwnProfileIdentity(
    val isOwnProfile: Boolean,
    val expectedAccountVisible: Boolean,
    val detectedUsername: String? = null,
)

data class NavigationDecision(
    val command: NavigationCommand,
    val message: String,
)

data class PendingNavigation(
    val command: NavigationCommand,
    val startedAt: Long = System.currentTimeMillis(),
)
