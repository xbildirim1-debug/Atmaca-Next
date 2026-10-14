package com.atmacanext.app.automation

import android.content.Context

/** Durable "this account already replied to this tweet" list. Not cleared when a task resets. */
object CommentedTweetMemory {
    private const val PREFS = "commented_tweets_v1"

    fun keys(context: Context, username: String?): Set<String> {
        val account = accountKey(username) ?: return emptySet()
        return context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(account, "").orEmpty().lineSequence().filter(String::isNotBlank).toSet()
    }

    fun remember(context: Context, username: String?, key: String?) {
        val account = accountKey(username) ?: return
        if (key.isNullOrBlank()) return
        val next = CommentedTweetPolicy.add(keys(context, username).toList(), key)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(account, next.joinToString("\n")).apply()
    }

    private fun accountKey(username: String?): String? =
        username?.removePrefix("@")?.trim()?.lowercase()?.takeIf(String::isNotBlank)
}
