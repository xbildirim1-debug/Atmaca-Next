package com.buse.app.automation

import android.graphics.Rect
import com.buse.app.domain.model.ScheduledTask
import com.buse.app.domain.model.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class SyncChainSpeed26_54Test {
    private fun rect(left: Int, top: Int, right: Int, bottom: Int) = Rect().apply {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom
    }
    private val screen get() = rect(0, 0, 1080, 2400)
    private fun n(text: String, top: Int, enabled: Boolean = true, visible: Boolean = true) = NodeSnapshot(
        text, null, null, "TextView", false, enabled, rect(120, top, 680, top + 70), visible = visible)
    private fun state(type: TaskType = TaskType.VERIFIED_FOLLOW) = AutomationRuntimeState(
        taskId = "task-2", sessionId = "session-2", username = "@ezeldestan", taskType = type,
        verifiedCount = 10, limit = 15, accountVerified = true, status = RuntimeStatus.RUNNING,
        flowStage = XFlowStage.PROCESS_VERIFIED_FOLLOW, activeScreen = XScreen.VERIFIED_FOLLOWERS_LIST)
    @Test fun fastPollingCannotRetapAndCloseDrawerBeforeFirstTapSettles() {
        val gate = AccountNavigationGate()
        gate.issued(XScreen.HOME, 1000)
        assertTrue(gate.wait(XScreen.HOME, 1100))
        assertTrue(gate.wait(XScreen.HOME, 2400))
        assertFalse(gate.wait(XScreen.ACCOUNT_DRAWER, 2450))
        gate.issued(XScreen.ACCOUNT_DRAWER, 2500)
        assertFalse(gate.wait(XScreen.ACCOUNT_DRAWER, 4000))
        gate.clear()
        assertFalse(gate.wait(XScreen.ACCOUNT_DRAWER, 4050))
    }
    @Test fun temporaryUnknownDuringDrawerAnimationDoesNotRearmTap() {
        val gate = AccountNavigationGate()
        gate.issued(XScreen.HOME, 1000)
        assertTrue(gate.wait(XScreen.UNKNOWN, 1100))
        assertTrue(gate.wait(XScreen.HOME, 1200))
        assertFalse(gate.wait(XScreen.UNKNOWN, 2500))
        assertFalse(gate.wait(XScreen.ACCOUNT_DRAWER, 2600))
    }
    private fun field(name: String) = AutomationController.javaClass.getDeclaredField(name).apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST") private fun putState(value: AutomationRuntimeState) {
        (field("_state").get(AutomationController) as MutableStateFlow<AutomationRuntimeState>).value = value
        field("activeSessionToken").set(AutomationController, value.sessionId)
    }

    @Test fun sameCurrentAccountSheetIsDismissedAfterSettleWithoutHomeDeepLink() {
        assertEquals(AccountSelectionReturnPolicy.Decision.WAIT, AccountSelectionReturnPolicy.decide(XScreen.ACCOUNT_SWITCHER, 100, 257))
        assertEquals(AccountSelectionReturnPolicy.Decision.CLOSE_SWITCHER, AccountSelectionReturnPolicy.decide(XScreen.ACCOUNT_SWITCHER, 300, 257))
    }
    @Test fun homeAfterSelectionImmediatelyOpensDrawerWithoutBack() {
        assertEquals(AccountSelectionReturnPolicy.Decision.OPEN_DRAWER, AccountSelectionReturnPolicy.decide(XScreen.HOME, 100, 1800))
    }
    @Test fun alreadyOpenDrawerCanVerifyFirstAccountImmediately() {
        assertEquals(AccountSelectionReturnPolicy.Decision.VERIFY_DRAWER, AccountSelectionReturnPolicy.decide(XScreen.ACCOUNT_DRAWER, 100, 1800))
    }
    @Test fun staleSheetLabelCannotBackFromFreshHomeOrAnotherApp() {
        assertFalse(AccountSelectionReturnPolicy.mayDismiss("com.twitter.android", XScreen.HOME))
        assertFalse(AccountSelectionReturnPolicy.mayDismiss("com.android.launcher", XScreen.ACCOUNT_SWITCHER))
        assertFalse(AccountSelectionReturnPolicy.mayDismiss(null, XScreen.ACCOUNT_SWITCHER))
        assertTrue(AccountSelectionReturnPolicy.mayDismiss("com.twitter.android", XScreen.ACCOUNT_SWITCHER))
    }
    @Test fun switchingAnimationGetsSettleBeforeNavigation() {
        assertEquals(AccountSelectionReturnPolicy.Decision.WAIT, AccountSelectionReturnPolicy.decide(XScreen.UNKNOWN, 80, 257))
        assertEquals(AccountSelectionReturnPolicy.Decision.RETURN, AccountSelectionReturnPolicy.decide(XScreen.PROFILE, 400, 257))
    }
    @Test fun tenRowListWithNegativeVirtualRowsNeverBuildsNegativeStroke() {
        val rows = (0..9).map { rect(100, -2100 + it * 480, 900, -1700 + it * 480) }
        val stroke = ListGestureGeometry.swipe(screen, rows, true)!!
        assertTrue(stroke.valid())
        assertTrue(stroke.startY > stroke.endY)
        assertTrue(stroke.startY <= 2400 && stroke.endY >= 0)
    }
    @Test fun whollyOffscreenRowsUseSafeViewportSwipe() {
        val stroke = ListGestureGeometry.swipe(screen, listOf(rect(100, -1900, 900, -100), rect(100, 3000, 900, 8000)), true)!!
        assertEquals(1872f, stroke.startY, 0.1f)
        assertEquals(720f, stroke.endY, 0.1f)
    }
    @Test fun partialRowsAreClippedAndDoNotTouchTabs() {
        val stroke = ListGestureGeometry.swipe(screen, listOf(rect(100, -400, 900, 500), rect(100, 2200, 900, 4000)), true)!!
        assertTrue(stroke.endY >= 360f)
        assertTrue(stroke.startY <= 2112f)
    }
    @Test fun backwardsStrokeIsSafeAndHasOppositeDirection() {
        val rows = listOf(rect(100, -700, 900, 500), rect(100, 1400, 900, 2000))
        val a = ListGestureGeometry.swipe(screen, rows, true)!!
        val b = ListGestureGeometry.swipe(screen, rows, false)!!
        assertEquals(a.startY, b.endY, 0f)
        assertEquals(a.endY, b.startY, 0f)
    }
    @Test fun rootBoundsAreClippedToDisplayInLandscapeAndPortrait() {
        val a = ListGestureGeometry.viewport(rect(-10, -120, 2500, 2700), 1080, 2400)!!
        assertEquals(0, a.left); assertEquals(0, a.top); assertEquals(1080, a.right); assertEquals(2400, a.bottom)
        val b = ListGestureGeometry.viewport(rect(0, 0, 2400, 1080), 2400, 1080)!!
        assertTrue(ListGestureGeometry.swipe(b, emptyList(), true)!!.valid())
    }
    @Test fun zeroAndNegativeRootCannotDispatch() {
        assertNull(ListGestureGeometry.swipe(rect(0, 0, 0, 0), emptyList(), true))
        assertNull(ListGestureGeometry.viewport(rect(-400, -400, -10, -10)))
    }
    @Test fun horizontalTabGesturesStayInsideScreen() {
        for (forward in listOf(true, false)) {
            val stroke = ListGestureGeometry.swipe(screen, emptyList(), forward, true)!!
            assertTrue(stroke.valid()); assertEquals(stroke.startY, stroke.endY, 0f)
        }
    }
    @Test fun nanInfinityAndNegativeCoordinatesCannotCreateGesture() {
        assertFalse(ListGestureGeometry.Stroke(1f, -1f, 1f, 100f).valid())
        assertFalse(ListGestureGeometry.Stroke(Float.NaN, 1f, 1f, 100f).valid())
        assertFalse(ListGestureGeometry.Stroke(1f, 1f, Float.POSITIVE_INFINITY, 100f).valid())
    }
    @Test fun staleScrollExceptionAllowsFallbackInsteadOfPausingTask() {
        assertEquals(ScrollAttemptResult.ACTION_REJECTED, ListViewportController.guardedScroll { throw IllegalArgumentException("stale virtual row") })
        assertEquals(ScrollAttemptResult.ACTION_REJECTED, ListViewportController.guardedScroll { throw IllegalStateException("stale node") })
        assertEquals(ScrollAttemptResult.SCROLLED, ListViewportController.guardedScroll { ScrollAttemptResult.SCROLLED })
    }
    @Test fun sourceSelectionPrefersVisiblePersonOverCachedOffscreenPerson() {
        assertEquals("ipekinthetardis", VerifiedSourcePolicy.choose(listOf("ipekinthetardis"), listOf("baysolares"), "ezeldestan", setOf("nuktedanlik"), Random(1)))
    }
    @Test fun followedRequestedAndNoButtonUsersRemainSourceCandidates() {
        val rows = listOf(n("@followed", 500), n("Takip ediliyor", 500), n("@requested", 700), n("Beklemede", 700), n("@nobutton", 900))
        assertEquals(listOf("followed", "requested", "nobutton"), VerifiedSourcePolicy.visible(rows, "own", emptySet(), screen))
    }
    @Test fun sourceCannotBeOwnVisitedHiddenDisabledOrVirtualOffscreenRow() {
        val rows = listOf(n("@own", 300), n("@visited", 500), n("@hidden", 700, visible = false),
            n("@disabled", 900, enabled = false), n("@virtual", -300), n("@below", 3000), n("@good", 1300))
        assertEquals(listOf("good"), VerifiedSourcePolicy.visible(rows, "own", setOf("visited"), screen))
    }
    @Test fun relationshipDescriptionIsNeverUsedAsSourceUsername() {
        assertTrue(VerifiedSourcePolicy.visible(listOf(n("Takip et @victim", 500), n("Requested @victim", 700)), "own", emptySet(), screen).isEmpty())
    }
    @Test fun visitedSourcesDoNotCreateProfileCycle() {
        assertNull(VerifiedSourcePolicy.choose(listOf("source", "own"), listOf("source"), "own", setOf("source"), Random(7)))
        assertEquals("fresh", VerifiedSourcePolicy.choose(listOf("source", "fresh"), emptyList(), "own", setOf("source"), Random(7)))
    }
    @Test fun exhaustedSecondAccountKeepsTenOfFifteenAndUsesSameListRoute() {
        val service = BuseAccessibilityService()
        try {
            AutomationController.stop()
            putState(state())
            field("sourceHandle").set(AutomationController, "nuktedanlik")
            @Suppress("UNCHECKED_CAST") val candidates = field("verifiedSourceCandidates").get(AutomationController) as MutableSet<String>
            candidates.addAll(listOf("baysolares", "ipekinthetardis"))
            val next = AutomationController.javaClass.getDeclaredMethod("nextVerifiedSource", BuseAccessibilityService::class.java, String::class.java).apply { isAccessible = true }
            next.invoke(AutomationController, service, "Onaylı liste bitti")
            assertEquals(10, AutomationController.state.value.verifiedCount)
            assertEquals("@ezeldestan", AutomationController.state.value.username)
            assertTrue(AutomationController.state.value.accountVerified)
            assertEquals(XFlowStage.LOCATE_SOURCE_ROW, AutomationController.state.value.flowStage)
            assertEquals(XScreen.VERIFIED_FOLLOWERS_LIST, AutomationController.state.value.activeScreen)
            assertEquals(2, candidates.size)
        } finally { AutomationController.stop() }
    }
    @Test fun transientReadRetriesAreBoundedAndResetForNextAccount() {
        val retry = SnapshotRetryPolicy()
        repeat(3) { assertTrue(retry.retry("account-2")) }
        assertFalse(retry.retry("account-2"))
        assertTrue(retry.retry("account-3"))
        retry.recovered()
        repeat(3) { assertTrue(retry.retry("account-3")) }
    }
    @Test fun snapshotReadRecoveryPreservesAccountStageAndCounterForEveryTask() {
        val service = BuseAccessibilityService()
        try {
            for (type in TaskType.entries.filter { it !in setOf(TaskType.SYNC, TaskType.PUBLISH, TaskType.TREND, TaskType.COMMUNITY) }) {
                AutomationController.stop()
                putState(state(type))
                AutomationController.onSnapshotReadFailure(service)
                val now = AutomationController.state.value
                assertEquals(type.toString(), 10, now.verifiedCount)
                assertEquals(type.toString(), "session-2", now.sessionId)
                assertEquals(type.toString(), XFlowStage.PROCESS_VERIFIED_FOLLOW, now.flowStage)
                assertTrue(now.accountVerified)
                assertEquals(RuntimeStatus.RECOVERING, now.status)
            }
        } finally { AutomationController.stop() }
    }
    @Test fun transientReadDuringFollowDoesNotClearOrResubmitPendingAction() {
        val service = BuseAccessibilityService()
        try {
            AutomationController.stop(); putState(state().copy(status = RuntimeStatus.VERIFYING))
            val kind = Class.forName("com.buse.app.automation.AutomationController\$PendingKind").enumConstants.first { it.toString() == "FOLLOW" }
            val clazz = Class.forName("com.buse.app.automation.AutomationController\$PendingAction")
            val constructor = clazz.declaredConstructors.first { it.parameterCount == 9 }.apply { isAccessible = true }
            val pending = constructor.newInstance(kind, "ipekinthetardis", System.currentTimeMillis(), false, null, false, emptySet<String>(), null, null)
            field("pendingAction").set(AutomationController, pending)
            AutomationController.onSnapshotReadFailure(service)
            assertSame(pending, field("pendingAction").get(AutomationController))
            assertEquals(RuntimeStatus.VERIFYING, AutomationController.state.value.status)
            assertEquals(10, AutomationController.state.value.verifiedCount)
        } finally { AutomationController.stop() }
    }
    @Test fun userPauseIsNeverUndoneByAutomaticReadRecovery() {
        val service = BuseAccessibilityService()
        try {
            AutomationController.stop(); putState(state().copy(status = RuntimeStatus.PAUSED, message = "Kullanıcı duraklattı"))
            AutomationController.onSnapshotReadFailure(service)
            assertEquals(RuntimeStatus.PAUSED, AutomationController.state.value.status)
            assertEquals("Kullanıcı duraklattı", AutomationController.state.value.message)
        } finally { AutomationController.stop() }
    }
    @Test fun shortSystemOverlayDoesNotRestartSelectedAccount() {
        assertTrue(ForegroundReadPolicy.wait("com.android.systemui", 500))
        assertTrue(ForegroundReadPolicy.wait("com.google.android.inputmethod.latin", 1000))
        assertTrue(ForegroundReadPolicy.wait(null, 2000))
        assertFalse(ForegroundReadPolicy.wait("com.android.systemui", 2500))
        assertFalse(ForegroundReadPolicy.wait("com.android.permissioncontroller", 10))
        assertFalse(ForegroundReadPolicy.wait("com.buse.mobile", 10))
    }
    @Test fun screenNoiseAndStatusTextCannotResetStallWatchdogForAnyTask() {
        for (type in TaskType.entries) {
            val base = state(type)
            assertEquals(AutomationStallPolicy.signature(base), AutomationStallPolicy.signature(base.copy(activeScreen = XScreen.UNKNOWN, message = "Olay geldi", status = RuntimeStatus.WAITING)))
            val watchdog = AutomationWatchdog(20_000)
            watchdog.observe(AutomationStallPolicy.signature(base), 1000)
            watchdog.observe(AutomationStallPolicy.signature(base.copy(activeScreen = XScreen.UNKNOWN)), 19000)
            assertTrue(watchdog.isStuck(21000))
        }
    }
    @Test fun secondAccountStartsWithCleanStallAndSourceHistoryForAllSupportedTasks() {
        try {
            for (type in TaskType.entries.filter { it !in setOf(TaskType.SYNC, TaskType.PUBLISH, TaskType.TREND, TaskType.COMMUNITY) }) {
                AutomationController.stop()
                field("stallRecoveryAttempts").setInt(AutomationController, 3)
                field("stallRecoverySignature").set(AutomationController, "old-account")
                assertTrue(AutomationController.start(ScheduledTask("second-$type", "2", "@account2", type = type, limit = 15,
                    targetUrl = "https://x.com/pusholder/status/1", contentText = "Metin", mediaUri = "content://image", quoteTargets = "pusholder"), listOf("pusholder")))
                assertEquals(0, field("stallRecoveryAttempts").getInt(AutomationController))
                assertEquals("", field("stallRecoverySignature").get(AutomationController))
                assertTrue((field("verifiedSourceCandidates").get(AutomationController) as Set<*>).isEmpty())
            }
        } finally { AutomationController.stop() }
    }
    @Test fun upgradeEnablesSevenTimesSpeedIncludingPreviouslySavedSlowSettings() {
        assertEquals(AutomationSpeedPreset.Timing(71, 257, 214), AutomationSpeedPreset.resolve(false, 500, 1800, 1500))
        assertEquals(AutomationSpeedPreset.Timing(71, 257, 214), AutomationSpeedPreset.resolve(false, 15000, 15000, 60000))
    }
    @Test fun laterManualTimingChoiceSurvivesAndRemainsBounded() {
        assertEquals(AutomationSpeedPreset.Timing(500, 1800, 1500), AutomationSpeedPreset.resolve(true, 500, 1800, 1500))
        assertEquals(AutomationSpeedPreset.Timing(71, 100, 60000), AutomationSpeedPreset.resolve(true, 1, 1, 999999))
    }
    @Test fun fastestPresetDividesUiDelaysBySevenExactlyOnce() {
        val before = AutomationTuning.betweenActionsMs
        try {
            AutomationTuning.betweenActionsMs = 71
            assertEquals(100L, AutomationTuning.scaleDelay(700))
            assertEquals(171L, AutomationTuning.scaleDelay(1200))
            assertEquals(16L, AutomationTuning.scaleDelay(50))
            AutomationTuning.betweenActionsMs = 500
            assertEquals(700L, AutomationTuning.scaleDelay(700))
        } finally { AutomationTuning.betweenActionsMs = before }
    }
    @Test fun fastFollowStillNeedsStableResultAndRejectsRevertedFollow() {
        assertEquals(VerifiedFollowOutcome.WAIT, VerifiedFollowPolicy.outcome(true, true, false, 249, 600, stableRequiredMs = 285))
        assertEquals(VerifiedFollowOutcome.SUCCESS, VerifiedFollowPolicy.outcome(true, true, false, 285, 700, stableRequiredMs = 285))
        assertEquals(VerifiedFollowOutcome.REVERTED, VerifiedFollowPolicy.outcome(true, false, true, 300, 700, stableRequiredMs = 285))
    }
}
