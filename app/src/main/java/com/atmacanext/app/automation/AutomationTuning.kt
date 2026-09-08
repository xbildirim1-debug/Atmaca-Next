package com.atmacanext.app.automation

/**
 * Runtime-safe, process-local copy of user tuning persisted in DataStore.
 * Values are bounded by SettingsStore before they reach this object.
 */
object AutomationTuning {
    @Volatile var betweenActionsMs: Long = 500L
    @Volatile var accountSwitchSettleMs: Long = 1_800L
    @Volatile var rateLimitCooldownMs: Long = 30L * 60_000L
}
