package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.AccountAccent
import com.atmacanext.app.domain.model.QueueItemStatus
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.QueueTaskItem
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskQueuePlanner26_42Test {
    private fun account(id: String, username: String) = Account(
        id = id,
        username = username,
        displayName = username,
        followers = "0",
        following = "0",
        engagement = "0",
        health = 100,
        accent = AccountAccent.BLUE,
        active = true,
    )

    private fun task(id: String, account: Account, type: TaskType) = ScheduledTask(
        id = id,
        accountId = account.id,
        username = account.username,
        type = type,
        limit = 3,
        repeatCount = 1,
    )

    @Test
    fun twoSelectedTaskTypesAcrossThreeAccountsStayAccountMajor() {
        val a1 = account("1", "@a1")
        val a2 = account("2", "@a2")
        val a3 = account("3", "@a3")
        val tasks = listOf(
            task("c1", a1, TaskType.COMMENTER_FOLLOW),
            task("c2", a2, TaskType.COMMENTER_FOLLOW),
            task("c3", a3, TaskType.COMMENTER_FOLLOW),
            task("r1", a1, TaskType.RETWEETER_FOLLOW),
            task("r2", a2, TaskType.RETWEETER_FOLLOW),
            task("r3", a3, TaskType.RETWEETER_FOLLOW),
        )
        val queue = TaskQueuePlanner.build(listOf(a1, a2, a3), tasks)
        assertEquals(listOf("c1", "r1", "c2", "r2", "c3", "r3"), queue.map { it.taskId })

        val afterFirstAccount = queue.mapIndexed { index, item ->
            if (index < 2) item.copy(status = QueueItemStatus.COMPLETED) else item
        }
        assertEquals(2, TaskQueuePlanner.nextRunnableIndex(afterFirstAccount, 1))
        assertEquals("2", afterFirstAccount[2].accountId)
    }

    @Test
    fun pausedFutureQueueItemRemainsRecoverable() {
        val a1 = account("1", "@a1")
        val a2 = account("2", "@a2")
        val queue = TaskQueuePlanner.build(
            listOf(a1, a2),
            listOf(task("c1", a1, TaskType.COMMENTER_FOLLOW), task("c2", a2, TaskType.COMMENTER_FOLLOW)),
        ).mapIndexed { index, item ->
            if (index == 0) item.copy(status = QueueItemStatus.COMPLETED)
            else item.copy(status = QueueItemStatus.PAUSED)
        }
        assertEquals(1, TaskQueuePlanner.nextRunnableIndex(queue, 0))
    }

    @Test
    fun unfinishedRowsCannotBeReportedAsSuccessfulQueue() {
        val items = listOf(
            QueueTaskItem("done", "1", "@a1", TaskType.COMMENTER_FOLLOW, QueueItemStatus.COMPLETED),
            QueueTaskItem("waiting", "2", "@a2", TaskType.COMMENTER_FOLLOW, QueueItemStatus.PENDING),
        )
        assertEquals(QueueStatus.PARTIAL, QueueResultPolicy.finalStatus(items))
    }
}
