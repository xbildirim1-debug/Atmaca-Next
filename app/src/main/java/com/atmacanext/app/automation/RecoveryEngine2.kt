package com.atmacanext.app.automation

/**
 * Checkpoint 11 recovery policy. It never clicks a follow/unfollow target while recovering.
 * Recovery always returns to a known X surface, re-verifies @handle, then lets the normal
 * state machine reopen the required list.
 */
enum class RecoveryCause {
    STUCK_NAVIGATION, WRONG_SCREEN, ACCOUNT_MISMATCH, SERVICE_RECONNECTED,
    X_PROCESS_RESTARTED, LIST_STALLED, UNKNOWN_DIALOG
}

enum class RecoveryAction {
    RELAUNCH_X, GO_BACK, REVERIFY_ACCOUNT, REOPEN_REQUIRED_LIST, PAUSE
}

data class RecoveryDecision(val action: RecoveryAction, val reason: String, val attempt: Int)

class RecoveryEngine2(private val maxAttempts: Int = 3) {
    private var attempts = 0
    private var lastCause: RecoveryCause? = null

    fun reset() { attempts = 0; lastCause = null }

    fun decide(cause: RecoveryCause, screen: XScreen, accountVerified: Boolean): RecoveryDecision {
        if (cause != lastCause) attempts = 0
        lastCause = cause
        attempts++
        if (attempts > maxAttempts) return RecoveryDecision(RecoveryAction.PAUSE, "Recovery sınırı aşıldı: $cause", attempts)
        val action = when {
            cause == RecoveryCause.SERVICE_RECONNECTED || cause == RecoveryCause.X_PROCESS_RESTARTED -> RecoveryAction.RELAUNCH_X
            !accountVerified -> RecoveryAction.REVERIFY_ACCOUNT
            screen == XScreen.UNKNOWN -> RecoveryAction.RELAUNCH_X
            cause == RecoveryCause.UNKNOWN_DIALOG -> RecoveryAction.GO_BACK
            else -> RecoveryAction.REOPEN_REQUIRED_LIST
        }
        return RecoveryDecision(action, "Recovery $attempts/$maxAttempts: $cause", attempts)
    }
}
