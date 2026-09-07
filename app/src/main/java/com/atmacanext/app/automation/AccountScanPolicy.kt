package com.atmacanext.app.automation

object AccountScanPolicy {
    fun next(handles: Collection<String>, saved: Set<String>, skipped: Set<String>): String? =
        handles.distinct().take(10).firstOrNull { it !in saved && it !in skipped }

    fun canRecord(expected: String?, actual: String?, followers: String?, following: String?): Boolean =
        !expected.isNullOrBlank() && expected == actual && !followers.isNullOrBlank() && !following.isNullOrBlank()

    /** A stream of accessibility events may bring a tick forward, but cannot postpone it. */
    fun replaceTick(existingDue: Long?, requestedDue: Long): Boolean = existingDue == null || requestedDue < existingDue
}
