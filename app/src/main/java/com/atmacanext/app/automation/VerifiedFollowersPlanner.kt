package com.atmacanext.app.automation

/**
 * CP15 Verified Followers state contract.
 *
 * This is deliberately evidence-driven: no absolute screen coordinates,
 * no random profile opening, and no success increment until the same row
 * is observed in Following state.
 */
data class VerifiedSourceCursor(
    val processedSourceHandles: Set<String> = emptySet(),
    val currentSourceHandle: String? = null,
    val successfulFollows: Int = 0,
    val limit: Int = 30,
)

sealed interface VerifiedDecision {
    data object NeedOwnFollowers : VerifiedDecision
    data object NeedStableTop : VerifiedDecision
    data class OpenSource(val handle: String) : VerifiedDecision
    data object NeedSourceFollowers : VerifiedDecision
    data object NeedVerifiedTab : VerifiedDecision
    data class FollowRow(val handle: String) : VerifiedDecision
    data class VerifyFollowing(val handle: String) : VerifiedDecision
    data object NeedNextSource : VerifiedDecision
    data object Completed : VerifiedDecision
    data class Pause(val reason: String) : VerifiedDecision
}

object VerifiedFollowersPlanner {
    fun next(
        cursor: VerifiedSourceCursor,
        ownFollowersStable: Boolean,
        candidateSourceHandles: List<String>,
        sourceProfileVerified: Boolean,
        sourceFollowersOpen: Boolean,
        verifiedTabOpen: Boolean,
        followableHandles: List<String>,
        pendingVerificationHandle: String?,
        pendingRowNowFollowing: Boolean?,
    ): VerifiedDecision {
        if (cursor.successfulFollows >= cursor.limit) return VerifiedDecision.Completed

        pendingVerificationHandle?.let { handle ->
            return when (pendingRowNowFollowing) {
                true -> VerifiedDecision.NeedVerifiedTab
                false -> VerifiedDecision.VerifyFollowing(handle)
                null -> VerifiedDecision.VerifyFollowing(handle)
            }
        }

        if (!ownFollowersStable && cursor.currentSourceHandle == null) {
            return VerifiedDecision.NeedStableTop
        }

        if (cursor.currentSourceHandle == null) {
            val source = candidateSourceHandles.firstOrNull {
                it.isNotBlank() && it !in cursor.processedSourceHandles
            } ?: return VerifiedDecision.Pause("Yeni kaynak takipçi bulunamadı.")
            return VerifiedDecision.OpenSource(source)
        }

        if (!sourceProfileVerified) {
            return VerifiedDecision.Pause("Kaynak profil @handle doğrulanamadı.")
        }

        if (!sourceFollowersOpen) return VerifiedDecision.NeedSourceFollowers
        if (!verifiedTabOpen) return VerifiedDecision.NeedVerifiedTab

        val target = followableHandles.firstOrNull()
            ?: return VerifiedDecision.NeedNextSource

        return VerifiedDecision.FollowRow(target)
    }

    fun afterVerifiedFollow(cursor: VerifiedSourceCursor): VerifiedSourceCursor =
        cursor.copy(successfulFollows = (cursor.successfulFollows + 1).coerceAtMost(cursor.limit))

    fun advanceSource(cursor: VerifiedSourceCursor): VerifiedSourceCursor {
        val current = cursor.currentSourceHandle
        return cursor.copy(
            processedSourceHandles = if (current == null) cursor.processedSourceHandles
            else cursor.processedSourceHandles + current,
            currentSourceHandle = null,
        )
    }
}
