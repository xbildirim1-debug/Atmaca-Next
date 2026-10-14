package com.atmacanext.app.automation

/** Per-account memory of tweets this account already commented on. Survives a new task. */
internal object CommentedTweetPolicy {
    const val LIMIT = 500

    fun add(existing: List<String>, key: String): List<String> {
        if (key.isBlank()) return existing
        return (existing.filterNot { it == key } + key).takeLast(LIMIT)
    }

    fun contains(existing: Collection<String>, key: String): Boolean = key.isNotBlank() && key in existing
}
