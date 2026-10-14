package com.atmacanext.app.data.settings

import android.content.Context
import com.atmacanext.app.automation.ReplyTextPoolPolicy

/** Prepared tweets stay on the device and are posted in order, then the list starts over. */
object TweetTextPool {
    private const val PREFS = "atmaca_tweet_pool_v1"
    private const val LINES = "lines"
    private const val CURSOR = "cursor"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lines(context: Context): List<String> =
        ReplyTextPoolPolicy.lines(prefs(context).getString(LINES, "").orEmpty(), maxChars = 280)

    fun count(context: Context): Int = lines(context).size

    fun cursor(context: Context): Int = prefs(context).getInt(CURSOR, 0)

    fun sample(context: Context): String = lines(context).firstOrNull().orEmpty()

    fun replace(context: Context, raw: String): Int {
        val stored = ReplyTextPoolPolicy.lines(raw, maxChars = 280)
        prefs(context).edit().putString(LINES, stored.joinToString("\n")).putInt(CURSOR, 0).apply()
        return stored.size
    }

    fun append(context: Context, raw: String): Int {
        val existing = lines(context)
        val added = ReplyTextPoolPolicy.lines(raw, maxChars = 280).filter { it !in existing }
        val merged = (existing + added).take(500)
        prefs(context).edit().putString(LINES, merged.joinToString("\n")).apply()
        return added.size
    }

    fun advance(context: Context): Int {
        val size = count(context)
        if (size == 0) return 0
        val next = ReplyTextPoolPolicy.slot(cursor(context) + 1, size)
        prefs(context).edit().putInt(CURSOR, next).apply()
        return next
    }
}
