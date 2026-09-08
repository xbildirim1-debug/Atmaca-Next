package com.atmacanext.app.automation

import kotlin.math.abs
import kotlin.math.max

/** Shared evidence for public profile recognition, identity and counter taps. */
internal object ProfileSurfaceEvidence {
    data class Profile(val handle: String, val followersIndex: Int, val followingIndex: Int)
    private val number = Regex("[0-9][0-9.,]*\\s*[kmbmn]*", RegexOption.IGNORE_CASE)
    private fun labels(n: NodeSnapshot) = listOfNotNull(n.text, n.contentDescription).map(XUiVocabulary::normalize)
    private fun visible(n: NodeSnapshot) = n.visible && n.enabled && n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top

    fun read(nodes: List<NodeSnapshot>): Profile? {
        if (nodes.any { it.visible && (it.selected || it.checked) &&
                RelationshipTabInspector.classifySelectedLabels(labels(it)) != RelationshipTabInspector.NONE }) return null
        val followers = counters(nodes, XUiVocabulary.followersHeaders)
        val following = counters(nodes, XUiVocabulary.followingHeaders)
        for (f in followers) for (g in following) {
            val a = nodes[f].bounds; val b = nodes[g].bounds
            val height = max(a.bottom - a.top, b.bottom - b.top)
            if (f == g || abs((a.top + a.bottom) - (b.top + b.bottom)) > height * 2) continue
            val statTop = minOf(a.top, b.top)
            // Header handle precedes the stats. Mentions in bio/tweets and the
            // known-follower strip cannot override the first dedicated header.
            val header = nodes.filter { visible(it) && it.bounds.bottom <= statTop }
                .sortedBy { it.bounds.top }
                .firstNotNullOfOrNull { n -> labels(n).firstNotNullOfOrNull(AccountSwitcherInspector::dedicatedHandle) }
                ?: continue
            return Profile(header, f, g)
        }
        return null
    }

    private fun counters(nodes: List<NodeSnapshot>, tokens: Set<String>): List<Int> = nodes.indices.filter { i ->
        val n = nodes[i]
        visible(n) && labels(n).any { label ->
            tokens.any { token ->
                val attached = if (label.endsWith(" $token")) label.removeSuffix(" $token")
                    else if (label.startsWith("$token ")) label.removePrefix("$token ") else ""
                number.matches(attached) || (label == token && nodes.any { value ->
                    visible(value) && labels(value).any(number::matches) && adjacent(value, n)
                })
            }
        }
    }

    private fun adjacent(value: NodeSnapshot, label: NodeSnapshot): Boolean {
        val v = value.bounds; val l = label.bounds
        val h = max(v.bottom - v.top, l.bottom - l.top)
        val sameLine = abs((v.top + v.bottom) - (l.top + l.bottom)) <= h &&
            v.right <= l.left + h / 2 && l.left - v.right <= h * 3
        val stacked = v.bottom <= l.top && l.top - v.bottom <= h &&
            abs((v.left + v.right) - (l.left + l.right)) <= max(v.right - v.left, l.right - l.left)
        return sameLine || stacked
    }
}
