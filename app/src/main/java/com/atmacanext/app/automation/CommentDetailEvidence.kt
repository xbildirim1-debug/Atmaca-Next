package com.atmacanext.app.automation

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
        // A timed author is a reply/feed row, not the expanded post's author.
        for (i in candidates) {
            val n = nodes[i]
            if (labels(n).any { TweetContentEvidence.header(it) != null }) return null
            val handle = labels(n).firstNotNullOfOrNull { raw ->
                AccountSwitcherInspector.dedicatedHandle(raw) ?: run {
                    val match = Regex("^[^@\\n]{1,70}\\s@([A-Za-z0-9_]{1,15})$").matchEntire(raw.trim())
                    match?.groupValues?.get(1)?.lowercase()
                }
            }
            if (handle != null) return Header(handle, i)
        }
        return null
    }

    fun actionIndex(nodes: List<NodeSnapshot>, expected: String, accepted: Set<String>): Int? {
        val header = header(nodes)?.takeIf { it.handle == expected } ?: return null
        val h = nodes[header.index].bounds
        // Name and handle can occupy two lines; use their measured line height.
        return nodes.indices.firstOrNull { i ->
            val n = nodes[i]
            val b = n.bounds
            n.visible && n.enabled && !n.editable && b.right > b.left && b.bottom > b.top &&
                b.left >= h.left + (h.bottom - h.top) && b.bottom >= h.top - (h.bottom - h.top) && b.top <= h.bottom &&
                labels(n).any { VerifiedFollowPolicy.matchesAction(it, accepted) }
        }
    }

    fun has(nodes: List<NodeSnapshot>, expected: String, accepted: Set<String>) = actionIndex(nodes, expected, accepted) != null

    private fun labels(node: NodeSnapshot) = listOfNotNull(node.text, node.contentDescription)
}
