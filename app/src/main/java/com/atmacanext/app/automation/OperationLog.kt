package com.atmacanext.app.automation

import android.util.Log
import com.atmacanext.app.core.AppServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Hafif canlı tanı günlüğü + mevcut Room automation_logs köprüsü. */
object OperationLog {
    private const val TAG = "AtmacaNext"
    private const val MAX_LINES = 250
    private const val DUPLICATE_WINDOW_MS = 2_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines = _lines.asStateFlow()
    private data class PersistRecord(val level: String, val category: String, val message: String)
    private data class DuplicateState(var lastEmittedAt: Long, var suppressed: Int = 0)
    private val duplicateStates = linkedMapOf<String, DuplicateState>()
    private val persistence = Channel<PersistRecord>(
        capacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        scope.launch {
            for (entry in persistence) {
                if (AppServices.ready) {
                    runCatching { AppServices.repository.log(entry.level, entry.category, null, null, entry.message) }
                }
            }
        }
    }

    fun i(category: String, message: String) = record("INFO", category, message)
    fun w(category: String, message: String) = record("WARN", category, message)
    fun e(category: String, message: String) = record("ERROR", category, message)

    fun recordDump(message: String) = record("INFO", "UI_DUMP", message)

    @Synchronized
    private fun record(level: String, category: String, message: String) {
        val now = System.currentTimeMillis()
        val key = "$level|$category|$message"
        val duplicate = duplicateStates[key]
        if (duplicate != null && now - duplicate.lastEmittedAt < DUPLICATE_WINDOW_MS) {
            duplicate.suppressed++
            return
        }
        val decorated = if (duplicate != null && duplicate.suppressed > 0) {
            "$message (önceki tekrarlar birleştirildi: ${duplicate.suppressed})"
        } else message
        duplicateStates[key] = DuplicateState(lastEmittedAt = now)
        if (duplicateStates.size > 128) duplicateStates.remove(duplicateStates.keys.first())
        val line = "$now $level $category · $decorated"
        _lines.value = (_lines.value + line).takeLast(MAX_LINES)
        when (level) {
            "ERROR" -> Log.e(TAG, "$category · $decorated")
            "WARN" -> Log.w(TAG, "$category · $decorated")
            else -> Log.i(TAG, "$category · $decorated")
        }
        persistence.trySend(PersistRecord(level, category, decorated))
    }
}
