package com.atmacanext.app.automation

internal enum class UnfollowConfirmationDecision { CLICK, WAIT, PAUSE }

/** A still-visible confirmation during dismissal is expected, not a new blocking popup. */
internal object UnfollowConfirmationPolicy {
    fun decide(clicked: Boolean, elapsedMs: Long, timeoutMs: Long): UnfollowConfirmationDecision = when {
        !clicked -> UnfollowConfirmationDecision.CLICK
        elapsedMs < timeoutMs -> UnfollowConfirmationDecision.WAIT
        else -> UnfollowConfirmationDecision.PAUSE
    }
}
