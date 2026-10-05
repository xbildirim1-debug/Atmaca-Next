package com.buse.app.automation

import com.buse.app.domain.model.ScheduledTask
import com.buse.app.domain.model.TaskStatus
import com.buse.app.domain.model.TaskType
import com.buse.app.domain.policy.BuseTaskPolicy
import org.junit.Assert.*
import org.junit.Test

class BusePartialTaskRecoveryTest {
    private fun partial() = ScheduledTask("task", "account", "@own", type = TaskType.UNFOLLOW,
        limit = 20, progress = 1, status = TaskStatus.COMPLETED, contentPrompt = BuseTaskPolicy.NON_FOLLOWER_MARKER)

    @Test fun incorrectlyCompletedOneOf20CanResumeWithoutLosingProgress() {
        val task = partial()
        assertTrue(BuseTaskPolicy.completionNeedsRecovery(task))
        assertEquals(1, task.progress)
        assertEquals(20, task.totalLimit)
    }

    @Test fun realCompleted20Of20StaysCompleted() {
        assertFalse(BuseTaskPolicy.completionNeedsRecovery(partial().copy(progress = 20)))
    }

    @Test fun normalUnfollowPartialCompletionAlsoRecovers() {
        assertTrue(BuseTaskPolicy.completionNeedsRecovery(partial().copy(contentPrompt = null)))
    }

    @Test fun runningPausedAndUnsupportedTasksAreNotRewritten() {
        assertFalse(BuseTaskPolicy.completionNeedsRecovery(partial().copy(status = TaskStatus.RUNNING)))
        assertFalse(BuseTaskPolicy.completionNeedsRecovery(partial().copy(status = TaskStatus.PAUSED)))
        assertFalse(BuseTaskPolicy.completionNeedsRecovery(partial().copy(type = TaskType.LIKE)))
    }

    @Test fun completedStatusCannotHideAnUnfinishedRepeatedTask() {
        assertTrue(BuseTaskPolicy.completionNeedsRecovery(partial().copy(progress = 20, repeatCount = 2)))
        assertFalse(BuseTaskPolicy.completionNeedsRecovery(partial().copy(progress = 40, repeatCount = 2)))
    }
}
