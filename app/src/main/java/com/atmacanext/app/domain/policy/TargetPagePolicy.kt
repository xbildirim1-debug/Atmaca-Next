package com.atmacanext.app.domain.policy

object TargetPagePolicy {
    const val MAX_TARGETS = 1
    fun normalize(raw: String): String? = raw.trim().removePrefix("@").lowercase(java.util.Locale.ROOT)
        .takeIf { it.matches(Regex("[a-z0-9_]{1,15}")) }
    fun canAdd(existing: Collection<String>, handle: String): Boolean =
        handle in existing || existing.distinct().size < MAX_TARGETS
}
