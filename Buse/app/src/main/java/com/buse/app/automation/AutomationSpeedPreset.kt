package com.buse.app.automation

/** One upgrade applies the Buse 1.1 preset, 10x faster than Buse 1.0; subsequent manual timing choices persist. */
internal object AutomationSpeedPreset {
    const val ACTION_MS = 7L
    const val ACCOUNT_MS = 25L
    const val TASK_MS = 21L
    data class Timing(val action: Long, val account: Long, val task: Long)

    fun resolve(savedAfterUpgrade: Boolean, action: Long?, account: Long?, task: Long?): Timing =
        if (!savedAfterUpgrade) Timing(ACTION_MS, ACCOUNT_MS, TASK_MS)
        else Timing((action ?: ACTION_MS).coerceIn(ACTION_MS, 15_000L),
            (account ?: ACCOUNT_MS).coerceIn(ACCOUNT_MS, 15_000L), (task ?: TASK_MS).coerceIn(TASK_MS, 60_000L))
}
