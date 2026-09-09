package com.atmacanext.app.automation

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

internal object TweetAgeEvidence {
    fun detailMinutes(nodes: List<NodeSnapshot>, now: Long): Long? = nodes.filter { it.visible && !it.editable }
        .sortedBy { it.bounds.top }.firstNotNullOfOrNull { n ->
            listOfNotNull(n.text, n.contentDescription).firstNotNullOfOrNull { label -> parseDetailTime(label, now) }
        }

    fun parseDetailTime(raw: String, now: Long): Long? {
        val s = raw.substringBefore("Görüntü").substringBefore("görüntü").substringBefore("Views").substringBefore("views")
        val match = Regex("^(\\d{1,2}:\\d{2}(?:\\s*[APap][Mm])?)\\s*[·•]\\s*(\\d{1,2}\\s+[^\\s·•]+\\s+\\d{2,4})(?:\\s*[·•].*)?$").matchEntire(s.trim()) ?: return null
        val text = "${match.groupValues[1]} ${match.groupValues[2]}"
        for (locale in listOf(Locale("tr", "TR"), Locale.US)) for (pattern in listOf("HH:mm d MMM yy", "HH:mm d MMM yyyy", "h:mm a d MMM yy", "h:mm a d MMM yyyy")) {
            val date = runCatching { LocalDateTime.parse(text, DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(locale)) }.getOrNull() ?: continue
            val millis = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return ((now - millis) / 60_000L).coerceAtLeast(0L)
        }
        return null
    }
}
