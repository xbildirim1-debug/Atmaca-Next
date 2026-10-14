package com.atmacanext.app.ai

import com.atmacanext.app.automation.*
import com.atmacanext.app.domain.model.TaskType
import org.junit.Assert.*
import org.junit.Test

class FastPilot27_66Test {
    private val runtime = AutomationRuntimeState(taskId = "task", sessionId = "session", username = "own",
        taskType = TaskType.UNFOLLOW_NON_FOLLOWERS, status = RuntimeStatus.RUNNING, accountVerified = true,
        activeScreen = XScreen.FOLLOWING_LIST, flowStage = XFlowStage.SEEK_UNFOLLOW_DEPTH, verifiedCount = 7, limit = 35)
    private fun observation(s: AutomationRuntimeState = runtime) = PilotScreenObservation(s, "progress", "view", 10_000L)
    @Test fun healthyFollowingListNeverGetsBack() {
        assertEquals(setOf(PilotMove.REFRESH), FastPilotPolicy.allowed(observation(), 11_000L))
    }
    @Test fun ownProfileCanReopenTheRequiredCounterWithinOneSecond() {
        val o = observation(runtime.copy(activeScreen = XScreen.PROFILE)).copy(ownProfile = true)
        assertEquals(PilotMove.OPEN_FOLLOWING, FastPilotPolicy.localMove(o, 1_000L, 11_000L))
    }
    @Test fun tabletProfileMisclassifiedAsHomeStillNeedsOwnIdentity() {
        val o = observation(runtime.copy(activeScreen = XScreen.HOME))
        assertFalse(PilotMove.OPEN_FOLLOWING in FastPilotPolicy.allowed(o, 11_000L))
        assertTrue(PilotMove.OPEN_FOLLOWING in FastPilotPolicy.allowed(o.copy(ownProfile = true), 11_000L))
    }
    @Test fun wrongProfileCannotOpenFollowing() {
        assertEquals(setOf(PilotMove.REFRESH), FastPilotPolicy.allowed(observation(runtime.copy(activeScreen = XScreen.PROFILE)), 11_000L))
    }
    @Test fun accidentalTweetDetailCanReturnWithinTheSameTask() {
        val o = observation(runtime.copy(activeScreen = XScreen.TWEET_DETAIL))
        assertEquals(PilotMove.BACK, FastPilotPolicy.localMove(o, 1_000L, 11_000L))
    }
    @Test fun manualPauseAndCycleWaitHaveNoPilotAction() {
        assertTrue(FastPilotPolicy.allowed(observation(runtime.copy(status = RuntimeStatus.PAUSED)), 11_000L).isEmpty())
        assertTrue(FastPilotPolicy.allowed(observation(runtime.copy(cycleWaitUntil = 30_000L)), 11_000L).isEmpty())
        assertTrue(FastPilotPolicy.allowed(observation(runtime.copy(flowStage = XFlowStage.WAIT_INTERVAL)), 11_000L).isEmpty())
    }
    @Test fun anUnverifiedAccountCannotAcquireNavigationAuthority() {
        assertTrue(FastPilotPolicy.allowed(observation(runtime.copy(accountVerified = false)), 11_000L).isEmpty())
    }
    @Test fun pendingActionQuoteAndPointerBlockAllPilotMoves() {
        val o = observation()
        for (pending in listOf(o.copy(pendingAction = true), o.copy(moving = true), o.copy(settling = true),
            observation(runtime.copy(quotePendingKey = "reply")))) {
            assertTrue(FastPilotPolicy.allowed(pending, 11_000L).isEmpty())
        }
    }
    @Test fun composedTextCannotBeInterrupted() {
        for (stage in listOf(XFlowStage.OPEN_COMPOSER, XFlowStage.FILL_COMPOSER, XFlowStage.SUBMIT_COMPOSER))
            assertTrue(FastPilotPolicy.allowed(observation(runtime.copy(flowStage = stage)), 11_000L).isEmpty())
    }
    @Test fun rateLimitPermissionAndActionConfirmationsBelongToTheirExistingHandlers() {
        for (popup in listOf(PopupType.RATE_LIMIT, PopupType.PERMISSION_PROMPT, PopupType.ACTION_CONFIRMATION))
            assertTrue(FastPilotPolicy.allowed(observation().copy(popup = popup), 11_000L).isEmpty())
    }
    @Test fun optionalPopupCanBeDismissedWithoutWaitingForTheServer() {
        assertEquals(PilotMove.DISMISS, FastPilotPolicy.localMove(observation().copy(popup = PopupType.NOTIFICATION_PROMPT), 0, 11_000L))
    }
    @Test fun loadingProfileDoesNotReceiveACounterClick() {
        val o = observation(runtime.copy(activeScreen = XScreen.PROFILE)).copy(ownProfile = true, loading = true)
        assertFalse(PilotMove.OPEN_FOLLOWING in FastPilotPolicy.allowed(o, 11_000L))
    }
    @Test fun staleAndFutureObservationsAreRejected() {
        assertTrue(FastPilotPolicy.allowed(observation(), 12_501L).isEmpty())
        assertTrue(FastPilotPolicy.allowed(observation(), 9_999L).isEmpty())
    }
    @Test fun modelProposalMustStillMatchTheAccountSessionProgressAndViewport() {
        val o = observation()
        for (fresh in listOf(o.copy(runtime = runtime.copy(taskId = "other")),
            o.copy(runtime = runtime.copy(sessionId = "new")), o.copy(signature = "advanced"), o.copy(fingerprint = "new-view")))
            assertFalse(FastPilotPolicy.matches(o, fresh))
        assertTrue(FastPilotPolicy.matches(o, o.copy(observedAt = 11_000L)))
    }
}
