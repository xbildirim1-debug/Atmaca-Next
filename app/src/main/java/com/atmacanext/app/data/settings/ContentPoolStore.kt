package com.atmacanext.app.data.settings

import android.content.Context
import com.atmacanext.app.ai.ContentPool
import com.atmacanext.app.ai.PilotPoolPolicy
import com.atmacanext.app.ai.TweetPoolPolicy
import com.atmacanext.app.automation.ReplyTextPoolPolicy
import org.json.JSONObject

/** Migrate legacy pools without changing their contents; keep transfers atomic in a single file. */
object ContentPoolStore {
    private fun prefs(context: Context): android.content.SharedPreferences {
        val app = context.applicationContext
        val p = app.getSharedPreferences("atmaca_content_pools_v2767", Context.MODE_PRIVATE)
        if (!p.getBoolean("initialized", false)) {
            val tweet = app.getSharedPreferences("atmaca_tweet_pool_v1", Context.MODE_PRIVATE)
            val reply = app.getSharedPreferences("atmaca_reply_pool_v1", Context.MODE_PRIVATE)
            check(p.edit().putString("TWEET.lines", tweet.getString("lines", ""))
                .putInt("TWEET.cursor", tweet.getInt("cursor", 0))
                .putString("REPLY.lines", reply.getString("lines", ""))
                .putInt("REPLY.cursor", reply.getInt("cursor", 0)).putBoolean("initialized", true).commit()) {
                "Metin havuzları kaydedilemedi; eski kayıtlar korundu"
            }
        }
        return p
    }
    @Synchronized fun lines(context: Context, pool: ContentPool): List<String> =
        ReplyTextPoolPolicy.lines(prefs(context).getString("${pool.name}.lines", "").orEmpty(), 280, TweetPoolPolicy.MAX_LINES)
    @Synchronized fun cursor(context: Context, pool: ContentPool): Int = prefs(context).getInt("${pool.name}.cursor", 0)
    @Synchronized fun replace(context: Context, pool: ContentPool, raw: String): Int {
        val values = ReplyTextPoolPolicy.lines(raw, 280, TweetPoolPolicy.MAX_LINES)
        val p = prefs(context)
        check(p.edit().putString("${pool.name}.lines", values.joinToString("\n")).putInt("${pool.name}.cursor", 0)
            .remove("undo").putLong("revision", p.getLong("revision", 0) + 1).commit()) { "Havuz kaydedilemedi" }
        return values.size
    }
    @Synchronized fun append(context: Context, pool: ContentPool, values: List<String>): List<String> {
        val p = prefs(context); val merged = TweetPoolPolicy.merge(lines(context, pool), values)
        if (merged.added.isEmpty()) return emptyList()
        check(p.edit().putString("${pool.name}.lines", merged.lines.joinToString("\n"))
            .remove("undo").putLong("revision", p.getLong("revision", 0) + 1).commit()) { "Metinler kaydedilemedi" }
        return merged.added
    }
    @Synchronized fun advance(context: Context, pool: ContentPool): Int {
        val size = lines(context, pool).size; val next = ReplyTextPoolPolicy.slot(cursor(context, pool) + 1, size)
        val p = prefs(context)
        check(p.edit().putInt("${pool.name}.cursor", next).remove("undo").putLong("revision", p.getLong("revision", 0) + 1).commit()) { "Havuz sırası kaydedilemedi" }
        return next
    }
    @Synchronized fun edit(context: Context, source: ContentPool, destination: ContentPool?, count: Int, copy: Boolean = false): String {
        require(destination != source) { "Kaynak ve hedef havuz aynı" }
        val p = prefs(context); val from = lines(context, source)
        val to = destination?.let { lines(context, it) }.orEmpty()
        val edit = destination?.let { PilotPoolPolicy.transfer(from, to, count, copy) }
        val after = edit?.source ?: PilotPoolPolicy.deleteLast(from, count)
        val backup = JSONObject().put("pool", source.name).put("source", from.joinToString("\n"))
            .put("cursor", cursor(context, source)).put("after", after.joinToString("\n"))
        val writer = p.edit().putString("${source.name}.lines", after.joinToString("\n"))
            .putInt("${source.name}.cursor", PilotPoolPolicy.cursor(cursor(context, source), after.size))
        if (destination != null && edit != null) {
            writer.putString("${destination.name}.lines", edit.destination.joinToString("\n"))
            backup.put("destinationPool", destination.name).put("destination", to.joinToString("\n"))
                .put("destinationAfter", edit.destination.joinToString("\n"))
        }
        check(writer.putString("undo", backup.toString()).putLong("revision", p.getLong("revision", 0) + 1).commit()) {
            "Havuz işlemi kaydedilemedi"
        }
        return if (destination == null) "${source.title}: son $count metin silindi."
        else "$count metin ${source.title} havuzundan ${destination.title} havuzuna ${if (copy) "kopyalandı" else "taşındı"}. ${edit!!.added} yeni metin eklendi; ${count - edit.added} metin hedefte zaten vardı."
    }
    @Synchronized fun undo(context: Context): String {
        val p = prefs(context); val raw = p.getString("undo", null) ?: error("Geri alınabilecek son havuz işlemi yok")
        val b = JSONObject(raw); val source = ContentPool.valueOf(b.getString("pool"))
        require(lines(context, source).joinToString("\n") == b.getString("after")) { "Kaynak değişmiş; yeni metinlerin üzerine yazılmadı" }
        val destination = b.optString("destinationPool").takeIf(String::isNotBlank)?.let(ContentPool::valueOf)
        if (destination != null) require(lines(context, destination).joinToString("\n") == b.getString("destinationAfter")) {
            "Hedef değişmiş; yeni metinlerin üzerine yazılmadı"
        }
        val writer = p.edit().putString("${source.name}.lines", b.getString("source")).putInt("${source.name}.cursor", b.getInt("cursor"))
        destination?.let { writer.putString("${it.name}.lines", b.getString("destination")) }
        check(writer.remove("undo").putLong("revision", p.getLong("revision", 0) + 1).commit()) { "Geri alma kaydedilemedi" }
        return "Son havuz işlemi geri alındı; önceki metinler ve sıra korundu."
    }
}
