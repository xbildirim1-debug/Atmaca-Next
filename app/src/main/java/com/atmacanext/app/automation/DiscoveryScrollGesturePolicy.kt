package com.atmacanext.app.automation

import android.graphics.Rect

/** Safe vertical path outside inline media and the inline reply composer. */
internal object DiscoveryScrollGesturePolicy {
    private val X_RATIOS = floatArrayOf(0.06f, 0.12f)
    const val FORWARD_START_Y_RATIO = 0.88f
    const val FORWARD_END_Y_RATIO = 0.20f
    const val BACKWARD_START_Y_RATIO = FORWARD_END_Y_RATIO
    const val BACKWARD_END_Y_RATIO = FORWARD_START_Y_RATIO

    data class VerticalPath(val startY: Float, val endY: Float)

    private val inlineReplyLabels = setOf(
        "yanıtını gönder",
        "yanıt gönder",
        "post your reply",
        "reply",
    )

    /** Retry in a second media-free gutter when X accepts but swallows a swipe. */
    fun xRatio(recoveryAttempt: Int): Float =
        X_RATIOS[recoveryAttempt.coerceAtLeast(0) % X_RATIOS.size]

    /**
     * The default profile gesture may be long, but a reply thread needs overlap:
     * short text replies, tall images and video cards can all coexist in one view.
     * When the inline reply composer is visible we therefore cap the swipe travel
     * to a fraction of the live screen height and leave most of the old viewport
     * on screen. Every newly exposed row is then parsed before another gesture.
     *
     * The lower edge is derived from the actual composer label/editable rectangle.
     * A generous semantic clearance is used because X often exposes only the text
     * inside the rounded composer; its container can begin well above that label.
     * No device pixels, account-specific coordinates or media dimensions are fixed.
     */
    fun verticalPath(
        bounds: Rect,
        nodes: List<NodeSnapshot>,
        forward: Boolean,
        maxStartRatio: Float = FORWARD_START_Y_RATIO,
    ): VerticalPath? {
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (width <= 0 || height <= 0) return null

        val upper = bounds.top + height * FORWARD_END_Y_RATIO
        val cappedStart = maxStartRatio.coerceIn(0.50f, FORWARD_START_Y_RATIO)
        val defaultLower = bounds.top + height * minOf(FORWARD_START_Y_RATIO, cappedStart)
        val composerTop = nodes.asSequence()
            .filter { it.visible && it.bounds.right > it.bounds.left && it.bounds.bottom > it.bounds.top }
            .mapNotNull { node ->
                val labels = listOfNotNull(node.text, node.contentDescription)
                    .map(XUiVocabulary::normalize)
                val id = node.viewId.orEmpty().lowercase()
                val labelEvidence = labels.any { label ->
                    label in inlineReplyLabels || label.startsWith("yanıtını gönder") ||
                        label.startsWith("post your reply")
                }
                val idEvidence = id.contains("reply") &&
                    (id.contains("compose") || id.contains("composer") ||
                        id.contains("input") || id.contains("editor") || id.contains("entry"))
                if (!node.editable && !labelEvidence && !idEvidence) return@mapNotNull null

                val nodeHeight = (node.bounds.bottom - node.bounds.top).coerceAtLeast(1)
                // Text-only semantics describe the inner label, not the rounded
                // composer. Clear at least three label heights; for an editable or
                // identified composer node the live rectangle is already stronger.
                val clearance = if (node.editable || idEvidence) {
                    maxOf(nodeHeight / 3, (height * 0.025f).toInt()).coerceAtLeast(1)
                } else {
                    maxOf(nodeHeight * 3, (height * 0.055f).toInt()).coerceAtLeast(1)
                }
                (node.bounds.top - clearance).toFloat()
            }
            .minOrNull()

        val lower = minOf(defaultLower, composerTop ?: defaultLower)
        val phraseTop = bottomComposerTop(bounds, nodes)
        val guardedLower = minOf(lower, phraseTop ?: lower)
        val minimumTravel = height * 0.16f
        if (guardedLower - upper < minimumTravel) return null

        // Composer evidence is a strong signal that this is the tweet-detail reply
        // viewport. Preserve overlap there so a single long swipe cannot jump over
        // several short commenters after returning from one profile.
        val conservative = composerTop != null || phraseTop != null
        val maxReplyTravel = height * 0.42f
        val conservativeUpper = if (conservative) maxOf(upper, guardedLower - maxReplyTravel) else upper
        if (guardedLower - conservativeUpper < minimumTravel) return null

        return if (forward) VerticalPath(guardedLower, conservativeUpper)
        else VerticalPath(conservativeUpper, guardedLower)
    }

    /** Top of the inline reply box. Commenter rows at or below this are the composer, not people. */
    fun composerCeiling(nodes: List<NodeSnapshot>): Int? {
        val width = nodes.filter { it.visible }.maxOfOrNull { it.bounds.right - it.bounds.left } ?: return null
        if (width <= 0) return null
        return nodes.asSequence()
            .filter { it.visible && it.bounds.bottom > it.bounds.top }
            .mapNotNull { node ->
                val labels = listOfNotNull(node.text, node.contentDescription).map(XUiVocabulary::normalize)
                val nodeWidth = node.bounds.right - node.bounds.left
                val wide = nodeWidth >= width * 0.45f
                val phrase = labels.any { label ->
                    label in inlineReplyLabels ||
                        label.startsWith("yanıtını") ||
                        label.startsWith("post your reply") ||
                        label.contains("tweetini yanıtla") ||
                        label.contains("gönderini yanıtla") ||
                        (wide && (label == "yanıtla" || label == "cevapla" || label == "reply"))
                }
                if (!phrase && !node.editable) return@mapNotNull null
                node.bounds.top
            }
            .minOrNull()
    }

    /**
     * The inline reply bar is not only "Yanıtını gönder". A wide control parked
     * on the bottom of a tweet ("Yanıtla", "Tweetini yanıtla") is the same field.
     * Small reply icons in the thread are not: they fail the width test.
     */
    private fun bottomComposerTop(bounds: Rect, nodes: List<NodeSnapshot>): Float? {
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (width <= 0 || height <= 0) return null
        val floor = bounds.top + height * 0.62f
        return nodes.asSequence()
            .filter { it.visible && it.bounds.right > it.bounds.left && it.bounds.top >= floor }
            .mapNotNull { node ->
                val nodeWidth = node.bounds.right - node.bounds.left
                if (nodeWidth < width * 0.45f) return@mapNotNull null
                val labels = listOfNotNull(node.text, node.contentDescription).map(XUiVocabulary::normalize)
                val phrase = labels.any { label ->
                    label in inlineReplyLabels ||
                        label.startsWith("yanıtını") ||
                        label.startsWith("post your reply") ||
                        label.startsWith("tweet your reply") ||
                        label.contains("tweetini yanıtla") ||
                        label.contains("gönderini yanıtla") ||
                        label == "yanıtla" || label == "cevapla" || label == "reply"
                }
                if (!phrase && !node.editable) return@mapNotNull null
                val nodeHeight = (node.bounds.bottom - node.bounds.top).coerceAtLeast(1)
                val clearance = maxOf(nodeHeight, (height * 0.04f).toInt()).coerceAtLeast(1)
                (node.bounds.top - clearance).toFloat()
            }
            .minOrNull()
    }

    private val sheetRepost = setOf("yeniden gönder", "repost", "retweet")
    private val sheetQuote = setOf("alıntı", "alıntıla", "quote", "quote post", "quote tweet")

    /** The repost menu, not the tweet's own action icon and not a tab. */
    fun repostSheet(nodes: List<NodeSnapshot>): Boolean {
        val labels = nodes.asSequence()
            .filter { it.visible && it.enabled && it.clickable }
            .flatMap { node -> sequenceOf(node.text, node.contentDescription) }
            .mapNotNull { XUiVocabulary.normalize(it).takeIf(String::isNotBlank) }
            .toList()
        val repost = labels.any { it in sheetRepost || it in XUiVocabulary.undoRepostActions }
        val quote = labels.any { it in sheetQuote }
        return repost && quote
    }

    private val actionExact = XUiVocabulary.replyActions +
        XUiVocabulary.repostActions +
        XUiVocabulary.undoRepostActions +
        XUiVocabulary.likeActions +
        XUiVocabulary.unlikeActions +
        XUiVocabulary.bookmarkActions +
        XUiVocabulary.removeBookmarkActions +
        XUiVocabulary.quoteActions +
        setOf("paylaş", "share")

    private val toolExact = setOf(
        "gif", "grok", "görsel", "gorsel", "medya", "emoji", "anket", "poll",
        "galeri", "gallery", "fotoğraf", "fotograf",
    )

    /** Reply, repost, GIF, Grok and the comment avatar. A down event on these is a tap. */
    fun blocksGesture(node: NodeSnapshot, screenWidth: Int, screenTop: Int, screenHeight: Int): Boolean {
        if (isActionControl(node, screenWidth)) return true
        if (!node.visible || !node.enabled || !node.clickable || screenWidth <= 0 || screenHeight <= 0) return false
        val width = node.bounds.right - node.bounds.left
        val height = node.bounds.bottom - node.bounds.top
        if (width <= 0 || height <= 0) return false
        val labels = listOfNotNull(node.text, node.contentDescription).map(XUiVocabulary::normalize)
        val id = node.viewId.orEmpty().lowercase()
        val tool = labels.any { label ->
            label in toolExact || toolExact.any { marker -> label.startsWith("$marker ") || label.startsWith("$marker,") }
        } || listOf("gif", "grok", "emoji", "gallery", "media_button", "composer").any(id::contains)
        if (tool && width <= screenWidth * 0.42f) return true
        val bottomBand = node.bounds.top >= screenTop + screenHeight * 0.62f
        if (bottomBand && width <= screenWidth * 0.22f && height <= screenHeight * 0.12f) return true
        val square = height.toFloat() / width in 0.70f..1.45f
        val leftColumn = node.bounds.left <= screenWidth * 0.20f && width <= screenWidth * 0.18f
        return square && leftColumn && !bottomBand
    }

    /** Small reply / repost / like icons. A full-width composer is not one of these. */
    fun isActionControl(node: NodeSnapshot, screenWidth: Int): Boolean {
        if (!node.visible || !node.enabled || screenWidth <= 0) return false
        val width = node.bounds.right - node.bounds.left
        val height = node.bounds.bottom - node.bounds.top
        if (width <= 0 || height <= 0 || width > screenWidth * 0.42f) return false
        val labels = listOfNotNull(node.text, node.contentDescription).map(XUiVocabulary::normalize)
        return labels.any { label ->
            label in actionExact || actionExact.any { action ->
                label.startsWith(action) && label.length <= action.length + 16
            }
        }
    }

    /**
     * Left gutter ratios stay put for media. If that line crosses a reply or
     * repost icon, step into the nearest gap that is still left of the picture.
     */
    fun safeX(bounds: Rect, nodes: List<NodeSnapshot>, preferredX: Float, startY: Float, endY: Float): Float {
        val width = bounds.right - bounds.left
        if (width <= 0) return preferredX
        val top = minOf(startY, endY)
        val bottom = maxOf(startY, endY)
        val controls = nodes.filter {
            blocksGesture(it, width, bounds.top, bounds.bottom - bounds.top) &&
                it.bounds.bottom >= top && it.bounds.top <= bottom
        }
        fun hits(x: Float) = controls.any { it.bounds.left <= x && x <= it.bounds.right }
        if (!hits(preferredX)) return preferredX
        val leftmost = controls.minOfOrNull { it.bounds.left }
        if (leftmost != null) {
            val gutter = leftmost - width * 0.025f
            if (gutter >= bounds.left + 4f && !hits(gutter)) return gutter
        }
        val sorted = controls.sortedBy { it.bounds.left }
        for (index in 0 until sorted.lastIndex) {
            val gapStart = sorted[index].bounds.right.toFloat()
            val gapEnd = sorted[index + 1].bounds.left.toFloat()
            val mid = (gapStart + gapEnd) / 2f
            if (gapEnd - gapStart < width * 0.04f) continue
            if (mid > bounds.left + width * 0.36f) continue
            if (!hits(mid)) return mid
        }
        val edge = bounds.left + 6f
        if (!hits(edge)) return edge
        return preferredX
    }

    /** Do not begin or end the stroke inside a reply field or an action icon. */
    fun liftOffControls(bounds: Rect, nodes: List<NodeSnapshot>, x: Float, path: VerticalPath): VerticalPath? {
        val height = (bounds.bottom - bounds.top).toFloat()
        if (height <= 0f) return null
        val gap = (height * 0.02f).coerceAtLeast(8f)
        val controls = nodes.filter { node ->
            val width = bounds.right - bounds.left
            val height = bounds.bottom - bounds.top
            blocksGesture(node, width, bounds.top, height) ||
                (node.visible && node.editable && node.bounds.top > bounds.top + height * 0.55f)
        }
        fun covering(y: Float) = controls.firstOrNull { node ->
            x >= node.bounds.left - 6 && x <= node.bounds.right + 6 &&
                y >= node.bounds.top && y <= node.bounds.bottom
        }
        var start = path.startY
        var end = path.endY
        val downward = start > end
        repeat(4) {
            val hit = covering(start) ?: return@repeat
            start = hit.bounds.top - gap
        }
        repeat(4) {
            val hit = covering(end) ?: return@repeat
            end = if (downward) hit.bounds.bottom + gap else hit.bounds.top - gap
        }
        val minTravel = height * 0.16f
        if (downward) {
            if (start - end < minTravel) end = start - minTravel
            if (covering(start) != null || covering(end) != null) return null
            if (start - end < minTravel) return null
            return VerticalPath(start, end)
        }
        if (end - start < minTravel) return null
        if (covering(start) != null || covering(end) != null) return null
        return VerticalPath(start, end)
    }
}
