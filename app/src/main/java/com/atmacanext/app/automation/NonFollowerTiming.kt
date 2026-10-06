package com.atmacanext.app.automation

/** Preserve Buse's working timing for the new task; existing Atmaca tuning stays in effect elsewhere. */
internal object NonFollowerTiming {
    const val ACTION_MS = 7L
    fun scaleDelay(nominalMs: Long, enabled: Boolean): Long =
        if (enabled) (nominalMs / 70L).coerceIn(16L, 60_000L)
        else AutomationTuning.scaleDelay(nominalMs)
}
