package com.atmacanext.app.automation

import com.atmacanext.app.domain.model.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class NonFollowerUnfollowCompletionTest {
    private fun field(name: String) = AutomationController::class.java.getDeclaredField(name).apply { isAccessible = true }
    private fun install(progress: Int, repeats: Int = 1) {
        AutomationController.stop()
        field("cycleStartProgress").setInt(AutomationController, 0)
        field("queueOwnsCycleWait").setBoolean(AutomationController, true)
        @Suppress("UNCHECKED_CAST")
        val state = field("_state").get(AutomationController) as MutableStateFlow<AutomationRuntimeState>
        state.value = AutomationRuntimeState(sessionId = "test", taskId = "unfollow-20", username = "@own",
            taskType = TaskType.UNFOLLOW_NON_FOLLOWERS, status = RuntimeStatus.RUNNING, verifiedCount = progress,
            limit = 20 * repeats, perCycleLimit = 20, repeatCount = repeats, cycleIndex = 0,
            flowStage = XFlowStage.PROCESS_UNFOLLOW, accountVerified = true)
    }
    private fun finish() = AutomationController::class.java.getDeclaredMethod("finishCycleOrTask",
        AtmacaAccessibilityService::class.java, String::class.java).apply { isAccessible = true }
        .invoke(AutomationController, AtmacaAccessibilityService(), "Liste sonu")

    @Test fun oneOf20CannotBeCompletedWhenAListEndIsReported() {
        try {
            install(1); finish()
            val state = AutomationController.state.value
            assertEquals(RuntimeStatus.PAUSED, state.status)
            assertEquals(1, state.verifiedCount)
            assertEquals(20, state.limit)
            assertTrue(state.message.contains("1/20"))
        } finally { AutomationController.stop() }
    }

    @Test fun all20ConfirmedActionsCanCompleteTheTask() {
        try { install(20); finish(); assertEquals(RuntimeStatus.COMPLETED, AutomationController.state.value.status) }
        finally { AutomationController.stop() }
    }

    @Test fun aPartialRoundCannotBeReplacedByTheNextRound() {
        try {
            install(1, 2); finish()
            assertEquals(RuntimeStatus.PAUSED, AutomationController.state.value.status)
            assertEquals(0, AutomationController.state.value.cycleIndex)
            assertEquals(1, AutomationController.state.value.verifiedCount)
        } finally { AutomationController.stop() }
    }

    @Test fun aFullRoundStillAllowsTheConfiguredNextRound() {
        try {
            install(20, 2); finish()
            assertEquals(RuntimeStatus.WAITING, AutomationController.state.value.status)
            assertEquals(XFlowStage.WAIT_INTERVAL, AutomationController.state.value.flowStage)
            assertEquals(1, AutomationController.state.value.cycleIndex)
        } finally { AutomationController.stop() }
    }

    @Test fun realSuccessPathContinuesThrough19AndFinishesAt20() {
        val service = AtmacaAccessibilityService()
        val record = AutomationController::class.java.getDeclaredMethod("recordSuccess",
            AtmacaAccessibilityService::class.java, String::class.java).apply { isAccessible = true }
        try {
            install(0)
            for (number in 1..20) {
                record.invoke(AutomationController, service, "@person$number")
                val state = AutomationController.state.value
                assertEquals(number, state.verifiedCount)
                assertEquals(if (number < 20) RuntimeStatus.RUNNING else RuntimeStatus.COMPLETED, state.status)
            }
        } finally { AutomationController.stop() }
    }
}
