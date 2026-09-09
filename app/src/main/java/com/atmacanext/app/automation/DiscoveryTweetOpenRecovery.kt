package com.atmacanext.app.automation

/** Opening a truncated text can expand it in-place without navigating. */
internal object DiscoveryTweetOpenRecovery {
    data class Attempt(val key: String, val author: String, val text: String, val count: Int, val at: Long)
    data class Candidate(val key: String, val author: String?, val text: String, val age: Long?)
    enum class Decision { WAIT, RETRY, RESCAN }

    private fun normalized(text: String) = text.replace(Regex("\\s+"), " ").trim()
        .replace(Regex("(?i)\\s*(?:daha fazlasını göster|show more)\\s*$"), "")
        .trimEnd('.', '…', ' ')

    fun matches(attempt: Attempt, candidate: Candidate): Boolean {
        if (candidate.author != attempt.author || !XTweetInspector.eligibleAge(candidate.age)) return false
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
}
