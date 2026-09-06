package com.atmacanext.app.domain.policy

object EditorValidationPolicy {
    fun normalizeXUsername(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("@")) trimmed else "@$trimmed"
    }

    fun isValidXUsername(raw: String): Boolean {
        val normalized = normalizeXUsername(raw)
        if (normalized.length !in 2..16) return false
        return normalized.drop(1).all { it.isLetterOrDigit() || it == '_' }
    }

    fun isValidClock(hour: Int?, minute: Int?): Boolean =
        hour != null && minute != null && hour in 0..23 && minute in 0..59

    fun isValidTaskLimit(limit: Int?): Boolean = limit != null && limit in 1..35
}
