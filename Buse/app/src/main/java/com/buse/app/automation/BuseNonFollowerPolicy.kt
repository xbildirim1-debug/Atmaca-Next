package com.buse.app.automation

import java.util.Locale

enum class BuseRelationship { FOLLOWS_YOU, DOES_NOT_FOLLOW, UNKNOWN }

/** Absence is evidence only inside a complete, unambiguous, currently visible user row. */
data class BuseRowEvidence(
    val handle: String,
    val handlesInRow: Set<String>,
    val labels: List<String>,
    val complete: Boolean,
    val visible: Boolean,
    val enabled: Boolean,
    val following: Boolean,
    val treeComplete: Boolean = true,
)

object BuseNonFollowerPolicy {
    const val PROTECTED_COUNT = 200

    fun normalizeHandle(value: String): String = value.trim().removePrefix("@").lowercase(Locale.ROOT)
    fun validHandle(value: String): Boolean = value.matches(Regex("[a-z0-9_]{1,15}"))

    fun relationship(row: BuseRowEvidence): BuseRelationship {
        val handle = normalizeHandle(row.handle)
        if (!validHandle(handle) || row.handlesInRow.map(::normalizeHandle).toSet() != setOf(handle) ||
            !row.visible || !row.enabled || !row.treeComplete) return BuseRelationship.UNKNOWN
        val badge = row.labels.any(::hasFollowsYouLabel)
        if (badge) return BuseRelationship.FOLLOWS_YOU
        return if (row.complete && row.following) BuseRelationship.DOES_NOT_FOLLOW else BuseRelationship.UNKNOWN
    }

    fun hasFollowsYouLabel(label: String): Boolean {
            val normalized = java.text.Normalizer.normalize(label, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
        return Regex("\\b(seni takip ediyor|follows you)\\b").containsMatchIn(normalized)
    }
}

/** X may publish the badge as a sibling immediately above the person's card. */
object BuseBadgeAssociation {
    fun belongsToRow(badgeTop: Int, badgeBottom: Int, handleTop: Int, previousRowBottom: Int, viewportTop: Int): Boolean =
        badgeBottom > badgeTop && badgeTop >= maxOf(previousRowBottom, viewportTop) && badgeBottom <= handleTop
}

/** Counts people, never scrolls. Only forward, overlapping viewports may extend the first 200. */
class BuseFollowingRun {
    private val seen = LinkedHashSet<String>()
    private val protected = LinkedHashSet<String>()
    private var previous = emptyList<String>()
    val protectedHandles: Set<String> get() = protected.toSet()
    val seenCount: Int get() = seen.size
    val ready: Boolean get() = protected.size == BuseNonFollowerPolicy.PROTECTED_COUNT

    fun observe(orderedHandles: List<String>): Boolean {
        val handles = orderedHandles.map(BuseNonFollowerPolicy::normalizeHandle)
            .filter(BuseNonFollowerPolicy::validHandle).distinct()
        if (handles.isEmpty()) return false
        // A missing viewport could hide people; restarting at a guessed rank is forbidden.
        if (!ready && previous.isNotEmpty() && handles.none { it in previous }) return false
        // An overlapping prefix followed by new handles is the only acceptable forward order.
        if (!ready && handles.any { it !in seen } && handles.dropWhile { it in seen }.any { it in seen }) return false
        handles.forEach { handle ->
            if (seen.add(handle) && protected.size < BuseNonFollowerPolicy.PROTECTED_COUNT) protected += handle
        }
        previous = handles
        return true
    }

    fun mayUnfollow(row: BuseRowEvidence, ownHandle: String, excluded: Set<String>): Boolean {
        val handle = BuseNonFollowerPolicy.normalizeHandle(row.handle)
        return ready && handle in seen && handle !in protected && handle !in excluded &&
            handle != BuseNonFollowerPolicy.normalizeHandle(ownHandle) &&
            BuseNonFollowerPolicy.relationship(row) == BuseRelationship.DOES_NOT_FOLLOW
    }
}
