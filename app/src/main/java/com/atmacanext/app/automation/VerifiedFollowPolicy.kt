package com.atmacanext.app.automation

internal enum class VerifiedFollowOutcome { WAIT, SUCCESS, REVERTED, UNKNOWN }

internal object VerifiedFollowPolicy {
    val plainFollowLabels = setOf("takip et", "follow")
    fun matchesAction(label: String, expected: Set<String>): Boolean {
        val normalized = XUiVocabulary.normalize(label)
        return expected.any {
            normalized == it || normalized.startsWith("$it @") || normalized.startsWith("$it, @") ||
                (it == "follow" && normalized.startsWith("follow ")) ||
                (it == "takip et" && normalized.endsWith(" takip et"))
        }
    }
    fun isPlainFollow(labels: List<String>): Boolean =
        labels.any { matchesAction(it, plainFollowLabels) } &&
            labels.none { matchesAction(it, XUiVocabulary.followActions - plainFollowLabels + XUiVocabulary.followingActions + XUiVocabulary.requestedActions) }
    fun outcome(observedFollowing: Boolean, followingNow: Boolean, plainFollowNow: Boolean,
                stableMs: Long, elapsedMs: Long, requestedNow: Boolean = false): VerifiedFollowOutcome = when {
        requestedNow && !plainFollowNow && !followingNow -> VerifiedFollowOutcome.SUCCESS
        observedFollowing && plainFollowNow && !followingNow -> VerifiedFollowOutcome.REVERTED
        observedFollowing && followingNow && !plainFollowNow && stableMs >= 2_000L -> VerifiedFollowOutcome.SUCCESS
        elapsedMs >= 7_000L -> VerifiedFollowOutcome.UNKNOWN
        else -> VerifiedFollowOutcome.WAIT
    }
    fun nextStreak(previous: Int, outcome: VerifiedFollowOutcome): Int = when (outcome) {
        VerifiedFollowOutcome.REVERTED -> previous + 1
        VerifiedFollowOutcome.SUCCESS, VerifiedFollowOutcome.UNKNOWN -> 0
        VerifiedFollowOutcome.WAIT -> previous
    }
    fun stopAccount(streak: Int): Boolean = streak >= 3
    fun nextSource(candidates: Collection<String>, own: String, visited: Set<String>,
                   random: kotlin.random.Random = kotlin.random.Random.Default): String? =
        candidates.filter { it.isNotBlank() && it != own && it !in visited }.distinct().randomOrNull(random)
}
