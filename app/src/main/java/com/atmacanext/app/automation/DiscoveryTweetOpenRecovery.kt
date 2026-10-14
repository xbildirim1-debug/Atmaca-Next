package com.atmacanext.app.automation

/** Opening a truncated text can expand it in-place without navigating. */
internal object DiscoveryTweetOpenRecovery {
    data class Attempt(val key: String, val author: String, val text: String, val count: Int, val at: Long)
    data class Candidate(val key: String, val author: String?, val text: String, val age: Long?)
    enum class Decision { WAIT, RETRY, RESCAN }

    private fun normalized(text: String) = text.replace(Regex("\\s+"), " ").trim()
        .replace(Regex("(?i)\\s*(?:daha fazlasını göster|show more)\\s*$"), "")
        .trimEnd('.', '…', ' ')

    fun matches(attempt: Attempt, candidate: Candidate, requireOlderPost: Boolean = true): Boolean {
        if (candidate.author != attempt.author || (requireOlderPost && !XTweetInspector.eligibleAge(candidate.age))) return false
        if (candidate.text.isBlank()) return false
        if (candidate.key == attempt.key) return true
        val before = normalized(attempt.text)
        val after = normalized(candidate.text)
        // Only the same substantial prefix can survive an inline expansion. A
        // different quoted tweet by the same author must not become the retry.
        return before.length >= 40 && after.length >= 40 &&
            (after.startsWith(before) || before.startsWith(after))
    }

    fun decide(attempt: Attempt, sameCandidateVisible: Boolean, now: Long): Decision {
        if (now - attempt.at < 1_500L) return Decision.WAIT
        if (attempt.count >= 3 || (!sameCandidateVisible && now - attempt.at >= 3_000L)) return Decision.RESCAN
        return if (sameCandidateVisible) Decision.RETRY else Decision.WAIT
    }

    /** "Daha fazlasını göster" is an inline span. ACTION_CLICK expands it and never opens the post. */
    fun inlineExpansion(text: String): Boolean {
        val value = text.lowercase()
        return value.contains("daha fazlasını göster") || value.contains("show more")
    }

    /** Stay on the first lines, clear of the trailing expansion link. */
    fun expandTap(attempt: Int): Pair<Float, Float> = when (attempt) {
        0 -> 0.28f to 0.16f
        1 -> 0.22f to 0.34f
        else -> 0.40f to 0.28f
    }

    /** Distinct text-only retry points handle different Compose layouts on later accounts. */
    fun tapPosition(attempt: Int): Pair<Float, Float> = when (attempt) {
        1 -> 0.50f to 0.50f
        else -> 0.30f to 0.65f
    }
}
