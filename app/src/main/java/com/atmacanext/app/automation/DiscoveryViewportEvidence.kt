package com.atmacanext.app.automation

internal object DiscoveryViewportEvidence {
    /**
     * A discovery scroll is accepted only when the visible accessibility geometry
     * changes. Keep media geometry in the signature: on X, image/video/quote cards
     * can be the only visible evidence that the viewport actually moved. Hidden
     * pages and inline editable reply controls are excluded.
     */
    fun signature(nodes: List<NodeSnapshot>): String = nodes.asSequence()
        .filter {
            it.visible && !it.editable &&
                it.bounds.right > it.bounds.left && it.bounds.bottom > it.bounds.top
        }
        .map {
            "${it.text.orEmpty()}|${it.contentDescription.orEmpty()}|${it.viewId.orEmpty()}@" +
                "${it.bounds.left}:${it.bounds.top}:${it.bounds.right}:${it.bounds.bottom}"
        }
        .joinToString("\n")
        .hashCode()
        .toString(16)

    // Keep the working scan allowance. A transient unchanged read is not list end.
    fun quotesExhausted(scans: Int, stableReads: Int): Boolean = scans >= 12 || stableReads >= 3

    fun returnedToParent(
        before: String,
        current: String,
        beforeKeys: List<String>,
        currentKeys: List<String>,
        openedAuthor: String?,
        child: String?,
    ): Boolean = before.isNotBlank() && openedAuthor != child &&
        (before == current ||
            (beforeKeys.isNotEmpty() && (openedAuthor != null || beforeKeys.size >= 2) && beforeKeys == currentKeys))
}
