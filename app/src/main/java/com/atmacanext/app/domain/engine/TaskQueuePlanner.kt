package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.QueueItemStatus
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.QueueTaskItem
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TaskType

object TaskQueuePlanner {
    const val MAX_ACCOUNTS = 10
    private val supportedTypes = setOf(
        TaskType.TEXT_TWEET,
        TaskType.IMAGE_TWEET,
        TaskType.FOLLOW,
        TaskType.LIKE,
        TaskType.RETWEET,
        TaskType.BOOKMARK,
        TaskType.COMMENT,
        TaskType.QUOTE,
        TaskType.VERIFIED_FOLLOW,
        TaskType.UNFOLLOW,
        TaskType.COMMENTER_FOLLOW,
        TaskType.RETWEETER_FOLLOW,
        TaskType.QUOTER_FOLLOW,
    )

    /**
     * Account-major deterministic queue.
     * At most 10 active X accounts are accepted for one run.
     * Work for one account finishes before the next account starts.
     */
    fun build(accounts: List<Account>, tasks: List<ScheduledTask>): List<QueueTaskItem> {
        val activeAccounts = accounts
            .asSequence()
            .filter { it.active }
            .distinctBy { it.username.trim().lowercase() }
            .sortedWith(compareBy<Account> { it.id.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.id })
            .take(MAX_ACCOUNTS)
            .toList()

        val allowedIds = activeAccounts.mapTo(hashSetOf()) { it.id }

        val byAccount = tasks
            .asSequence()
            .filter { it.accountId in allowedIds }
            .filter { it.type in supportedTypes }
            .filter { it.status != TaskStatus.COMPLETED }
            .filter { it.progress < it.totalLimit }
            .groupBy { it.accountId }

        return buildList {
            activeAccounts.forEach { account ->
                byAccount[account.id]
                    .orEmpty()
                    .sortedWith(compareBy<ScheduledTask> { it.type.ordinal }.thenBy { it.id })
                    .forEach { task ->
                        add(
                            QueueTaskItem(
                                taskId = task.id,
                                accountId = task.accountId,
                                username = task.username,
                                taskType = task.type,
                                status = QueueItemStatus.PENDING,
                            )
                        )
                    }
            }
        }
    }

    fun nextRunnableIndex(items: List<QueueTaskItem>, afterIndex: Int): Int =
        ((afterIndex + 1) until items.size).firstOrNull { index ->
            items[index].status == QueueItemStatus.PENDING
        } ?: -1
}

/** A queue is never reported as fully successful when work failed or was skipped. */
object QueueResultPolicy {
    fun finalStatus(items: List<QueueTaskItem>): QueueStatus {
        val completed = items.count { it.status == QueueItemStatus.COMPLETED }
        val failed = items.count { it.status == QueueItemStatus.FAILED }
        val skipped = items.count { it.status == QueueItemStatus.SKIPPED }
        return when {
            failed > 0 && completed == 0 -> QueueStatus.FAILED
            failed > 0 || skipped > 0 -> QueueStatus.PARTIAL
            else -> QueueStatus.COMPLETED
        }
    }
}
