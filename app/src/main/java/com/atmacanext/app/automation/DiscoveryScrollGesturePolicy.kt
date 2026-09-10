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
     * The default 88%-to-20% gesture can start inside X's bottom inline reply
     * composer. On some X builds that focuses/clicks the composer instead of
     * scrolling the thread. Derive the lower gesture edge from the live
     * accessibility tree and stop above any editable/reply-composer surface.
     *
     * No account, screen resolution or fixed pixel coordinate is encoded here.
     * Direct rectangle fields are used instead of Android Rect helpers so this
     * policy remains deterministic in local JVM regression tests as well.
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

                // If only the text label is exposed, its bounds sit inside the
                // rounded composer. Use one label-height as a semantic clearance.
                // Editable/container nodes already describe the surface itself.
                val nodeHeight = (node.bounds.bottom - node.bounds.top).coerceAtLeast(1)
                val clearance = if (node.editable || idEvidence) {
                    (nodeHeight / 4).coerceAtLeast(1)
                } else {
                    nodeHeight
                }
                (node.bounds.top - clearance).toFloat()
            }
            .minOrNull()

        val lower = minOf(defaultLower, composerTop ?: defaultLower)
        val minimumTravel = height * 0.16f
        if (lower - upper < minimumTravel) return null

        return if (forward) VerticalPath(lower, upper) else VerticalPath(upper, lower)
    }
}
