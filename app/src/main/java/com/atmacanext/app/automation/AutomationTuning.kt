package com.atmacanext.app.automation

/**
 * Runtime-safe, process-local copy of user tuning persisted in DataStore.
 * Values are bounded by SettingsStore before they reach this object.
 */
object AutomationTuning {
    @Volatile var betweenActionsMs: Long = AutomationSpeedPreset.ACTION_MS
    @Volatile var accountSwitchSettleMs: Long = AutomationSpeedPreset.ACCOUNT_MS
    @Volatile var rateLimitCooldownMs: Long = 30L * 60_000L

    /**
     * The user's action interval is also the global runtime speed control.
     * 500 ms preserves authored timings. The 71 ms preset divides UI delays by 7.
     * Every scaled delay is then divided by 10 so a saved slow interval does not
     * leave multi-second gaps between profile scans. Floor stays at 16 ms.
     */
    fun scaleDelay(nominalMs: Long): Long {
        val value = betweenActionsMs.coerceIn(AutomationSpeedPreset.ACTION_MS, 15_000L)
        val scaled = if (value == AutomationSpeedPreset.ACTION_MS) nominalMs / 7L
            else nominalMs * value / 500L
        return (scaled / 10L).coerceIn(16L, 60_000L)
    }
}
