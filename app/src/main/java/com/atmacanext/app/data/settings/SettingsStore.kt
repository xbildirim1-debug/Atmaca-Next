package com.atmacanext.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.atmacaDataStore by preferencesDataStore(name = "atmaca_settings")

data class AppSettings(
    val defaultFollowLimit: Int = 35,
    val defaultUnfollowLimit: Int = 35,
    val dailyFollowLimitPerAccount: Int = 35,
    val dailyUnfollowLimitPerAccount: Int = 35,
    val keepLogDays: Int = 14,
    val betweenActionsMs: Long = 500L,
    val accountSwitchSettleMs: Long = 1_800L,
    val betweenTasksMs: Long = 1_500L,
    val continueAfterFailedTask: Boolean = true,
    val rateLimitCooldownMinutes: Int = 30,
    val minimumTweetAgeMinutes: Int = 120,
    val tweetsPerTarget: Int = 5,
    val geminiModel: String = "auto",
    val setupAcknowledged: Boolean = false,
    val lastReportPath: String? = null,
    val lastReportAt: Long? = null,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val defaultFollowLimit = intPreferencesKey("default_follow_limit")
        val defaultUnfollowLimit = intPreferencesKey("default_unfollow_limit")
        val dailyFollowLimitPerAccount = intPreferencesKey("daily_follow_limit_per_account")
        val dailyUnfollowLimitPerAccount = intPreferencesKey("daily_unfollow_limit_per_account")
        val keepLogDays = intPreferencesKey("keep_log_days")
        val betweenActionsMs = longPreferencesKey("between_actions_ms")
        val accountSwitchSettleMs = longPreferencesKey("account_switch_settle_ms")
        val betweenTasksMs = longPreferencesKey("between_tasks_ms")
        val continueAfterFailedTask = booleanPreferencesKey("continue_after_failed_task")
        val rateLimitCooldownMinutes = intPreferencesKey("rate_limit_cooldown_minutes")
        val minimumTweetAgeMinutes = intPreferencesKey("minimum_tweet_age_minutes")
        val tweetsPerTarget = intPreferencesKey("tweets_per_target")
        val geminiModel = stringPreferencesKey("gemini_model")
        val setupAcknowledged = booleanPreferencesKey("setup_acknowledged")
        val lastReportPath = stringPreferencesKey("last_report_path")
        val lastReportAt = longPreferencesKey("last_report_at")
    }

    val settings: Flow<AppSettings> = context.atmacaDataStore.data.map { p ->
        AppSettings(
            defaultFollowLimit = (p[Keys.defaultFollowLimit] ?: 35).coerceIn(1, 100),
            defaultUnfollowLimit = (p[Keys.defaultUnfollowLimit] ?: 35).coerceIn(1, 100),
            dailyFollowLimitPerAccount = (p[Keys.dailyFollowLimitPerAccount] ?: 35).coerceIn(1, 100),
            dailyUnfollowLimitPerAccount = (p[Keys.dailyUnfollowLimitPerAccount] ?: 35).coerceIn(1, 100),
            keepLogDays = (p[Keys.keepLogDays] ?: 14).coerceIn(1, 365),
            betweenActionsMs = (p[Keys.betweenActionsMs] ?: 500L).coerceIn(500L, 15_000L),
            accountSwitchSettleMs = (p[Keys.accountSwitchSettleMs] ?: 1_800L).coerceIn(1_500L, 15_000L),
            betweenTasksMs = (p[Keys.betweenTasksMs] ?: 1_500L).coerceIn(1_000L, 60_000L),
            continueAfterFailedTask = p[Keys.continueAfterFailedTask] ?: true,
            rateLimitCooldownMinutes = (p[Keys.rateLimitCooldownMinutes] ?: 30).coerceIn(5, 180),
            minimumTweetAgeMinutes = (p[Keys.minimumTweetAgeMinutes] ?: 120).coerceIn(120, 1_440),
            tweetsPerTarget = (p[Keys.tweetsPerTarget] ?: 5).coerceIn(1, 5),
            geminiModel = p[Keys.geminiModel]?.trim().orEmpty().ifBlank { "auto" },
            setupAcknowledged = p[Keys.setupAcknowledged] ?: false,
            lastReportPath = p[Keys.lastReportPath],
            lastReportAt = p[Keys.lastReportAt],
        )
    }

    suspend fun setLimits(follow: Int, unfollow: Int) {
        context.atmacaDataStore.edit { p ->
            p[Keys.defaultFollowLimit] = follow.coerceIn(1, 100)
            p[Keys.defaultUnfollowLimit] = unfollow.coerceIn(1, 100)
        }
    }

    suspend fun setDailyAccountLimits(follow: Int, unfollow: Int) {
        context.atmacaDataStore.edit { p ->
            p[Keys.dailyFollowLimitPerAccount] = follow.coerceIn(1, 100)
            p[Keys.dailyUnfollowLimitPerAccount] = unfollow.coerceIn(1, 100)
        }
    }

    suspend fun setAutomationTiming(betweenActionsMs: Long, accountSwitchSettleMs: Long, cooldownMinutes: Int, betweenTasksMs: Long = 1_500L) {
        context.atmacaDataStore.edit { p ->
            p[Keys.betweenActionsMs] = betweenActionsMs.coerceIn(500L, 15_000L)
            p[Keys.accountSwitchSettleMs] = accountSwitchSettleMs.coerceIn(1_500L, 15_000L)
            p[Keys.betweenTasksMs] = betweenTasksMs.coerceIn(1_000L, 60_000L)
            p[Keys.rateLimitCooldownMinutes] = cooldownMinutes.coerceIn(5, 180)
        }
    }

    suspend fun setContinueAfterFailedTask(enabled: Boolean) {
        context.atmacaDataStore.edit { it[Keys.continueAfterFailedTask] = enabled }
    }

    suspend fun setDiscoveryRules(minimumAgeMinutes: Int = 120, tweetsPerTarget: Int = 5) {
        context.atmacaDataStore.edit { p ->
            p[Keys.minimumTweetAgeMinutes] = minimumAgeMinutes.coerceIn(120, 1_440)
            p[Keys.tweetsPerTarget] = tweetsPerTarget.coerceIn(1, 5)
        }
    }

    suspend fun setGeminiModel(model: String) {
        context.atmacaDataStore.edit { it[Keys.geminiModel] = model.trim().take(80).ifBlank { "auto" } }
    }

    suspend fun acknowledgeSetup() {
        context.atmacaDataStore.edit { it[Keys.setupAcknowledged] = true }
    }

    suspend fun setKeepLogDays(days: Int) {
        context.atmacaDataStore.edit { it[Keys.keepLogDays] = days.coerceIn(1, 365) }
    }

    suspend fun recordReport(path: String, at: Long = System.currentTimeMillis()) {
        context.atmacaDataStore.edit { p ->
            p[Keys.lastReportPath] = path
            p[Keys.lastReportAt] = at
        }
    }
}
