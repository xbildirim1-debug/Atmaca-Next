package com.atmacanext.app.automation

/**
 * Pure account-switch decision helper.
 * Follow/Unfollow is permitted only after the target @handle is seen on
 * a verified own-profile surface after the switch settle phase.
 */
object XAccountSwitchGuard {
    enum class Result {
        VERIFIED,
        NEED_SWITCH,
        WRONG_ACCOUNT,
        UNKNOWN,
    }

    fun evaluate(
        targetUsername: String,
        detectedUsername: String?,
        screen: XScreen,
        ownProfileEvidence: Boolean,
    ): Result {
        val target = XIdentityDetector.normalizeUsername(targetUsername)
        val detected = detectedUsername?.let(XIdentityDetector::normalizeUsername)

        if (target.isBlank()) return Result.UNKNOWN
        if (screen == XScreen.ACCOUNT_SWITCHER) return Result.NEED_SWITCH
        if (screen != XScreen.PROFILE || !ownProfileEvidence) return Result.UNKNOWN
        if (detected.isNullOrBlank()) return Result.UNKNOWN
        return if (detected == target) Result.VERIFIED else Result.WRONG_ACCOUNT
    }

    fun mayPerformMutation(result: Result): Boolean = result == Result.VERIFIED
}
