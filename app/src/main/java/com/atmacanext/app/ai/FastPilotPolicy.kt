package com.atmacanext.app.ai

import com.atmacanext.app.automation.AutomationRuntimeState
import com.atmacanext.app.automation.PopupType
import com.atmacanext.app.automation.RuntimeStatus
import com.atmacanext.app.automation.XFlowStage
import com.atmacanext.app.automation.XScreen
import com.atmacanext.app.domain.model.TaskType

enum class PilotMove { NONE, REFRESH, DISMISS, BACK, OPEN_FOLLOWING }
data class PilotScreenObservation(
    val runtime: AutomationRuntimeState,
    val signature: String,
    val fingerprint: String,
    val observedAt: Long,
    val popup: PopupType = PopupType.NONE,
    val pendingAction: Boolean = false,
    val settling: Boolean = false,
    val moving: Boolean = false,
    val loading: Boolean = false,
    val ownProfile: Boolean = false,
    val labels: List<String> = emptyList(),
)

/** The model can suggest only a navigation operation permitted by the current verified task. */
object FastPilotPolicy {
    const val PULSE_MS = 1_000L
    const val FRESH_MS = 2_500L
    fun allowed(observation: PilotScreenObservation, now: Long): Set<PilotMove> {
        val s = observation.runtime
        if (s.taskId == null || s.sessionId == null || !s.accountVerified ||
            s.status in setOf(RuntimeStatus.IDLE, RuntimeStatus.PAUSED, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED, RuntimeStatus.COOLDOWN) ||
            s.flowStage == XFlowStage.WAIT_INTERVAL || s.cycleWaitUntil != null ||
            observation.popup in setOf(PopupType.RATE_LIMIT, PopupType.PERMISSION_PROMPT, PopupType.ACTION_CONFIRMATION) ||
            s.flowStage in setOf(XFlowStage.OPEN_COMPOSER, XFlowStage.FILL_COMPOSER, XFlowStage.SUBMIT_COMPOSER) ||
            observation.pendingAction || s.quotePendingKey != null || observation.settling || observation.moving ||
            now - observation.observedAt !in 0L..FRESH_MS) return emptySet()
        val moves = mutableSetOf(PilotMove.REFRESH)
        if (observation.popup in setOf(PopupType.NOTIFICATION_PROMPT, PopupType.CONTACT_SYNC,
                PopupType.PROMOTION, PopupType.GENERIC_ERROR, PopupType.RETRYABLE_ERROR, PopupType.UNKNOWN_DIALOG)) {
            moves += PilotMove.DISMISS
        }
        val needsFollowing = s.taskType == TaskType.UNFOLLOW_NON_FOLLOWERS &&
            s.flowStage in setOf(XFlowStage.SEEK_UNFOLLOW_DEPTH, XFlowStage.PROCESS_UNFOLLOW)
        if (needsFollowing && observation.popup == PopupType.NONE && !observation.loading) {
            if (s.activeScreen in setOf(XScreen.PROFILE, XScreen.HOME) && observation.ownProfile) moves += PilotMove.OPEN_FOLLOWING
            if (s.activeScreen == XScreen.TWEET_DETAIL) moves += PilotMove.BACK
        }
        // HOME, account sheets, a healthy following list and unknown screens never acquire Back.
        return moves
    }

    fun localMove(observation: PilotScreenObservation, unchangedMs: Long, now: Long): PilotMove {
        val allowed = allowed(observation, now)
        if (PilotMove.DISMISS in allowed) return PilotMove.DISMISS
        if (unchangedMs < 1_000L) return PilotMove.NONE
        return when {
            PilotMove.OPEN_FOLLOWING in allowed -> PilotMove.OPEN_FOLLOWING
            PilotMove.BACK in allowed -> PilotMove.BACK
            unchangedMs >= 2_000L && PilotMove.REFRESH in allowed -> PilotMove.REFRESH
            else -> PilotMove.NONE
        }
    }

    fun matches(expected: PilotScreenObservation, fresh: PilotScreenObservation): Boolean =
        expected.runtime.taskId == fresh.runtime.taskId && expected.runtime.sessionId == fresh.runtime.sessionId &&
            expected.signature == fresh.signature && expected.fingerprint == fresh.fingerprint
}
