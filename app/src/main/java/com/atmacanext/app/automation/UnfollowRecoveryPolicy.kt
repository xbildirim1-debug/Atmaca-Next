package com.atmacanext.app.automation

/** Only an unapplied action can release its attempt budget and restart the task. */
internal object UnfollowRecoveryPolicy {
    fun shouldRestart(confirmed: Boolean, followWasObserved: Boolean, follow: Boolean,
                      following: Boolean, elapsedMs: Long, stableMs: Long): Boolean =
        !confirmed && !followWasObserved && !follow && following && elapsedMs >= 10_000L && stableMs >= 1_000L
}
