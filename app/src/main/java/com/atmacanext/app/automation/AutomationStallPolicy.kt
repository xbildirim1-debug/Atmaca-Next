package com.atmacanext.app.automation

/**
 * Defines what counts as real automation progress for unattended self-recovery.
 * Accessibility event noise and changing status messages are deliberately excluded.
 */
internal object AutomationStallPolicy {
    const val TIMEOUT_MS = 20_000L

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
            )

    fun signature(state: AutomationRuntimeState): String = listOf(
        state.taskId.orEmpty(),
        state.sessionId.orEmpty(),
        state.flowStage.name,
        state.activeScreen.name,
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
}
