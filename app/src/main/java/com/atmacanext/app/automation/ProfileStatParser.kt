package com.atmacanext.app.automation

/** Reads only labelled counters; keeps X's displayed precision (for example 1,2 B). */
object ProfileStatParser {
    data class Stats(val followers: String? = null, val following: String? = null)
    private const val NUMBER = "[0-9]+(?:[.,][0-9]+| [0-9]{3})*(?: ?(?:bin|mn|milyon|[kmb]))?"
    private const val FOLLOWERS = "(?:takipçiler|takipçi|followers|follower)"
    private const val FOLLOWING = "(?:takip ediliyor|takip ediyor|takip edilen|following)"
    private val numeric = Regex("^$NUMBER$", RegexOption.IGNORE_CASE)
    private val labelled = Regex("($NUMBER) +($FOLLOWERS|$FOLLOWING)(?= |$)", RegexOption.IGNORE_CASE)
    private val reversed = Regex("^($FOLLOWERS|$FOLLOWING) *:?[ ]+($NUMBER)$", RegexOption.IGNORE_CASE)
    private val followerLabel = Regex("^$FOLLOWERS$", RegexOption.IGNORE_CASE)
    private val followingLabel = Regex("^$FOLLOWING$", RegexOption.IGNORE_CASE)

    fun number(raw: String): String? = clean(raw).takeIf(numeric::matches)

    fun parse(rawLabels: List<String>): Stats {
        var followers: String? = null
        var following: String? = null
        fun accept(label: String, count: String) {
            if (followerLabel.matches(label)) followers = followers ?: count
            if (followingLabel.matches(label)) following = following ?: count
        }
        val labels = rawLabels.map(::clean).filter(String::isNotEmpty)
        for (raw in labels) {
            // A compound statistics row is allowed, arbitrary profile/bio prose is not.
            val matches = labelled.findAll(raw).toList()
            val remainder = labelled.replace(raw, "").trim(' ', ',', '·', '|')
            if (matches.isNotEmpty() && remainder.isEmpty()) {
                matches.forEach { accept(it.groupValues[2], it.groupValues[1]) }
            }
            reversed.matchEntire(raw)?.let { accept(it.groupValues[1], it.groupValues[2]) }
        }
        // Split text nodes: prefer the preceding value. Never consume the next counter.
        labels.forEachIndexed { index, label ->
            if (followerLabel.matches(label) || followingLabel.matches(label)) {
                val before = labels.getOrNull(index - 1)?.let(::number)
                val after = if (index == 0) labels.getOrNull(1)?.let(::number) else null
                (before ?: after)?.let { accept(label, it) }
            }
        }
        return Stats(followers, following)
    }

    private fun clean(raw: String) = raw.replace('\u00a0', ' ').replace('\u202f', ' ')
        .replace(Regex("\\s+"), " ").trim()
}
