package com.atmacanext.app.automation

/**
 * CP15 Unfollow evidence gate.
 * A destructive action is allowed only on a single-handle row that is proven
 * to belong to the Following list. Bio/birthday/show-more/translation nodes
 * are rejected explicitly.
 */
object UnfollowSafetyGate {
    private val forbiddenFragments = listOf(
        "show more", "daha fazla", "birthday", "birth date", "doğum tarihi",
        "translate bio", "translate profile", "biyografiyi çevir", "çevir"
    )

    data class RowEvidence(
        val handle: String?,
        val rowText: String,
        val buttonText: String?,
        val distinctHandleCountSeen: Int,
        val stableEndObservations: Int,
    )

    fun depthSatisfied(e: RowEvidence): Boolean =
        e.distinctHandleCountSeen >= 100 || e.stableEndObservations >= 2

    fun mayClickFollowing(e: RowEvidence): Boolean {
        val handle = e.handle?.trim().orEmpty()
        if (!handle.startsWith("@") || handle.length < 2) return false

        val row = e.rowText.lowercase()
        if (forbiddenFragments.any(row::contains)) return false

        val button = e.buttonText?.lowercase()?.trim().orEmpty()
        val followingButton = button in setOf(
            "following", "takip ediliyor", "takiptesin", "takiptesiniz"
        )
        return followingButton && depthSatisfied(e)
    }

    fun confirmationMatches(targetHandle: String, dialogText: String): Boolean {
        val target = targetHandle.trim().lowercase()
        val text = dialogText.lowercase()
        if (!text.contains(target)) return false

        val exactSignals = listOf(
            "unfollow", "takibi bırak", "takipten çık"
        )
        return exactSignals.any(text::contains)
    }

    fun successVerified(
        targetHandle: String,
        rowHandle: String?,
        buttonText: String?,
    ): Boolean {
        val target = targetHandle.trim().lowercase()
        val row = rowHandle?.trim()?.lowercase()
        if (row != target) return false

        val button = buttonText?.trim()?.lowercase().orEmpty()
        return button in setOf("follow", "takip et")
    }
}
