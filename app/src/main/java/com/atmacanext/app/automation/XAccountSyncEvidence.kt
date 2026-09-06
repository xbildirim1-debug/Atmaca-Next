package com.atmacanext.app.automation

import java.util.Locale

data class XProfileCounters(
    val username: String,
    val followers: String?,
    val following: String?,
)

/**
 * CP15 account/profile evidence reader.
 *
 * It does not click anything and does not depend on screen coordinates.
 * It only accepts explicit X @handles and profile counter evidence coming
 * from Accessibility text/contentDescription/view-id snapshots.
 */
object XAccountSyncEvidence {
    const val MAX_ACCOUNTS = 10

    fun accountHandles(nodes: List<NodeSnapshot>): List<String> =
        nodes.asSequence()
            .flatMap { node ->
                sequenceOf(node.text, node.contentDescription)
            }
            .mapNotNull(XIdentityDetector::extractHandle)
            .map(XIdentityDetector::normalizeUsername)
            .filter { it.isNotBlank() }
            .map { "@$it" }
            .distinct()
            .take(MAX_ACCOUNTS)
            .toList()

    fun profileCounters(
        nodes: List<NodeSnapshot>,
        expectedUsername: String,
    ): XProfileCounters? {
        val expected = XIdentityDetector.normalizeUsername(expectedUsername)
        if (expected.isBlank()) return null

        val visibleHandles = nodes.asSequence()
            .flatMap { sequenceOf(it.text, it.contentDescription) }
            .mapNotNull(XIdentityDetector::extractHandle)
            .map(XIdentityDetector::normalizeUsername)
            .toSet()

        // Never bind counters to an unverified/wrong account.
        if (expected !in visibleHandles) return null

        var followers: String? = null
        var following: String? = null

        nodes.forEach { node ->
            val candidates = listOfNotNull(node.text, node.contentDescription, node.viewId)
            candidates.forEach { raw ->
                parseCounter(raw, CounterKind.FOLLOWERS)?.let { followers = followers ?: it }
                parseCounter(raw, CounterKind.FOLLOWING)?.let { following = following ?: it }
            }
        }

        if (followers == null && following == null) return null
        return XProfileCounters(expected, followers, following)
    }

    private enum class CounterKind { FOLLOWERS, FOLLOWING }

    private fun parseCounter(raw: String, kind: CounterKind): String? {
        val normalized = raw.lowercase(Locale.ROOT)
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        val labels = when (kind) {
            CounterKind.FOLLOWERS -> listOf("followers", "takipçiler", "takipçi")
            CounterKind.FOLLOWING -> listOf("following", "takip edilen", "takip ediliyor")
        }

        // Strong resource-id evidence is allowed even if label and value are split.
        val idSignal = when (kind) {
            CounterKind.FOLLOWERS -> normalized.contains("followers") && normalized.contains("count")
            CounterKind.FOLLOWING -> normalized.contains("following") && normalized.contains("count")
        }

        val number = Regex("""(?<![\w@])(\d[\d.,]*\s*[kmb]?)""", RegexOption.IGNORE_CASE)
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(" ", "")
            ?.takeIf { it.isNotBlank() }

        if (number != null && labels.any(normalized::contains)) return number
        if (number != null && idSignal) return number
        return null
    }
}
