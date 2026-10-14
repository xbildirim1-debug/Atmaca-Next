package com.atmacanext.app.ai

import android.content.Context
import java.io.File

/** Pilot transcript. Leaving the tab or the app must not wipe it. */
internal object PilotChatStore {
    data class Line(val mine: Boolean, val text: String)

    private const val FILE_NAME = "pilot_chat.json"
    private const val MAX_LINES = 200

    fun load(context: Context): List<Line> {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return runCatching { decode(file.readText()) }.getOrDefault(emptyList())
    }

    fun save(context: Context, lines: List<Line>) {
        File(context.applicationContext.filesDir, FILE_NAME).writeText(encode(lines))
    }

    fun encode(lines: List<Line>): String = lines.takeLast(MAX_LINES).mapNotNull { line ->
        val text = line.text.trim().replace("\n", "\\n").take(2_000)
        if (text.isBlank()) null else "${if (line.mine) "1" else "0"}\u001f$text"
    }.joinToString("\n")

    fun decode(raw: String): List<Line> = raw.lineSequence().mapNotNull { row ->
        val parts = row.split("\u001f", limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val text = parts[1].replace("\\n", "\n").trim()
        if (text.isBlank()) null else Line(parts[0] == "1", text)
    }.toList()
}
