package com.atmacanext.app.automation

/** Reads follower counts as X shows them: 1.234, 43,5 B, 12.4K. */
internal object AudienceCount {
    fun parse(raw: String?): Long? {
        if (raw.isNullOrBlank() || raw == "—") return null
        val text = raw.lowercase().replace(" ", "").replace("\u00a0", "")
        val match = Regex("""(\d{1,3}(?:\.\d{3})+|\d+(?:[.,]\d+)?)([a-zçğıöşü]*)""").find(text) ?: return null
        val token = match.groupValues[1]
        val unit = match.groupValues[2]
        val grouped = Regex("""\d{1,3}(\.\d{3})+""").matches(token)
        val number = when {
            grouped -> token.replace(".", "").toDoubleOrNull()
            token.contains(',') -> token.replace(',', '.').toDoubleOrNull()
            else -> token.toDoubleOrNull()
        } ?: return null
        val scale = when (unit) {
            "k", "b", "bin" -> 1_000.0
            "m", "mn", "milyon" -> 1_000_000.0
            else -> 1.0
        }
        return (number * scale).toLong().takeIf { it >= 0 }
    }
}
