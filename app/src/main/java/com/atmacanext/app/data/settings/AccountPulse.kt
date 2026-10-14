package com.atmacanext.app.data.settings

import android.content.Context
import com.atmacanext.app.automation.AudienceCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccountPulseSnapshot(
    val followers: Long? = null,
    val following: Long? = null,
    val gainedFollowers: Long? = null,
    val tweetsPosted: Int = 0,
)

/** Follower change since the first scan, plus tweets this app has published. */
object AccountPulse {
    private const val PREFS = "atmaca_account_pulse_v1"
    private val revisionState = MutableStateFlow(0)
    val revision = revisionState.asStateFlow()

    fun read(context: Context, username: String?): AccountPulseSnapshot {
        val key = key(username) ?: return AccountPulseSnapshot()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val followers = prefs.getLong("$key:followers", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        val following = prefs.getLong("$key:following", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        val baseline = prefs.getLong("$key:baseline", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        return AccountPulseSnapshot(
            followers = followers,
            following = following,
            gainedFollowers = if (followers != null && baseline != null) followers - baseline else null,
            tweetsPosted = prefs.getInt("$key:tweets", 0),
        )
    }

    fun noteScan(context: Context, username: String?, followersRaw: String?, followingRaw: String?) {
        val key = key(username) ?: return
        val followers = AudienceCount.parse(followersRaw) ?: return
        val following = AudienceCount.parse(followingRaw)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit().putLong("$key:followers", followers)
        if (following != null) edit.putLong("$key:following", following)
        if (!prefs.contains("$key:baseline")) edit.putLong("$key:baseline", followers)
        edit.apply()
        ActivityStats.noteFollowers(context, username, followers)
        revisionState.value = revisionState.value + 1
    }

    fun noteTweet(context: Context, username: String?) {
        val key = key(username) ?: return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = prefs.getInt("$key:tweets", 0) + 1
        prefs.edit().putInt("$key:tweets", next).apply()
        ActivityStats.noteTweet(context, username)
        revisionState.value = revisionState.value + 1
    }

    fun resetDisplay(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit()
        prefs.all.keys.forEach { key ->
            when {
                key.endsWith(":tweets") -> edit.putInt(key, 0)
                key.endsWith(":followers") -> {
                    val user = key.removeSuffix(":followers")
                    edit.putLong("$user:baseline", prefs.getLong(key, 0L))
                }
            }
        }
        edit.apply()
        revisionState.value = revisionState.value + 1
    }

    private fun key(username: String?): String? =
        username?.removePrefix("@")?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
}
