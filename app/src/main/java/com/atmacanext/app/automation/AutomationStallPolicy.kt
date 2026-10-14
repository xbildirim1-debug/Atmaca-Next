package com.atmacanext.app.automation

import com.atmacanext.app.domain.model.TaskType

/**
 * Defines what counts as real automation progress for unattended self-recovery.
 * Accessibility event noise and changing status messages are deliberately excluded.
 */
internal object AutomationStallPolicy {
    const val TIMEOUT_MS = 10_000L

    fun shouldWatch(state: AutomationRuntimeState): Boolean =
        state.taskId != null &&
            state.flowStage != XFlowStage.WAIT_INTERVAL &&
            state.cycleWaitUntil == null &&
            state.status !in setOf(
                RuntimeStatus.IDLE,
                RuntimeStatus.PAUSED,
                RuntimeStatus.COOLDOWN,
                RuntimeStatus.COMPLETED,
                RuntimeStatus.FAILED,
            ) &&
            !composingTweet(state) &&
            !verifyingAccount(state) &&
            !verifiedFollowSetup(state)

    /** Tweet compose already has its own step timer. The 10s banner fired while the
     *  composer was opening or the post was already on its way out. */
    private fun composingTweet(state: AutomationRuntimeState): Boolean =
        (state.taskType == TaskType.TEXT_TWEET || state.taskType == TaskType.IMAGE_TWEET) &&
            state.flowStage in setOf(
                XFlowStage.OPEN_COMPOSER,
                XFlowStage.FILL_COMPOSER,
                XFlowStage.SUBMIT_COMPOSER,
            )

    /** Drawer, switcher and settle routinely take longer than 10s. Screen changes are
     *  not part of the signature, so every account looked stuck and showed Görev takıldı.
     *  The previous account's follower list is different: nothing there is account
     *  verification, and sitting on it must return to Atmaca. */
    private fun verifyingAccount(state: AutomationRuntimeState): Boolean {
        if (state.flowStage != XFlowStage.VERIFY_ACCOUNT) return false
        val stuckOnPreviousList = state.taskType == TaskType.VERIFIED_FOLLOW &&
            state.activeScreen in PREVIOUS_ACCOUNT_LISTS
        return !stuckOnPreviousList
    }

    /** These steps wait on X for their own 15–30s deadline. The 10s watchdog restarted
     *  a healthy verified-follow before that deadline and surfaced a false stall. */
    private fun verifiedFollowSetup(state: AutomationRuntimeState): Boolean =
        state.taskType == TaskType.VERIFIED_FOLLOW &&
            state.flowStage in setOf(
                XFlowStage.OPEN_VERIFIED_OWN_PROFILE,
                XFlowStage.OPEN_MY_FOLLOWERS,
                XFlowStage.LOCATE_SOURCE_ROW,
                XFlowStage.RETURN_VERIFIED_SOURCE,
                XFlowStage.OPEN_SOURCE_PROFILE,
            )

    fun signature(state: AutomationRuntimeState): String = listOf(
        state.taskId.orEmpty(),
        state.sessionId.orEmpty(),
        state.flowStage.name,
        state.verifiedCount.toString(),
        state.cycleIndex.toString(),
        state.detectedAccount.orEmpty(),
        state.accountVerified.toString(),
        state.listScrolls.toString(),
        state.depthUniqueUsers.toString(),
        state.sourceIndex.toString(),
        state.sourceHandle.orEmpty(),
        state.lastTarget.orEmpty(),
        state.unfollowRevertCount.toString(),
    ).joinToString("|")

    private val PREVIOUS_ACCOUNT_LISTS = setOf(
        XScreen.FOLLOWERS_LIST,
        XScreen.FOLLOWING_LIST,
        XScreen.VERIFIED_FOLLOWERS_LIST,
    )
}
