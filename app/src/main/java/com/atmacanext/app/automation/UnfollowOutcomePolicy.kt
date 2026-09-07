package com.atmacanext.app.automation

/**
 * Converts X's visible post-condition into a deterministic unfollow result.
 * The runtime owns the two-step daily-limit proof: it must first observe
 * Follow, then a return to Following. This policy never turns an unchanged
 * Following button into a daily-limit success by itself.
 */
enum class UnfollowOutcome {
    WAIT,
    SUCCESS,
    DAILY_LIMIT,
    UNCONFIRMED,
    UNKNOWN,
}

object UnfollowOutcomePolicy {
    fun evaluate(
        confirmationClicked: Boolean,
        elapsedMs: Long,
        rowShowsFollow: Boolean,
        rowStillFollowing: Boolean,
        timeoutMs: Long,
    ): UnfollowOutcome = when {
        confirmationClicked && rowShowsFollow && !rowStillFollowing -> UnfollowOutcome.SUCCESS
        !confirmationClicked && elapsedMs >= timeoutMs -> UnfollowOutcome.UNCONFIRMED
        confirmationClicked && elapsedMs >= timeoutMs && rowStillFollowing -> UnfollowOutcome.UNKNOWN
        confirmationClicked && elapsedMs >= timeoutMs * 2L -> UnfollowOutcome.UNKNOWN
        else -> UnfollowOutcome.WAIT
    }
}
