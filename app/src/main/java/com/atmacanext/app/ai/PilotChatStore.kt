package com.atmacanext.app.ai

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Pilot transcript. Leaving the tab or the app must not wipe it. */
internal object PilotChatStore {
    data class Line(val mine: Boolean, val text: String, val imagePath: String? = null)

    private const val FILE_NAME = "pilot_chat.json"
    private const val MAX_LINES = 200

    fun load(context: Context): List<Line> {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return runCatching { decode(file.readText()) }.getOrDefault(emptyList())
    }

    fun save(context: Context, lines: List<Line>) {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        temp.writeText(encode(lines))
        check(temp.renameTo(file)) { "Pilot sohbeti kaydedilemedi" }
    }

    fun encode(lines: List<Line>): String = JSONArray().apply {
        lines.takeLast(MAX_LINES).filter { it.text.isNotBlank() || it.imagePath != null }.forEach { line ->
            put(JSONObject().put("mine", line.mine).put("text", line.text.trim().take(6_000))
                .put("image", line.imagePath ?: JSONObject.NULL))
        }
    }.toString()

    fun decode(raw: String): List<Line> {
        if (raw.trimStart().startsWith("[")) {
            val values = JSONArray(raw)
            return (0 until values.length()).mapNotNull { i ->
                val row = values.optJSONObject(i) ?: return@mapNotNull null
                val image = row.optString("image").takeIf { it.isNotBlank() && it != "null" }
                val text = row.optString("text")
                if (text.isBlank() && image == null) null else Line(row.optBoolean("mine"), text, image)
            }.takeLast(MAX_LINES)
        }
        return raw.lineSequence().mapNotNull { row ->
        val parts = row.split("\u001f", limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val text = parts[1].replace("\\n", "\n").trim()
        if (text.isBlank()) null else Line(parts[0] == "1", text)
        }.toList()
    }
}
