package com.atmacanext.app.domain.model

enum class QueueStatus {
    IDLE,
    PREPARING,
    RUNNING,
    BETWEEN_TASKS,
    PAUSED,
    COMPLETED,
    PARTIAL,
    FAILED,
    STOPPED,
}

enum class QueueItemStatus {
    PENDING,
    RUNNING,
    PAUSED,
    COMPLETED,
    SKIPPED,
    FAILED,
}

data class QueueTaskItem(
    val taskId: String,
    val accountId: String,
    val username: String,
    val taskType: TaskType,
    val status: QueueItemStatus = QueueItemStatus.PENDING,
    val note: String? = null,
)

data class AutomationQueueState(
    val sessionId: String? = null,
    val status: QueueStatus = QueueStatus.IDLE,
    val items: List<QueueTaskItem> = emptyList(),
    val currentIndex: Int = -1,
    val message: String = "Çoklu hesap kuyruğu hazır",
    val startedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val currentItem: QueueTaskItem?
        get() = items.getOrNull(currentIndex)

    val completedCount: Int
        get() = items.count { it.status == QueueItemStatus.COMPLETED }

    val skippedCount: Int
        get() = items.count { it.status == QueueItemStatus.SKIPPED }

    val failedCount: Int
        get() = items.count { it.status == QueueItemStatus.FAILED }

    val pendingCount: Int
        get() = items.count { it.status in setOf(QueueItemStatus.PENDING, QueueItemStatus.PAUSED, QueueItemStatus.RUNNING) }

    val isActive: Boolean
        get() = status in setOf(QueueStatus.PREPARING, QueueStatus.RUNNING, QueueStatus.BETWEEN_TASKS, QueueStatus.PAUSED)
}
