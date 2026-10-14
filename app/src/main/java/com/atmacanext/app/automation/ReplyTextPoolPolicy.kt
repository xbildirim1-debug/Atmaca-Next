package com.atmacanext.app.automation

/** One prepared line, a space, then the text the user typed. */
internal object ReplyTextPoolPolicy {
    fun lines(raw: String, maxChars: Int = 240, maxLines: Int = 500): List<String> = raw.replace("\r\n", "\n").replace('\r', '\n')
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(maxLines.coerceIn(1, 10_000))
        .map { it.substring(0, it.offsetByCodePoints(0, minOf(maxChars.coerceIn(1, 280), it.codePointCount(0, it.length)))) }
        .toList()

    fun comment(line: String, addition: String): String {
        val ready = line.trim()
        val extra = addition.trim()
        return when {
            ready.isBlank() -> extra.take(280)
            extra.isBlank() -> ready.take(280)
            else -> "$ready $extra".take(280)
        }
    }

    fun slot(cursor: Int, size: Int): Int = if (size <= 0) 0 else Math.floorMod(cursor, size)
}
