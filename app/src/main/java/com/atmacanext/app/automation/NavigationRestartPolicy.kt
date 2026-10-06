package com.atmacanext.app.automation

/** A navigation restart cannot discard an outstanding action or undo a user pause. */
internal object NavigationRestartPolicy {
    const val MAX_ATTEMPTS = 3

    fun deadlineReached(elapsedMs: Long): Boolean = elapsedMs >= AutomationStallPolicy.TIMEOUT_MS

    fun returnExpired(startedAt: Long, now: Long): Boolean = startedAt > 0L && deadlineReached(now - startedAt)

    fun mayPrepare(state: AutomationRuntimeState, pendingAction: Boolean, returning: Boolean): Boolean =
        AutomationStallPolicy.shouldWatch(state) && !pendingAction && !returning && state.quotePendingKey == null

    fun acceptsCallback(state: AutomationRuntimeState, taskId: String?, sessionId: String?, returning: Boolean,
                        expectedGeneration: Long = 0L, currentGeneration: Long = 0L): Boolean =
        returning && taskId != null && sessionId != null && state.taskId == taskId &&
            state.sessionId == sessionId && state.status == RuntimeStatus.RECOVERING && expectedGeneration == currentGeneration
}
