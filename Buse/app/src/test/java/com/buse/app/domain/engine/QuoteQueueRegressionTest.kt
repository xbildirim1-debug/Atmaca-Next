package com.buse.app.domain.engine

import com.buse.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class QuoteQueueRegressionTest {
    private fun account(id: String) = Account(id, "@hesap" + id, "Hesap", "0", "0", "0", 100, AccountAccent.BLUE)
    @Test fun quoteTasksRunOnBothSelectedAccountsInOrder() {
        val tasks = (1..2).map { i -> ScheduledTask("q" + i, i.toString(), "@hesap" + i,
            type = TaskType.COMMENT_QUOTE_TARGETS, limit = 1, quoteTargets = "hedef\nhedef2", contentText = "Yorum") }
        val queue = TaskQueuePlanner.build(listOf(account("1"), account("2")), tasks)
        assertEquals(listOf("q1", "q2"), queue.map { it.taskId })
        assertEquals(1, TaskQueuePlanner.nextRunnableIndex(queue.mapIndexed { i, item -> if (i == 0) item.copy(status = QueueItemStatus.COMPLETED) else item }, 0))
    }
    @Test fun progressOfFirstTargetDoesNotRemoveTaskWithRemainingTargets() {
        val task = ScheduledTask("q", "1", "@hesap1", type = TaskType.COMMENT_QUOTE_TARGETS,
            limit = 1, progress = 1, quoteTargets = "hedef\nhedef2", contentText = "Yorum")
        assertEquals(1, TaskQueuePlanner.build(listOf(account("1")), listOf(task)).size)
    }
}
