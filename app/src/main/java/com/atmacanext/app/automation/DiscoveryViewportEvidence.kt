package com.atmacanext.app.automation

internal object DiscoveryViewportEvidence {
    /**
     * Track real content movement, not video/player animation. Prefer semantic post
     * rows and their vertical positions; fall back to stable text only when X has
     * not exposed rows yet. This keeps image/video/quote layouts from faking a
     * successful scroll or an endless changing viewport.
     */
    fun signature(nodes: List<NodeSnapshot>): String {
        val visible = nodes.filter { it.visible && !it.editable &&
            it.bounds.right > it.bounds.left && it.bounds.bottom > it.bounds.top }
        val rows = FeedRowEvidence.rows(visible)
        if (rows.isNotEmpty()) {
            val material = buildString {
                rows.take(16).forEach { row ->
                    val h = visible[row.headerIndex]
                    append(row.author)
                    append('|')
                    append(row.key)
                    append('@')
                    append(h.bounds.top)
                    append(':')
                    append(h.bounds.bottom)
                    append('\n')
                }
                if (ReplyThreadEndEvidence.visible(visible)) append("<THREAD_END>")
            }
            return material.hashCode().toString(16)
        }

        val material = visible.asSequence()
            .filterNot { node ->
                val id = node.viewId.orEmpty().lowercase()
                val clazz = node.className.orEmpty().lowercase()
                listOf(
                    "video", "player", "media", "image", "photo", "gif", "progress",
                    "toolbar_like", "toolbar_retweet", "toolbar_reply", "bookmark",
                ).any { token -> id.contains(token) || clazz.contains(token) }
            }
            .mapNotNull { node ->
                val label = listOfNotNull(node.text, node.contentDescription)
                    .joinToString(" ").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                "$label@${node.bounds.top}:${node.bounds.bottom}"
            }
            .take(80)
            .joinToString("\n")
        return material.hashCode().toString(16)
    }

    // Keep the working 26.20 scan allowance. A transient unchanged read is not list end.
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
