package com.buse.app.automation

/** One upgrade applies the requested 7x preset; subsequent manual timing choices persist. */
internal object AutomationSpeedPreset {
    const val ACTION_MS = 71L
    const val ACCOUNT_MS = 257L
    const val TASK_MS = 214L
    data class Timing(val action: Long, val account: Long, val task: Long)

    fun resolve(savedAfterUpgrade: Boolean, action: Long?, account: Long?, task: Long?): Timing =
        if (!savedAfterUpgrade) Timing(ACTION_MS, ACCOUNT_MS, TASK_MS)
        else Timing((action ?: ACTION_MS).coerceIn(ACTION_MS, 15_000L),
            (account ?: ACCOUNT_MS).coerceIn(100L, 15_000L), (task ?: TASK_MS).coerceIn(100L, 60_000L))
}
