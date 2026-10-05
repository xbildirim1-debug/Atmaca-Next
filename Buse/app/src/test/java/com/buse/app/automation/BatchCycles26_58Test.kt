package com.buse.app.automation

import com.buse.app.domain.engine.BatchCyclePolicy
import com.buse.app.domain.engine.TaskQueuePlanner
import com.buse.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class BatchCycles26_58Test {
    @Test fun countdownShowsMinutesAndSeconds() {
        assertEquals("Sonraki döngüye 4 dakika 32 saniye kaldı",BatchCyclePolicy.remainingLabel(272000L,0L))
    }
    @Test fun countdownDoesNotShowZeroBeforeDeadline() {
        assertEquals("Sonraki döngüye 0 dakika 1 saniye kaldı",BatchCyclePolicy.remainingLabel(1000L,999L))
    }
    @Test fun countdownNeverGoesNegative() {
        assertEquals("Sonraki döngüye 0 dakika 0 saniye kaldı",BatchCyclePolicy.remainingLabel(1000L,1200L))
    }
    private fun rows() = (1..4).map { QueueTaskItem("task$it","$it","account$it",TaskType.VERIFIED_FOLLOW) }
    @Test fun firstAccountCycleHandsOffToSecondBeforeWait() {
        val items=rows().mapIndexed { i,row -> if(i==0) row.copy(status=QueueItemStatus.PAUSED) else row }
        assertEquals(1,TaskQueuePlanner.nextRunnableIndex(items,0))
        assertEquals(listOf(0),BatchCyclePolicy.nextRoundIndices(items,setOf("task1")))
    }
    @Test fun fourAccountsFinishFirstRoundBeforeSecondRoundStarts() {
        var items=rows();val deferred=linkedSetOf<String>()
        for(i in 0..3) {
            items=items.mapIndexed { index,row -> if(index==i) row.copy(status=QueueItemStatus.PAUSED) else row }
            deferred.add(items[i].taskId)
            assertEquals(if(i<3) i+1 else -1,TaskQueuePlanner.nextRunnableIndex(items,i))
        }
        assertEquals(listOf(0,1,2,3),BatchCyclePolicy.nextRoundIndices(items,deferred))
    }
    @Test fun secondRoundKeepsSameAccountOrder() {
        val items=rows().map { it.copy(status=QueueItemStatus.PAUSED) }
        val next=BatchCyclePolicy.nextRoundIndices(items,items.map { it.taskId }.toSet())
        assertEquals(listOf("account1","account2","account3","account4"),next.map { items[it].username })
    }
    @Test fun finalRoundDoesNotCreateAnotherWait() {
        val items=rows().map { it.copy(status=QueueItemStatus.COMPLETED) }
        assertTrue(BatchCyclePolicy.nextRoundIndices(items,items.map { it.taskId }.toSet()).isEmpty())
    }
    @Test fun skippedOrFailedAccountIsNotRevivedForSecondRound() {
        val items=rows().mapIndexed { i,row -> row.copy(status=when(i) {0->QueueItemStatus.SKIPPED;1->QueueItemStatus.FAILED;else->QueueItemStatus.PAUSED}) }
        assertEquals(listOf(2,3),BatchCyclePolicy.nextRoundIndices(items,items.map { it.taskId }.toSet()))
    }
    @Test fun manualPausedTaskIsNotMistakenForCompletedRound() {
        assertTrue(BatchCyclePolicy.nextRoundIndices(rows().map { it.copy(status=QueueItemStatus.PAUSED) },emptySet()).isEmpty())
    }
    @Test fun singleAccountRepeatsUseSameRoundBoundary() {
        val items=rows().take(1).map { it.copy(status=QueueItemStatus.PAUSED) }
        assertEquals(-1,TaskQueuePlanner.nextRunnableIndex(items,0))
        assertEquals(listOf(0),BatchCyclePolicy.nextRoundIndices(items,setOf("task1")))
    }
    @Test fun intentionalBatchIntervalKeepsQueueActive() {
        assertTrue(AutomationQueueState(status=QueueStatus.WAITING_INTERVAL).isActive)
        assertFalse(AutomationQueueState(status=QueueStatus.STOPPED).isActive)
    }
    @Test fun resumedTaskStartsRemainingCycleWithoutLosingTotalProgress() {
        try {
            AutomationController.stop()
            assertTrue(AutomationController.start(ScheduledTask("resume","1","account1",type=TaskType.VERIFIED_FOLLOW,
                progress=10,limit=10,repeatCount=2,intervalMinutes=5),delegateCycleWait=true))
            val state=AutomationController.state.value
            assertEquals(10,state.verifiedCount);assertEquals(20,state.limit);assertEquals(1,state.cycleIndex)
            val field=AutomationController.javaClass.getDeclaredField("queueOwnsCycleWait").apply { isAccessible=true }
            assertTrue(field.getBoolean(AutomationController))
        } finally { AutomationController.stop() }
    }
    @Test fun directRuntimeCallerRetainsExistingSingleTaskWaitOwnership() {
        try {
            AutomationController.stop()
            assertTrue(AutomationController.start(ScheduledTask("direct","1","account1",type=TaskType.VERIFIED_FOLLOW,repeatCount=2)))
            val field=AutomationController.javaClass.getDeclaredField("queueOwnsCycleWait").apply { isAccessible=true }
            assertFalse(field.getBoolean(AutomationController))
        } finally { AutomationController.stop() }
    }
}
