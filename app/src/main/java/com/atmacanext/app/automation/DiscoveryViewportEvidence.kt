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

    /**
     * Detect the Back transition from an opened commenter post/profile to the
     * original reply thread.
     *
     * X/Compose is allowed to rebuild the parent viewport while Back is settling:
     * autoplay media, reply hydration and lazy-row composition can change both the
     * accessibility signature and the exact visible key list even though we are
     * back on the same tweet. Requiring complete equality caused a valid return to
     * be rejected and, after the timeout, the runtime abandoned a large comment
     * thread and searched the target account again.
     *
     * The child must no longer be the detail header. After that, an exact viewport
     * match is sufficient; otherwise stable overlap with the rows that existed
     * before opening the commenter proves that this is the same parent thread.
     * When the header is temporarily unavailable we require at least two shared
     * row keys, avoiding a false positive from the child row alone.
     */
    fun returnedToParent(
        before: String,
        current: String,
        beforeKeys: List<String>,
        currentKeys: List<String>,
        openedAuthor: String?,
        child: String?,
    ): Boolean {
        if (before.isBlank() || child.isNullOrBlank()) return false
        if (openedAuthor == child) return false
        if (before == current) return true

        val previous = beforeKeys.filter(String::isNotBlank).toSet()
        val now = currentKeys.filter(String::isNotBlank).toSet()
        if (previous.isEmpty() || now.isEmpty()) return false

        val sharedCount = previous.intersect(now).size
        return if (openedAuthor != null) sharedCount >= 1 else sharedCount >= 2
    }
}
