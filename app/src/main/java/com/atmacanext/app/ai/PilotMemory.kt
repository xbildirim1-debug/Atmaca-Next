package com.atmacanext.app.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local memory for the director. Survives process death.
 * Stores recent user intents, preferred accounts, and last recovery so the
 * next turn does not start from zero.
 */
internal object PilotMemory {
    private const val PREFS = "atmaca_pilot_memory"
    private const val KEY_FACTS = "facts"
    private const val MAX = 24

    fun remember(context: Context, fact: String) {
        val clean = fact.trim().take(180)
        if (clean.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = load(prefs).toMutableList()
        existing.remove(clean)
        existing.add(0, clean)
        while (existing.size > MAX) existing.removeAt(existing.lastIndex)
        prefs.edit().putString(KEY_FACTS, JSONArray(existing).toString()).apply()
    }

    fun facts(context: Context): List<String> = load(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    fun summary(context: Context): String = facts(context).take(6).joinToString("; ").ifBlank { "hafıza boş" }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun load(prefs: android.content.SharedPreferences): List<String> {
        val raw = prefs.getString(KEY_FACTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        }.getOrDefault(emptyList())
    }
}
