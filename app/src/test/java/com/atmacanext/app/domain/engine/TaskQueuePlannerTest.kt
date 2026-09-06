package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.AccountAccent
import com.atmacanext.app.domain.model.QueueItemStatus
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskQueuePlannerTest {
    private val accounts = listOf(
        Account("2", "@two", "Two", "0", "0", "0", 100, AccountAccent.BLUE),
        Account("1", "@one", "One", "0", "0", "0", 100, AccountAccent.BLUE),
        Account("3", "@three", "Three", "0", "0", "0", 100, AccountAccent.BLUE, active = false),
    )

    @Test
    fun `queue is account-major and filters unsupported inactive and completed work`() {
        val tasks = listOf(
            ScheduledTask("b", "2", "@two", "09:00", TaskType.UNFOLLOW, TaskStatus.QUEUED, 0, 35),
            ScheduledTask("a2", "1", "@one", "12:00", TaskType.UNFOLLOW, TaskStatus.QUEUED, 0, 35),
            ScheduledTask("a1", "1", "@one", "10:00", TaskType.VERIFIED_FOLLOW, TaskStatus.QUEUED, 0, 35),
            ScheduledTask("inactive", "3", "@three", "08:00", TaskType.UNFOLLOW, TaskStatus.QUEUED, 0, 35),
            ScheduledTask("unsupported", "1", "@one", "08:00", TaskType.PUBLISH, TaskStatus.QUEUED, 0, 35),
            ScheduledTask("done", "2", "@two", "08:30", TaskType.UNFOLLOW, TaskStatus.COMPLETED, 35, 35),
        )

        val queue = TaskQueuePlanner.build(accounts, tasks)

        assertEquals(listOf("a1", "a2", "b"), queue.map { it.taskId })
        assertEquals(listOf("1", "1", "2"), queue.map { it.accountId })
        assertEquals(List(3) { QueueItemStatus.PENDING }, queue.map { it.status })
    }

    @Test
    fun `next runnable ignores completed skipped and failed items`() {
        val base = TaskQueuePlanner.build(
            accounts.take(2),
            listOf(
                ScheduledTask("a", "1", "@one", "09:00", TaskType.UNFOLLOW),
                ScheduledTask("b", "1", "@one", "10:00", TaskType.UNFOLLOW),
                ScheduledTask("c", "2", "@two", "11:00", TaskType.UNFOLLOW),
            )
        )
        val items = listOf(
            base[0].copy(status = QueueItemStatus.COMPLETED),
            base[1].copy(status = QueueItemStatus.SKIPPED),
            base[2].copy(status = QueueItemStatus.PENDING),
        )

        assertEquals(2, TaskQueuePlanner.nextRunnableIndex(items, 0))
        assertEquals(-1, TaskQueuePlanner.nextRunnableIndex(items, 2))
    }
}
