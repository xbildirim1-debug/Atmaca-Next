package com.atmacanext.app.automation

import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class NavigationRestart26_60Test {
    private fun current(type: TaskType = TaskType.COMMENTER_FOLLOW) = AutomationRuntimeState(
        taskId = "task", sessionId = "session", username = "@saresirinnn", taskType = type,
        status = RuntimeStatus.NAVIGATING, verifiedCount = 4, limit = 70, perCycleLimit = 35,
        repeatCount = 2, cycleIndex = 0, flowStage = XFlowStage.RETURN_ENGAGEMENT,
        activeScreen = XScreen.HOME, accountVerified = true, lastTarget = "kirmizituborg48", lastActionAt = 1234L)

    @Test fun sixSecondReturnFailureStaysUnderTheWatchdog() {
        assertEquals(CommenterReturnPolicy.Decision.WAIT, CommenterReturnPolicy.decide(false, false, false, 3, 6000, 1000))
        assertTrue(AutomationStallPolicy.shouldWatch(current()))
    }
    @Test fun missingParentOnHomeRequestsRestartAtTenSeconds() {
        assertEquals(CommenterReturnPolicy.Decision.RESTART, CommenterReturnPolicy.decide(false, false, false, 3, 10000, 1000))
    }
    @Test fun recoveryDoesNotWaitPastDeadlineForAnUnprovenChild() {
        assertEquals(CommenterReturnPolicy.Decision.RESTART, CommenterReturnPolicy.decide(false, false, true, 0, 10000, 1000))
    }
    @Test fun aVisibleParentStopsBackAndRestart() {
        assertEquals(CommenterReturnPolicy.Decision.PARENT, CommenterReturnPolicy.decide(true, false, true, 3, 10000, 1000))
    }
    @Test fun reachingTheTargetProfileContinuesItsScan() {
        assertEquals(CommenterReturnPolicy.Decision.TARGET, CommenterReturnPolicy.decide(false, true, true, 3, 10000, 1000))
    }
    @Test fun parentHeaderWithLateRowsCannotReceiveAnExtraBack() {
        assertFalse(CommenterReturnPolicy.provenChildDetail("parent", "commenter", "parent"))
        assertFalse(CommenterReturnPolicy.provenChildDetail(null, "commenter", "parent"))
        assertFalse(CommenterReturnPolicy.provenChildDetail("other", "commenter", "parent"))
        assertTrue(CommenterReturnPolicy.provenChildDetail("commenter", "commenter", "parent"))
    }
    @Test fun ambiguousSharedParentAndChildAuthorCannotReceiveAnExtraBack() {
        assertFalse(CommenterReturnPolicy.provenChildDetail("same", "same", "same"))
    }
    @Test fun transientNavigationFailureUsesTheFullTenSecondDeadline() {
        assertFalse(NavigationRestartPolicy.deadlineReached(6000L))
        assertFalse(NavigationRestartPolicy.deadlineReached(9999L))
        assertTrue(NavigationRestartPolicy.deadlineReached(10000L))
    }
    @Test fun aMissingReturnCallbackCannotLeaveAnInfiniteRecoveryGuard() {
        assertFalse(NavigationRestartPolicy.returnExpired(1000L,10999L))
        assertTrue(NavigationRestartPolicy.returnExpired(1000L,11000L))
        assertFalse(NavigationRestartPolicy.returnExpired(0L,11000L))
    }
    @Test fun oldReturnAttemptCannotCompleteANewerReturn() {
        val returning=current().copy(status=RuntimeStatus.RECOVERING)
        assertFalse(NavigationRestartPolicy.acceptsCallback(returning,"task","session",true,1L,2L))
        assertTrue(NavigationRestartPolicy.acceptsCallback(returning,"task","session",true,2L,2L))
    }
    @Test fun everyRunnableTaskUsesTheSameNavigationRecoveryPolicy() {
        TaskType.entries.forEach { assertTrue(it.name, NavigationRestartPolicy.mayPrepare(current(it), false, false)) }
    }
    @Test fun manualPauseAndDeviceCooldownCannotBeResumedAutomatically() {
        listOf(RuntimeStatus.PAUSED, RuntimeStatus.COOLDOWN, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED, RuntimeStatus.IDLE)
            .forEach { assertFalse(NavigationRestartPolicy.mayPrepare(current().copy(status=it), false, false)) }
    }
    @Test fun intervalWaitIsNeverAnUnattendedStall() {
        assertFalse(NavigationRestartPolicy.mayPrepare(current().copy(status=RuntimeStatus.WAITING,
            flowStage=XFlowStage.WAIT_INTERVAL, cycleWaitUntil=999999L), false, false))
    }
    @Test fun aPendingActionOrQuoteCheckpointCannotBeDiscarded() {
        assertFalse(NavigationRestartPolicy.mayPrepare(current(), true, false))
        assertFalse(NavigationRestartPolicy.mayPrepare(current().copy(quotePendingKey="post"), false, false))
    }
    @Test fun aReturnAlreadyInFlightCannotStartAgain() {
        assertFalse(NavigationRestartPolicy.mayPrepare(current(), false, true))
    }
    @Test fun staleCallbacksFromAnotherTaskOrSessionAreIgnored() {
        val returning=current().copy(status=RuntimeStatus.RECOVERING)
        assertTrue(NavigationRestartPolicy.acceptsCallback(returning,"task","session",true))
        assertFalse(NavigationRestartPolicy.acceptsCallback(returning,"other","session",true))
        assertFalse(NavigationRestartPolicy.acceptsCallback(returning,"task","old-session",true))
    }
    @Test fun stoppedOrManuallyPausedReturnCallbacksAreIgnored() {
        assertFalse(NavigationRestartPolicy.acceptsCallback(current().copy(status=RuntimeStatus.PAUSED),"task","session",true))
        assertFalse(NavigationRestartPolicy.acceptsCallback(AutomationRuntimeState(),"task","session",true))
        assertFalse(NavigationRestartPolicy.acceptsCallback(current().copy(status=RuntimeStatus.RECOVERING),"task","session",false))
    }

    private fun field(name:String)=AutomationController::class.java.getDeclaredField(name).apply {isAccessible=true}
    @Suppress("UNCHECKED_CAST")
    private fun install() {
        AutomationController.stop()
        field("activeTask").set(AutomationController,ScheduledTask("task","account","@saresirinnn",type=TaskType.COMMENTER_FOLLOW,limit=35,repeatCount=2))
        field("activeSessionToken").set(AutomationController,"session")
        (field("_state").get(AutomationController) as MutableStateFlow<AutomationRuntimeState>).value=current()
        field("cycleStartProgress").setInt(AutomationController,0)
        (field("processedHandles").get(AutomationController) as MutableSet<String>).addAll(listOf("first","second","third","kirmizituborg48"))
        (field("skippedHandles").get(AutomationController) as MutableSet<String>).add("skipped")
        (field("skippedReplyKeys").get(AutomationController) as MutableSet<String>).add("media-reply")
        field("discoveryTargetIndex").setInt(AutomationController,2)
        field("queueOwnsCycleWait").setBoolean(AutomationController,true)
    }
    private fun prepare()=AutomationController::class.java.getDeclaredMethod("prepareNavigationRestart",String::class.java)
        .apply {isAccessible=true}.invoke(AutomationController,"Ana yorumlara dönüş doğrulanamadı")

    @Test fun actualRestartPreparationPreservesFourOf35AndAllCycleFields() {
        try {
            install();assertNotNull(prepare())
            val actual=AutomationController.state.value
            assertEquals(4,actual.verifiedCount);assertEquals(70,actual.limit);assertEquals(35,actual.perCycleLimit)
            assertEquals(2,actual.repeatCount);assertEquals(0,actual.cycleIndex)
            assertEquals("task",actual.taskId);assertEquals("session",actual.sessionId)
            assertEquals("kirmizituborg48",actual.lastTarget);assertEquals(1234L,actual.lastActionAt)
            assertEquals(RuntimeStatus.RECOVERING,actual.status);assertEquals(XFlowStage.VERIFY_ACCOUNT,actual.flowStage)
            assertFalse(actual.accountVerified)
            assertEquals(0,field("cycleStartProgress").getInt(AutomationController))
        } finally {AutomationController.stop()}
    }
    @Test fun exactReportedSingleCycleFourOf35StaysFourOf35() {
        try {
            install()
            field("activeTask").set(AutomationController,
                ScheduledTask("task","account","@saresirinnn",type=TaskType.COMMENTER_FOLLOW,limit=35,repeatCount=1))
            @Suppress("UNCHECKED_CAST")
            val state=field("_state").get(AutomationController) as MutableStateFlow<AutomationRuntimeState>
            state.value=current().copy(limit=35,repeatCount=1)
            assertNotNull(prepare())
            assertEquals(4,AutomationController.state.value.verifiedCount)
            assertEquals(35,AutomationController.state.value.limit)
            assertEquals(1,AutomationController.state.value.repeatCount)
            assertEquals(0,AutomationController.state.value.cycleIndex)
            assertEquals(RuntimeStatus.RECOVERING,AutomationController.state.value.status)
        } finally {AutomationController.stop()}
    }
    @Test fun actualRestartKeepsCompletedPeopleSkippedRepliesAndCurrentTarget() {
        try {
            install();prepare()
            assertEquals(setOf("first","second","third","kirmizituborg48"),field("processedHandles").get(AutomationController))
            assertEquals(setOf("skipped"),field("skippedHandles").get(AutomationController))
            assertEquals(setOf("media-reply"),field("skippedReplyKeys").get(AutomationController))
            assertEquals(2,field("discoveryTargetIndex").getInt(AutomationController))
            assertTrue(field("queueOwnsCycleWait").getBoolean(AutomationController))
        } finally {AutomationController.stop()}
    }
    @Test fun manualPauseCancelsTheActualReturnGuard() {
        try {install();prepare();AutomationController.pause();assertFalse(field("navigationRecoveryReturning").getBoolean(AutomationController))}
        finally {AutomationController.stop()}
    }
    @Test fun actualStopClearsTheReturnGuardAndSession() {
        install();prepare();AutomationController.stop()
        assertFalse(field("navigationRecoveryReturning").getBoolean(AutomationController));assertNull(AutomationController.state.value.taskId)
    }
    @Test fun repeatedRestartWithoutProgressStopsAtItsBoundAndKeepsCount() {
        try {
            install()
            repeat(3) {assertNotNull(prepare());field("navigationRecoveryReturning").setBoolean(AutomationController,false)}
            assertNull(prepare());assertEquals(RuntimeStatus.PAUSED,AutomationController.state.value.status)
            assertEquals(4,AutomationController.state.value.verifiedCount)
        } finally {AutomationController.stop()}
    }
}
