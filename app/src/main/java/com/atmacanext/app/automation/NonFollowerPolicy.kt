package com.atmacanext.app.automation

import java.util.Locale

enum class NonFollowerRelationship { FOLLOWS_YOU, DOES_NOT_FOLLOW, UNKNOWN }

/** Absence is evidence only inside a complete, unambiguous, currently visible user row. */
data class NonFollowerRowEvidence(
    val handle: String,
    val handlesInRow: Set<String>,
    val labels: List<String>,
    val complete: Boolean,
    val visible: Boolean,
    val enabled: Boolean,
    val following: Boolean,
    val treeComplete: Boolean = true,
)

object NonFollowerPolicy {
    const val PROTECTED_COUNT = 100

    fun normalizeHandle(value: String): String = value.trim().removePrefix("@").lowercase(Locale.ROOT)
    fun validHandle(value: String): Boolean = value.matches(Regex("[a-z0-9_]{1,15}"))

    fun relationship(row: NonFollowerRowEvidence): NonFollowerRelationship {
        val handle = normalizeHandle(row.handle)
        if (!validHandle(handle) || row.handlesInRow.map(::normalizeHandle).toSet() != setOf(handle) ||
            !row.visible || !row.enabled || !row.treeComplete) return NonFollowerRelationship.UNKNOWN
        val badge = row.labels.any(::hasFollowsYouLabel)
        if (badge) return NonFollowerRelationship.FOLLOWS_YOU
        return if (row.complete && row.following) NonFollowerRelationship.DOES_NOT_FOLLOW else NonFollowerRelationship.UNKNOWN
    }

    fun hasFollowsYouLabel(label: String): Boolean {
            val normalized = java.text.Normalizer.normalize(label, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
        return Regex("\\b(seni takip ediyor|follows you)\\b").containsMatchIn(normalized)
    }
}

/** X may publish the badge as a sibling immediately above the person's card. */
object NonFollowerBadgeAssociation {
    fun belongsToRow(badgeTop: Int, badgeBottom: Int, handleTop: Int, previousRowBottom: Int, viewportTop: Int): Boolean =
        badgeBottom > badgeTop && badgeTop >= maxOf(previousRowBottom, viewportTop) && badgeBottom <= handleTop
}

/** Counts people, never scrolls. Only forward, overlapping viewports may extend the first 100. */
class NonFollowerFollowingRun {
    private val seen = LinkedHashSet<String>()
    private val protected = LinkedHashSet<String>()
    private var previous = emptyList<String>()
    val protectedHandles: Set<String> get() = protected.toSet()
    val seenCount: Int get() = seen.size
    val ready: Boolean get() = protected.size == NonFollowerPolicy.PROTECTED_COUNT

    fun observe(orderedHandles: List<String>): Boolean {
        val handles = orderedHandles.map(NonFollowerPolicy::normalizeHandle)
            .filter(NonFollowerPolicy::validHandle).distinct()
        if (handles.isEmpty()) return false
        // A missing viewport could hide people; restarting at a guessed rank is forbidden.
        if (!ready && previous.isNotEmpty() && handles.none { it in previous }) return false
        // An overlapping prefix followed by new handles is the only acceptable forward order.
        if (!ready && handles.any { it !in seen } && handles.dropWhile { it in seen }.any { it in seen }) return false
        handles.forEach { handle ->
            if (seen.add(handle) && protected.size < NonFollowerPolicy.PROTECTED_COUNT) protected += handle
        }
        previous = handles
        return true
    }

    fun mayUnfollow(row: NonFollowerRowEvidence, ownHandle: String, excluded: Set<String>): Boolean {
        val handle = NonFollowerPolicy.normalizeHandle(row.handle)
        return ready && handle in seen && handle !in protected && handle !in excluded &&
            handle != NonFollowerPolicy.normalizeHandle(ownHandle) &&
            NonFollowerPolicy.relationship(row) == NonFollowerRelationship.DOES_NOT_FOLLOW
    }
}
