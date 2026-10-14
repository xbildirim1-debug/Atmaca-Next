package com.atmacanext.app.automation

import kotlin.math.abs

/** The opened reply's own header, never a Follow button belonging to its replies. */
internal object CommentDetailEvidence {
    data class Header(val handle: String, val index: Int)

    fun isTitle(raw: String): Boolean {
        val label = XUiVocabulary.normalize(raw)
        return label in setOf("gönderi", "post", "tweet") ||
            Regex("^(gönderi|post|tweet),? (başlık|heading)$").matches(label)
    }

    fun header(nodes: List<NodeSnapshot>): Header? {
        // A parent can repeat the title with bounds covering the whole page.
        // Prefer its actual text/toolbar node, otherwise every author lies inside
        // the title and the old top >= title.bottom check rejects the whole page.
        val title = nodes.filter { it.visible && !it.editable && labels(it).any(::isTitle) }
            .minByOrNull { (it.bounds.bottom - it.bounds.top).toLong() * (it.bounds.right - it.bounds.left) }
            ?: return null
        val candidates = nodes.indices.filter { i ->
            val n = nodes[i]
            n.visible && !n.editable && n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top &&
                n.bounds.top >= title.bounds.bottom
        }.sortedBy { nodes[it].bounds.top }
        val timedRows = FeedRowEvidence.rows(nodes)
        val timedHeaders = timedRows.map { it.headerIndex }.toSet()

        // Reply rows sit under Alakalı. They are not the opened post.
        for (i in candidates) {
            val n = nodes[i]
            val rawLabels = labels(n)
            if (rawLabels.any { XUiVocabulary.normalize(it) in replySectionLabels }) return null
            val timedAuthor = rawLabels.firstNotNullOfOrNull { label ->
                TweetContentEvidence.header(label)?.takeIf { !it.truncated }?.handle
            } ?: timedRows.firstOrNull { it.headerIndex == i && !it.authorTruncated }?.author
            if (i in timedHeaders || rawLabels.any { TweetContentEvidence.header(it) != null }) {
                compactOpenedHandle(nodes, candidates, i, timedAuthor)?.let { return it }
                continue
            }
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
        val nextReplyTop = firstReplyTop(nodes, h.top)
        val bandBottom = minOf(h.bottom + (lineHeight * 3), nextReplyTop)
        return nodes.indices.asSequence().filter { i ->
            val n = nodes[i]
            val b = n.bounds
            n.visible && (n.enabled || statusOnly(accepted)) && !n.editable && b.right > b.left && b.bottom > b.top &&
                b.bottom >= bandTop && b.bottom <= bandBottom &&
                b.left >= h.left && b.right > (h.left + h.right) / 2 &&
                b.bottom - b.top <= lineHeight * 3 &&
                labels(n).any { VerifiedFollowPolicy.matchesAction(it, accepted) }
        }.minByOrNull { i ->
            val b = nodes[i].bounds
            abs((b.top + b.bottom) - (h.top + h.bottom))
        }
    }

    fun has(nodes: List<NodeSnapshot>, expected: String, accepted: Set<String>) =
        actionIndex(nodes, expected, accepted) != null

    /**
     * X sometimes omits the opened reply author's name/handle while still exposing
     * the single relationship button in the detail header. Bind that button to the
     * top-right header area, never to follow controls in lower replies.
     */
    fun headerActionIndex(nodes: List<NodeSnapshot>, accepted: Set<String>): Int? {
        val title = nodes.filter { it.visible && !it.editable && labels(it).any(::isTitle) }
            .minByOrNull { (it.bounds.bottom - it.bounds.top).toLong() * (it.bounds.right - it.bounds.left) }
            ?: return null
        val screenRight = nodes.filter { it.visible }.maxOfOrNull { it.bounds.right } ?: return null
        val titleHeight = (title.bounds.bottom - title.bounds.top).coerceAtLeast(1)
        val bandBottom = minOf(title.bounds.bottom + maxOf(360, titleHeight * 8),
            firstReplyTop(nodes, title.bounds.bottom))
        return nodes.indices.asSequence().filter { i ->
            val n = nodes[i]
            val b = n.bounds
            n.visible && (n.enabled || statusOnly(accepted)) && !n.editable && b.right > b.left && b.bottom > b.top &&
                b.top >= title.bounds.bottom && b.bottom <= bandBottom &&
                b.left >= screenRight * 3 / 5 && b.right >= screenRight * 4 / 5 &&
                b.bottom - b.top <= titleHeight * 3 &&
                labels(n).any { VerifiedFollowPolicy.matchesAction(it, accepted) }
        }.minByOrNull { nodes[it].bounds.top }
    }

    fun hasHeaderAction(nodes: List<NodeSnapshot>, accepted: Set<String>) =
        headerActionIndex(nodes, accepted) != null

    /** Missing identity permits the bounded header fallback; a conflicting identity never does. */
    fun relationshipActionIndex(nodes: List<NodeSnapshot>, expected: String, accepted: Set<String>): Int? {
        val author = header(nodes)
        if (author != null) {
            if (author.handle != XIdentityDetector.normalizeUsername(expected)) return null
            return actionIndex(nodes, expected, accepted)
        }
        return headerActionIndex(nodes, accepted)
    }

    private fun statusOnly(accepted: Set<String>): Boolean =
        accepted.isNotEmpty() && accepted.all { it in XUiVocabulary.followingActions || it in XUiVocabulary.requestedActions }

    private fun firstReplyTop(nodes: List<NodeSnapshot>, after: Int): Int {
        val rowTop = FeedRowEvidence.rows(nodes).asSequence().map { nodes[it.headerIndex].bounds.top }
            .filter { it > after }.minOrNull() ?: Int.MAX_VALUE
        val separator = nodes.filter { n -> n.visible && !n.editable && n.bounds.top > after &&
            labels(n).any { XUiVocabulary.normalize(it) in setOf("alakalı", "relevant", "en yeni", "latest", "yanıtlar", "replies") } }
            .minOfOrNull { it.bounds.top } ?: Int.MAX_VALUE
        return minOf(rowTop, separator)
    }

    private fun compactOpenedHandle(
        nodes: List<NodeSnapshot>,
        candidates: List<Int>,
        timedIndex: Int,
        timedAuthor: String?,
    ): Header? {
        if (timedAuthor == null) return null
        val timed = nodes[timedIndex]
        val height = (timed.bounds.bottom - timed.bounds.top).coerceAtLeast(1)
        // A merged parent can cover the whole post. Only the top of that parent
        // is the author line; a later same-handle mention in the body is not.
        val windowBottom = if (height > 280) timed.bounds.top + 280 else timed.bounds.bottom + height * 3
        for (j in candidates) {
            if (j == timedIndex) continue
            val child = nodes[j]
            val bounds = child.bounds
            if (bounds.top < timed.bounds.top || bounds.top > windowBottom) continue
            if (bounds.bottom - bounds.top > 160) continue
            val raw = labels(child)
            if (raw.any { TweetContentEvidence.header(it) != null }) continue
            val handle = raw.firstNotNullOfOrNull(::headerHandle) ?: continue
            if (handle == timedAuthor) return Header(handle, j)
        }
        return null
    }

    private fun headerHandle(raw: String): String? {
        val text = raw.trim().replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]"), "")
        AccountSwitcherInspector.dedicatedHandle(text)?.let { return it }
        val badge = "(?:doğrulandı|doğrulanmış(?:\\s+hesap)?|verified(?:\\s+account)?)"
        val match = Regex("^[^@\\n]{1,70}[,\\s]+@([A-Za-z0-9_]{1,15})(?:[,\\s.]+$badge)?$", RegexOption.IGNORE_CASE).matchEntire(text)
            ?: Regex("^[^@\\n]{1,70}\\n+@([A-Za-z0-9_]{1,15})(?:[,\\s.]+$badge)?$", RegexOption.IGNORE_CASE).matchEntire(text)
            ?: Regex("^@([A-Za-z0-9_]{1,15})[,\\s.]+$badge$", RegexOption.IGNORE_CASE).matchEntire(text)
        val prefix = Regex(
            "^[^@\\n]{1,70}[,\\s\\n]+@([A-Za-z0-9_]{1,15})(?![A-Za-z0-9_])"
        ).find(text)
        if (prefix != null) return prefix.groupValues[1].lowercase()
        return match?.groupValues?.get(1)?.lowercase()
    }

    private fun labels(node: NodeSnapshot) = listOfNotNull(node.text, node.contentDescription)

    private val replySectionLabels = setOf(
        "alakalı", "relevant", "en yeni", "latest", "alıntıları görüntüle", "view quotes",
    )
}
