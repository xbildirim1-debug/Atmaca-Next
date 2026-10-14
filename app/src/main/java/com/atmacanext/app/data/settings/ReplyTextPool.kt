package com.atmacanext.app.data.settings

import android.content.Context
import com.atmacanext.app.automation.ReplyTextPoolPolicy

/** Prepared reply lines stay on the device. The cursor is shared by every account. */
object ReplyTextPool {
    private const val PREFS = "atmaca_reply_pool_v1"
    private const val LINES = "lines"
    private const val CURSOR = "cursor"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lines(context: Context): List<String> = ReplyTextPoolPolicy.lines(prefs(context).getString(LINES, "").orEmpty())

    fun count(context: Context): Int = lines(context).size

    fun cursor(context: Context): Int = prefs(context).getInt(CURSOR, 0)

    fun sample(context: Context): String = lines(context).firstOrNull().orEmpty()

    /** Replaces the pool and starts again from the first line. */
    fun replace(context: Context, raw: String): Int {
        val stored = ReplyTextPoolPolicy.lines(raw)
        prefs(context).edit().putString(LINES, stored.joinToString("\n")).putInt(CURSOR, 0).apply()
        return stored.size
    }

    fun comment(context: Context, addition: String): String {
        val stored = lines(context)
        if (stored.isEmpty()) return addition.trim().take(280)
        val line = stored[ReplyTextPoolPolicy.slot(cursor(context), stored.size)]
        return ReplyTextPoolPolicy.comment(line, addition)
    }

    fun advance(context: Context): Int {
        val size = count(context)
        if (size == 0) return 0
        val next = ReplyTextPoolPolicy.slot(cursor(context) + 1, size)
        prefs(context).edit().putInt(CURSOR, next).apply()
        return next
    }
}
