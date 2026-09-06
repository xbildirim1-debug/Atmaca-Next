package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.QueueItemStatus
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.QueueTaskItem
import com.atmacanext.app.domain.model.TaskType
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
