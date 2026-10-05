package com.buse.app.domain.engine

import com.buse.app.domain.model.QueueItemStatus
import com.buse.app.domain.model.QueueStatus
import com.buse.app.domain.model.QueueTaskItem
import com.buse.app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueResultPolicyTest {
    private fun item(status: QueueItemStatus) = QueueTaskItem("task-$status", "account", "@account", TaskType.LIKE, status)

    @Test fun allCompletedIsCompleted() {
        assertEquals(QueueStatus.COMPLETED, QueueResultPolicy.finalStatus(listOf(item(QueueItemStatus.COMPLETED))))
    }

    @Test fun skippedWorkIsPartial() {
        assertEquals(
            QueueStatus.PARTIAL,
            QueueResultPolicy.finalStatus(listOf(item(QueueItemStatus.COMPLETED), item(QueueItemStatus.SKIPPED))),
        )
    }

    @Test fun noSuccessfulWorkWithFailureIsFailed() {
        assertEquals(
            QueueStatus.FAILED,
            QueueResultPolicy.finalStatus(listOf(item(QueueItemStatus.FAILED), item(QueueItemStatus.SKIPPED))),
        )
    }
}
