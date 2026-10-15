package com.atmacanext.app.data.settings

import android.content.Context
import com.atmacanext.app.ai.ContentPool
import com.atmacanext.app.automation.ReplyTextPoolPolicy

object ReplyTextPool {
    private val pool = ContentPool.REPLY
    fun lines(context: Context): List<String> = ContentPoolStore.lines(context, pool)
    fun count(context: Context): Int = lines(context).size
    fun cursor(context: Context): Int = ContentPoolStore.cursor(context, pool)
    fun sample(context: Context): String = lines(context).firstOrNull().orEmpty()
    fun replace(context: Context, raw: String): Int = ContentPoolStore.replace(context, pool, raw)
    fun append(context: Context, raw: String): Int = appendBatch(context, raw.lines()).size
    fun appendBatch(context: Context, values: List<String>): List<String> = ContentPoolStore.append(context, pool, values)
    fun advance(context: Context): Int = ContentPoolStore.advance(context, pool)
    fun comment(context: Context, addition: String): String {
        val values = lines(context)
        return if (values.isEmpty()) addition.trim().take(280)
        else ReplyTextPoolPolicy.comment(values[ReplyTextPoolPolicy.slot(cursor(context), values.size)], addition)
    }
}
