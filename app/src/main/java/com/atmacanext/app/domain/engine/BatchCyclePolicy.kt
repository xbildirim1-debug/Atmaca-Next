package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.QueueItemStatus
import com.atmacanext.app.domain.model.QueueTaskItem

/** Only a completed round with remaining work participates in the next batch round. */
object BatchCyclePolicy {
    fun remainingLabel(deadline: Long, now: Long): String {
        val remainingSeconds = ((deadline - now).coerceAtLeast(0L) + 999L) / 1_000L
        return "Sonraki döngüye ${remainingSeconds / 60} dakika ${remainingSeconds % 60} saniye kaldı"
    }

    fun nextRoundIndices(items: List<QueueTaskItem>, deferredTaskIds: Set<String>): List<Int> =
        items.indices.filter { items[it].taskId in deferredTaskIds && items[it].status == QueueItemStatus.PAUSED }
}
