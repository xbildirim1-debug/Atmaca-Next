package com.atmacanext.app.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Incident memory for the system-admin half of Pilot.
 * Records stalls by screen+stage, counts consecutive repeats, and remembers
 * which recovery worked so the next stall can escalate instead of looping.
 */
internal object PilotIncident {
    private const val PREFS = "atmaca_pilot_incidents"
    private const val KEY_ROWS = "rows"
    private const val MAX = 120

    data class Row(
        val screen: String,
        val stage: String,
        val account: String,
        val recovery: String,
        val at: Long,
        val streak: Int,
        val failed: Boolean = false,
    )

    fun record(context: Context, screen: String, stage: String, account: String, recovery: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rows = load(prefs).toMutableList()
        val last = rows.firstOrNull()
        val same = last != null && last.screen == screen && last.stage == stage
        if (same && last != null && !last.failed) {
            rows[0] = last.copy(failed = true)
        }
        val streak = if (same && last != null) last.streak + 1 else 1
        rows.add(0, Row(screen, stage, account, recovery, System.currentTimeMillis(), streak))
        while (rows.size > MAX) rows.removeAt(rows.lastIndex)
        prefs.edit().putString(KEY_ROWS, toJson(rows)).apply()
    }

    fun markSucceeded(context: Context, screen: String, stage: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rows = load(prefs).toMutableList()
        val index = rows.indexOfFirst { it.screen == screen && it.stage == stage && !it.failed }
        if (index < 0) return
        rows[index] = rows[index].copy(failed = false, recovery = rows[index].recovery + ":ok")
        prefs.edit().putString(KEY_ROWS, toJson(rows)).apply()
    }

    fun failedRecoveries(context: Context, screen: String, stage: String): Int =
        recent(context).count { it.screen == screen && it.stage == stage && it.failed }

    fun lastFailedRecovery(context: Context, screen: String, stage: String): String? =
        recent(context).firstOrNull { it.screen == screen && it.stage == stage && it.failed }?.recovery

    fun byLocation(context: Context): List<Triple<String, Int, Int>> {
        val grouped = recent(context).groupBy { "${it.screen} / ${it.stage}" }
        return grouped.map { (where, rows) -> Triple(where, rows.size, rows.count { it.failed }) }
            .sortedByDescending { it.second }
    }

    fun recent(context: Context): List<Row> = load(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    /** Top consecutive-error locations, most frequent first. */
    fun hotspots(context: Context, limit: Int = 8): List<Pair<String, Int>> {
        val counts = linkedMapOf<String, Int>()
        for (row in recent(context)) {
            val key = "${row.screen} / ${row.stage}"
            counts[key] = (counts[key] ?: 0) + 1
        }
        return counts.entries.sortedByDescending { it.value }.take(limit).map { it.key to it.value }
    }

    /** Longest consecutive streak currently recorded for any location. */
    fun longestStreak(context: Context): Pair<String, Int>? {
        val best = recent(context).maxByOrNull { it.streak } ?: return null
        return "${best.screen} / ${best.stage}" to best.streak
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun load(prefs: android.content.SharedPreferences): List<Row> {
        val raw = prefs.getString(KEY_ROWS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                Row(
                    o.optString("screen"),
                    o.optString("stage"),
                    o.optString("account"),
                    o.optString("recovery"),
                    o.optLong("at"),
                    o.optInt("streak", 1),
                    o.optBoolean("failed", false),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun toJson(rows: List<Row>): String {
        val array = JSONArray()
        rows.forEach { row ->
            array.put(JSONObject().apply {
                put("screen", row.screen)
                put("stage", row.stage)
                put("account", row.account)
                put("recovery", row.recovery)
                put("at", row.at)
                put("streak", row.streak)
                put("failed", row.failed)
            })
        }
        return array.toString()
    }
}
