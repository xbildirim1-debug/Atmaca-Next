package com.atmacanext.app.automation

import kotlin.math.abs

/** The opened reply's own header, never a Follow button belonging to its replies. */
internal object CommentDetailEvidence {
    data class Header(val handle: String, val index: Int)

    fun header(nodes: List<NodeSnapshot>): Header? {
        val title = nodes.filter { it.visible && labels(it).any { s -> XUiVocabulary.normalize(s) in XUiVocabulary.tweetDetailSignals } }
            .minByOrNull { it.bounds.top } ?: return null
        val candidates = nodes.indices.filter { i ->
            val n = nodes[i]
            n.visible && !n.editable && n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top &&
                n.bounds.top >= title.bounds.bottom
        }.sortedBy { nodes[it].bounds.top }

        // The expanded post's own author is the first untimed identity below the
        // detail title. A timed author belongs to a reply/feed row, so do not ever
        // borrow a Follow control from that lower row.
        for (i in candidates) {
            val n = nodes[i]
            val rawLabels = labels(n)
            if (rawLabels.any { TweetContentEvidence.header(it) != null }) return null
            val handle = rawLabels.firstNotNullOfOrNull(::headerHandle)
            if (handle != null) return Header(handle, i)
        }
        return null
    }

    fun actionIndex(nodes: List<NodeSnapshot>, expected: String, accepted: Set<String>): Int? {
        val header = header(nodes)?.takeIf { it.handle == XIdentityDetector.normalizeUsername(expected) } ?: return null
        val h = nodes[header.index].bounds
        val lineHeight = (h.bottom - h.top).coerceAtLeast(1)

        // X/Compose changes whether name and @handle are one line, two lines, or a
        // wide semantics node. Bind the relationship control to a measured vertical
        // band around the actual header instead of requiring its top edge to end on
        // the exact @handle line. This still excludes Follow buttons on replies below.
        val bandTop = h.top - (lineHeight * 2)
        val bandBottom = h.bottom + (lineHeight * 3)
        return nodes.indices.asSequence().filter { i ->
            val n = nodes[i]
            val b = n.bounds
            n.visible && n.enabled && !n.editable && b.right > b.left && b.bottom > b.top &&
                b.bottom >= bandTop && b.top <= bandBottom &&
                labels(n).any { VerifiedFollowPolicy.matchesAction(it, accepted) }
        }.minByOrNull { i ->
            val b = nodes[i].bounds
            abs((b.top + b.bottom) - (h.top + h.bottom))
        }
    }

    fun has(nodes: List<NodeSnapshot>, expected: String, accepted: Set<String>) =
        actionIndex(nodes, expected, accepted) != null

    private fun headerHandle(raw: String): String? {
        AccountSwitcherInspector.dedicatedHandle(raw)?.let { return it }
        val text = raw.trim()
        // Expanded post headers can be exposed as "Name @handle", "Name, @handle"
        // or on two semantic lines. Only accept a single trailing handle so a body
        // mention can never become the detail author.
        val match = Regex("^[^@\\n]{1,70}[,\\s]+@([A-Za-z0-9_]{1,15})$").matchEntire(text)
            ?: Regex("^[^@\\n]{1,70}\\n+@([A-Za-z0-9_]{1,15})$").matchEntire(text)
        return match?.groupValues?.get(1)?.lowercase()
    }

    private fun labels(node: NodeSnapshot) = listOfNotNull(node.text, node.contentDescription)
}
