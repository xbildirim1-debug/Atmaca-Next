package com.atmacanext.app.automation

import com.atmacanext.app.data.repository.AtmacaRepository
import com.atmacanext.app.domain.model.QueueStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Protects the gap between two queue items. Runtime watchdogs cannot see this gap
 * because the previous runtime may already be terminal while the next account has
 * not started yet.
 *
 * If the same queue hand-off state remains unchanged for 20 seconds, the guard
 * checkpoints through the repository-backed task rows, stops the stale queue and
 * starts a fresh queue containing only unfinished selected task rows.
 */
class BatchQueueContinuityGuard(
    private val repository: AtmacaRepository,
    private val orchestrator: TaskOrchestrator,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
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
                val current = queue.currentItem
                val transitionNeedsWatch = current != null && queue.isActive && when (queue.status) {
                    QueueStatus.BETWEEN_TASKS, QueueStatus.PREPARING -> true
                    QueueStatus.RUNNING -> runtime.taskId != current.taskId || runtime.status in TERMINAL_RUNTIME
                    else -> false
                }

                if (!transitionNeedsWatch) {
                    lastSignature = ""
                    unchangedSince = 0L
                    if (!queue.isActive) {
                        recoveryFingerprint = ""
                        consecutiveRecoveries = 0
                    }
                    continue
                }

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
                    continue
                }
                if (unchangedSince == 0L || now - unchangedSince < AutomationStallPolicy.TIMEOUT_MS) continue

                recover(queue.items.map { it.taskId }.distinct(), queue.sessionId, current.username, signature)
            }
        }
    }

    private suspend fun recover(selectedTaskIds: List<String>, sessionId: String?, username: String, signature: String) {
        if (recoveryInProgress || selectedTaskIds.isEmpty()) return
        recoveryInProgress = true
        try {
            val fingerprint = selectedTaskIds.sorted().joinToString("|")
            if (fingerprint == recoveryFingerprint) consecutiveRecoveries++
            else {
                recoveryFingerprint = fingerprint
                consecutiveRecoveries = 1
            }

            if (consecutiveRecoveries > MAX_CONSECUTIVE_RECOVERIES) {
                repository.log(
                    "ERROR", "QUEUE_CONTINUITY", null, username,
                    "Kuyruk geçişi $MAX_CONSECUTIVE_RECOVERIES otomatik denemeden sonra hâlâ ilerlemedi; sonsuz yeniden başlatma engellendi",
                    "session=$sessionId; signature=$signature",
                )
                orchestrator.stop()
                return
            }

            repository.log(
                "WARN", "QUEUE_CONTINUITY", null, username,
                "20 saniye kuyruk ilerlemesi yok; seçili toplu görev kalan alt işlerden yeniden kuruluyor",
                "session=$sessionId; attempt=$consecutiveRecoveries/$MAX_CONSECUTIVE_RECOVERIES; signature=$signature",
            )

            orchestrator.stop()
            var wait = 0
            while (orchestrator.state.value.isActive && wait < 30) {
                delay(100L)
                wait++
            }
            delay(250L)

            val remaining = selectedTaskIds.mapNotNull { repository.getTaskById(it) }
                .filter { it.progress < it.totalLimit }
                .filter { it.status != com.atmacanext.app.domain.model.TaskStatus.COMPLETED }

            if (remaining.isEmpty()) {
                repository.log("INFO", "QUEUE_CONTINUITY", null, username, "Kuyruk kurtarmasında bekleyen alt iş kalmadı")
                return
            }

            orchestrator.startSelection(remaining)
            lastSignature = ""
            unchangedSince = System.currentTimeMillis()
        } finally {
            recoveryInProgress = false
        }
    }

    private companion object {
        const val MAX_CONSECUTIVE_RECOVERIES = 3
        val TERMINAL_RUNTIME = setOf(RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED)
    }
}
