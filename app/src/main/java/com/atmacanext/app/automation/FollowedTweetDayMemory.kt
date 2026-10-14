package com.atmacanext.app.automation

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One shared daily list. A tweet that was entered and followed is not opened again that day. */
object FollowedTweetDayMemory {
    private const val PREFS = "followed_tweets_day_v1"
    private const val ENTRY = "day"
    private val dayFormat = DateTimeFormatter.ISO_LOCAL_DATE

    fun day(now: Long = System.currentTimeMillis()): String =
        Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().format(dayFormat)

    fun keys(context: Context, now: Long = System.currentTimeMillis()): Set<String> {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ENTRY, "").orEmpty().lineSequence().filter(String::isNotBlank).toList()
        return FollowedTweetDayPolicy.keysFor(raw, day(now))
    }

    fun remember(context: Context, key: String?, now: Long = System.currentTimeMillis()) {
        if (key.isNullOrBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(ENTRY, "").orEmpty().lineSequence().filter(String::isNotBlank).toList()
        val next = FollowedTweetDayPolicy.add(raw, day(now), key)
        prefs.edit().putString(ENTRY, next.joinToString("\n")).apply()
    }
}
