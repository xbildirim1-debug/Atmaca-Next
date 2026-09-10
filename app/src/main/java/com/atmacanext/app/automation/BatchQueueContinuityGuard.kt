package com.atmacanext.app.automation

import com.atmacanext.app.data.repository.AtmacaRepository
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Protects unattended multi-item queue continuity.
 *
 * The runtime watchdog only sees a task while an AutomationController session exists.
 * The hand-off after one queue item completes belongs to TaskOrchestrator; if that
 * transition is lost, there may be no live runtime left for the runtime watchdog.
 * This guard watches the queue itself and remembers the exact task ids in the batch.
 */
class BatchQueueContinuityGuard(
    private val repository: AtmacaRepository,
    private val orchestrator: TaskOrchestrator,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var batchTaskIds: List<String> = emptyList()
    private var batchSessionId: String? = null
    private var lastSignature = ""
    private var unchangedSince = 0L
    private var recoveryInProgress = false
    private var recoveryFingerprint = ""
    private var consecutiveRecoveries = 0

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                delay(1_000L)
                if (recoveryInProgress) continue

                val queue = orchestrator.state.value
                val runtime = AutomationController.state.value

                if (queue.isActive && queue.items.size > 1) {
                    val ids = queue.items.map { it.taskId }.distinct()
                    if (queue.sessionId != batchSessionId || ids != batchTaskIds) {
                        batchSessionId = queue.sessionId
                        batchTaskIds = ids
                        lastSignature = ""
                        unchangedSince = System.currentTimeMillis()
                        recoveryFingerprint = ""
                        consecutiveRecoveries = 0
                    }
                }

                if (batchTaskIds.size <= 1) {
                    resetTransitionTimer()
                    continue
                }

                val current = queue.currentItem
                val transitionNeedsWatch = current != null && queue.isActive && when (queue.status) {
                    QueueStatus.BETWEEN_TASKS, QueueStatus.PREPARING -> true
                    QueueStatus.RUNNING -> runtime.taskId != current.taskId || runtime.status in TERMINAL_RUNTIME
                    else -> false
                }

                if (transitionNeedsWatch) {
                    val now = System.currentTimeMillis()
                    val signature = listOf(
                        queue.sessionId.orEmpty(),
                        queue.status.name,
                        queue.currentIndex.toString(),
                        current.taskId,
                        runtime.taskId.orEmpty(),
                        runtime.status.name,
                    ).joinToString("|")
                    if (signature != lastSignature) {
                        lastSignature = signature
                        unchangedSince = now
                    } else if (unchangedSince > 0L && now - unchangedSince >= AutomationStallPolicy.TIMEOUT_MS) {
                        recoverBatch("20 saniye kuyruk geçişi ilerlemedi", current.username, signature)
                    }
                    continue
                }

                resetTransitionTimer()

                // A queue must never silently report completion while one of the exact
                // selected task rows is still unfinished. This covers the device case
                // where account 1 finishes but account 2/3 remain at zero progress.
                if (queue.sessionId == batchSessionId && queue.status in TERMINAL_QUEUE_EXCEPT_STOPPED) {
                    val remaining = unfinishedBatchTasks()
                    if (remaining.isNotEmpty()) {
                        recoverBatch(
                            "Toplu kuyruk erken sona erdi; ${remaining.size} alt iş hâlâ bekliyor",
                            remaining.first().username,
                            "terminal=${queue.status}; currentIndex=${queue.currentIndex}; remaining=${remaining.map { it.id }}",
                        )
                    } else {
                        clearBatch()
                    }
                } else if (queue.status == QueueStatus.STOPPED && !recoveryInProgress) {
                    // STOPPED is an explicit user-visible stop; never undo it automatically.
                    clearBatch()
                }
            }
        }
    }

    private suspend fun unfinishedBatchTasks() = batchTaskIds.mapNotNull { repository.getTaskById(it) }
        .filter { it.progress < it.totalLimit }
        .filter { it.status != TaskStatus.COMPLETED }

    private suspend fun recoverBatch(reason: String, username: String, signature: String) {
        if (recoveryInProgress || batchTaskIds.isEmpty()) return
        recoveryInProgress = true
        try {
            val remainingBeforeStop = unfinishedBatchTasks()
            if (remainingBeforeStop.isEmpty()) {
                clearBatch()
                return
            }

            val fingerprint = batchTaskIds.sorted().joinToString("|")
            if (fingerprint == recoveryFingerprint) consecutiveRecoveries++
            else {
                recoveryFingerprint = fingerprint
                consecutiveRecoveries = 1
            }

            if (consecutiveRecoveries > MAX_CONSECUTIVE_RECOVERIES) {
                repository.log(
                    "ERROR", "QUEUE_CONTINUITY", null, username,
                    "Kuyruk geçişi $MAX_CONSECUTIVE_RECOVERIES otomatik denemeden sonra hâlâ ilerlemedi; sonsuz yeniden başlatma engellendi",
                    "session=$batchSessionId; signature=$signature",
                )
                orchestrator.stop()
                clearBatch()
                return
            }

            repository.log(
                "WARN", "QUEUE_CONTINUITY", null, username,
                "$reason; kalan toplu görev güvenli biçimde yeniden kuruluyor",
                "session=$batchSessionId; attempt=$consecutiveRecoveries/$MAX_CONSECUTIVE_RECOVERIES; signature=$signature",
            )

            orchestrator.stop()
            var wait = 0
            while (orchestrator.state.value.isActive && wait < 30) {
                delay(100L)
                wait++
            }
            delay(300L)

            val remaining = unfinishedBatchTasks()
            if (remaining.isEmpty()) {
                clearBatch()
                return
            }

            // Completed account/task rows are excluded, so recovery cannot repeat
            // work whose postcondition was already verified and persisted.
            orchestrator.startSelection(remaining)
            lastSignature = ""
            unchangedSince = System.currentTimeMillis()
        } finally {
            recoveryInProgress = false
        }
    }

    private fun resetTransitionTimer() {
        lastSignature = ""
        unchangedSince = 0L
    }

    private fun clearBatch() {
        batchTaskIds = emptyList()
        batchSessionId = null
        recoveryFingerprint = ""
        consecutiveRecoveries = 0
        resetTransitionTimer()
    }

    private companion object {
        const val MAX_CONSECUTIVE_RECOVERIES = 3
        val TERMINAL_RUNTIME = setOf(RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED)
        val TERMINAL_QUEUE_EXCEPT_STOPPED = setOf(QueueStatus.COMPLETED, QueueStatus.PARTIAL, QueueStatus.FAILED)
    }
}
