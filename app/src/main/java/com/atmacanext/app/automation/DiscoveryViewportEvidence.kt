package com.atmacanext.app.automation

internal object DiscoveryViewportEvidence {
    /** Include visible media geometry; ignore keyboard/inline input and hidden old pages. */
    fun signature(nodes: List<NodeSnapshot>): String = nodes.asSequence()
        .filter { it.visible && !it.editable && it.bounds.right > it.bounds.left && it.bounds.bottom > it.bounds.top }
        .map { "${it.text.orEmpty()}|${it.contentDescription.orEmpty()}|${it.viewId.orEmpty()}@${it.bounds.left}:${it.bounds.top}:${it.bounds.right}:${it.bounds.bottom}" }
        .joinToString("\n").hashCode().toString(16)

    // Keep the working 26.20 scan allowance. A transient unchanged read is not list end.
    fun quotesExhausted(scans: Int, stableReads: Int): Boolean = scans >= 12 || stableReads >= 3

    fun returnedToParent(before: String, current: String, beforeKeys: List<String>, currentKeys: List<String>,
                         openedAuthor: String?, child: String?): Boolean =
        before.isNotBlank() && openedAuthor != child &&
            (before == current || (beforeKeys.isNotEmpty() && (openedAuthor != null || beforeKeys.size >= 2) && beforeKeys == currentKeys))
}
