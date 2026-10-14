package com.atmacanext.app.automation

/** Tweets already followed from, kept only for the local calendar day. */
internal object FollowedTweetDayPolicy {
    const val LIMIT = 400

    fun add(existing: List<String>, day: String, key: String): List<String> {
        if (day.isBlank() || key.isBlank()) return existing.filter { it.substringBefore('\t') == day }
        val kept = existing.filter { line ->
            val stamp = line.substringBefore('\t')
            val stored = line.substringAfter('\t', "")
            stamp == day && stored.isNotBlank() && stored != key
        }
        return (kept + "$day\t$key").takeLast(LIMIT)
    }

    fun keysFor(existing: List<String>, day: String): Set<String> {
        if (day.isBlank()) return emptySet()
        return existing.mapNotNull { line ->
            val stamp = line.substringBefore('\t')
            val stored = line.substringAfter('\t', "")
            if (stamp == day && stored.isNotBlank()) stored else null
        }.toSet()
    }
}
