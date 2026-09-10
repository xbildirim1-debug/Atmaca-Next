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
    fun verticalPath(bounds: Rect, nodes: List<NodeSnapshot>, forward: Boolean): VerticalPath? {
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (width <= 0 || height <= 0) return null

        val upper = bounds.top + height * FORWARD_END_Y_RATIO
        val defaultLower = bounds.top + height * FORWARD_START_Y_RATIO
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
        val minimumTravel = height * 0.16f
        if (lower - upper < minimumTravel) return null

        // Composer evidence is a strong signal that this is the tweet-detail reply
        // viewport. Preserve overlap there so a single long swipe cannot jump over
        // several short commenters after returning from one profile.
        val conservative = composerTop != null
        val maxReplyTravel = height * 0.42f
        val conservativeUpper = if (conservative) maxOf(upper, lower - maxReplyTravel) else upper
        if (lower - conservativeUpper < minimumTravel) return null

        return if (forward) VerticalPath(lower, conservativeUpper)
        else VerticalPath(conservativeUpper, lower)
    }
}
