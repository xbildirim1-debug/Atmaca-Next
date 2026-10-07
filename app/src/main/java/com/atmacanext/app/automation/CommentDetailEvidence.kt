package com.atmacanext.app.automation

import android.util.Log
import kotlin.math.abs

/** The opened reply's own header, never a Follow button belonging to its replies. */
internal object CommentDetailEvidence {
    data class Header(val handle: String, val index: Int)

    fun isTitle(raw: String): Boolean {
        val label = XUiVocabulary.normalize(raw)
        return label in setOf("gönderi", "post", "tweet") ||
            Regex("^(gönderi|post|tweet),? (başlık|heading)$").matches(label)
    }

    fun header(nodes: List<NodeSnapshot>, excludeFeedTimedHeaders: Boolean = true): Header? {
        fun finish(result: Header?, reason: String, step: Int, index: Int?): Header? {
            val node = index?.let(nodes::get)
            Log.d("AtmacaDetailHeader", "exit=$reason step=$step nodeIndex=$index handle=${result?.handle} " +
                "viewId=${node?.viewId} bounds=${node?.bounds}")
            return result
        }
        // A parent can repeat the title with bounds covering the whole page.
        // Prefer its actual text/toolbar node, otherwise every author lies inside
        // the title and the old top >= title.bottom check rejects the whole page.
        val title = nodes.filter { it.visible && !it.editable && labels(it).any(::isTitle) }
            .minByOrNull { (it.bounds.bottom - it.bounds.top).toLong() * (it.bounds.right - it.bounds.left) }
        Log.d("AtmacaDetailHeader", "titleFound=${title != null} titleIndex=${title?.let(nodes::indexOf)} " +
            "titleBounds=${title?.bounds} nodes=${nodes.size}")
        if (title == null) return finish(null, "NO_TITLE", 0, null)
        // A detail author may have the same time semantics as a feed row. Only
        // relax that exclusion above an independently measured main-post boundary.
        val headerEnd = if (excludeFeedTimedHeaders) Int.MAX_VALUE else headerBandEnd(nodes, title.bounds.bottom)
        val candidates = nodes.indices.filter { i ->
            val n = nodes[i]
            n.visible && !n.editable && n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top &&
                n.bounds.top >= title.bounds.bottom && n.bounds.top < headerEnd
        }.sortedBy { nodes[it].bounds.top }
        val rejectTimed = excludeFeedTimedHeaders || headerEnd == Int.MAX_VALUE
        val timedHeaders = if (rejectTimed) FeedRowEvidence.rows(nodes).map { it.headerIndex }.toSet() else emptySet()
        Log.d("AtmacaDetailHeader", "candidates=${candidates.size} timedHeaders=${timedHeaders.sorted()} " +
            "excludeFeedTimedHeaders=$excludeFeedTimedHeaders headerEnd=$headerEnd")

        // Commenter-follow keeps the strict feed exclusion. Reply composition can
        // read a time-bearing main author within the measured header band.
        for ((position, i) in candidates.withIndex()) {
            val n = nodes[i]
            val rawLabels = labels(n)
            if (i in timedHeaders) return finish(null, "TIMED_INDEX", position + 1, i)
            val timed = rawLabels.mapNotNull(TweetContentEvidence::header)
            if (rejectTimed && timed.isNotEmpty())
                return finish(null, "TIMED_LABEL", position + 1, i)
            val handle = rawLabels.firstNotNullOfOrNull(::headerHandle)
                ?: timed.firstOrNull { !it.truncated }?.handle
            if (handle != null) return finish(Header(handle, i), "AUTHOR_FOUND", position + 1, i)
        }
        return finish(null, "NO_HANDLE", candidates.size, candidates.lastOrNull())
    }

    fun headerBandEnd(nodes: List<NodeSnapshot>, titleBottom: Int): Int = nodes.asSequence()
        .filter { n -> n.visible && !n.editable && n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top &&
            n.bounds.top >= titleBottom }
        .filter { n ->
            val id = n.viewId.orEmpty().substringAfterLast('/').lowercase()
            id in setOf("tweet_text", "tweet_content", "status_text", "tweet_metadata", "tweet_stats") ||
                id in setOf("toolbar_reply", "toolbar_like", "toolbar_retweet", "toolbar_bookmark", "toolbar_share") || labels(n).any { raw ->
                    val label = XUiVocabulary.normalize(raw)
                    label in setOf("alakalı", "relevant", "en yeni", "latest", "yanıtlar", "replies") ||
                        Regex("^\\d{1,2}:\\d{2}\\s*[·•].+$").matches(label)
                }
        }.minOfOrNull { it.bounds.top } ?: Int.MAX_VALUE

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
