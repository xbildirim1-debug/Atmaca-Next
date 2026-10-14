package com.atmacanext.app.automation

import com.atmacanext.app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationStallPolicy26_41Test {
    private fun runtime(
        stage: XFlowStage = XFlowStage.SEARCH_DISCOVERY_TARGET,
        screen: XScreen = XScreen.HOME,
        verified: Int = 0,
        scrolls: Int = 0,
        status: RuntimeStatus = RuntimeStatus.NAVIGATING,
    ) = AutomationRuntimeState(
        sessionId = "session-1",
        taskId = "task-1",
        username = "@test",
        status = status,
        verifiedCount = verified,
        limit = 5,
        activeScreen = screen,
        flowStage = stage,
        listScrolls = scrolls,
    )

    @Test fun repeatedAccessibilityNoiseDoesNotChangeMeaningfulSignature() {
        val first = runtime()
        val sameMeaningfulState = first.copy(message = "Başka log metni", lastActionAt = 1234L)
        assertEquals(AutomationStallPolicy.signature(first), AutomationStallPolicy.signature(sameMeaningfulState))
    }

    @Test fun realNavigationAndListProgressResetSignature() {
        val base = runtime()
        // Temporary UNKNOWN/known screen flips cannot keep a stalled task alive.
        assertEquals(AutomationStallPolicy.signature(base), AutomationStallPolicy.signature(base.copy(activeScreen = XScreen.UNKNOWN)))
        assertNotEquals(AutomationStallPolicy.signature(base), AutomationStallPolicy.signature(base.copy(flowStage = XFlowStage.SCAN_LATEST_TWEETS)))
        assertNotEquals(AutomationStallPolicy.signature(base), AutomationStallPolicy.signature(base.copy(verifiedCount = 1)))
        assertNotEquals(AutomationStallPolicy.signature(base), AutomationStallPolicy.signature(base.copy(listScrolls = 1)))
    }

    @Test fun intentionalLongWaitsAreNeverWatched() {
        assertFalse(AutomationStallPolicy.shouldWatch(runtime(stage = XFlowStage.WAIT_INTERVAL, status = RuntimeStatus.WAITING).copy(cycleWaitUntil = 999999L)))
        assertFalse(AutomationStallPolicy.shouldWatch(runtime(status = RuntimeStatus.COOLDOWN)))
        assertFalse(AutomationStallPolicy.shouldWatch(runtime(status = RuntimeStatus.PAUSED)))
    }

    @Test fun ordinaryNavigationAndWaitingAreWatched() {
        assertTrue(AutomationStallPolicy.shouldWatch(runtime(status = RuntimeStatus.NAVIGATING)))
        assertTrue(AutomationStallPolicy.shouldWatch(runtime(status = RuntimeStatus.WAITING)))
    }

    @Test fun accountVerificationIsNotAStall() {
        assertFalse(AutomationStallPolicy.shouldWatch(runtime(stage = XFlowStage.VERIFY_ACCOUNT)))
    }

    @Test fun verifiedFollowStuckOnPreviousAccountListIsAStall() {
        val stuck = runtime(stage = XFlowStage.VERIFY_ACCOUNT, screen = XScreen.VERIFIED_FOLLOWERS_LIST)
            .copy(taskType = TaskType.VERIFIED_FOLLOW)
        assertTrue(AutomationStallPolicy.shouldWatch(stuck))
        assertTrue(AutomationStallPolicy.shouldWatch(stuck.copy(activeScreen = XScreen.FOLLOWERS_LIST)))
        assertFalse(AutomationStallPolicy.shouldWatch(stuck.copy(activeScreen = XScreen.HOME)))
        assertFalse(AutomationStallPolicy.shouldWatch(stuck.copy(activeScreen = XScreen.ACCOUNT_DRAWER)))
        assertFalse(AutomationStallPolicy.shouldWatch(stuck.copy(activeScreen = XScreen.ACCOUNT_SWITCHER)))
    }

    @Test fun verifiedFollowSetupWaitsAreNotStalls() {
        val setup = listOf(
            XFlowStage.OPEN_VERIFIED_OWN_PROFILE,
            XFlowStage.OPEN_MY_FOLLOWERS,
            XFlowStage.LOCATE_SOURCE_ROW,
            XFlowStage.RETURN_VERIFIED_SOURCE,
            XFlowStage.OPEN_SOURCE_PROFILE,
        )
        for (stage in setup) {
            assertFalse(
                AutomationStallPolicy.shouldWatch(
                    runtime(stage = stage).copy(taskType = TaskType.VERIFIED_FOLLOW),
                ),
            )
        }
        assertTrue(
            AutomationStallPolicy.shouldWatch(
                runtime(stage = XFlowStage.PROCESS_VERIFIED_FOLLOW).copy(taskType = TaskType.VERIFIED_FOLLOW),
            ),
        )
    }
}
