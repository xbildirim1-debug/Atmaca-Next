package com.atmacanext.app.automation

/**
 * Runtime-safe, process-local copy of user tuning persisted in DataStore.
 * Values are bounded by SettingsStore before they reach this object.
 */
object AutomationTuning {
    @Volatile var betweenActionsMs: Long = 500L
    @Volatile var accountSwitchSettleMs: Long = 1_800L
    @Volatile var rateLimitCooldownMs: Long = 30L * 60_000L

    /**
     * The user's action interval is also the global runtime speed control.
     * 500 ms preserves the authored timings; 100 ms runs UI steps at 20%.
     * Keep a small floor so Android can publish the next accessibility tree.
     */
    fun scaleDelay(nominalMs: Long): Long =
        (nominalMs * betweenActionsMs / 500L).coerceIn(60L, 60_000L)
}
