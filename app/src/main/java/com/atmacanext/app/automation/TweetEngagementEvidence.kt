package com.atmacanext.app.automation

/** Select the opened post's first visible action row, never a reply's selected button. */
internal object TweetEngagementEvidence {
    enum class Action { LIKE, REPOST, BOOKMARK }
    data class Control(val index: Int, val done: Boolean)
    private fun labels(n: NodeSnapshot) = listOfNotNull(n.text, n.contentDescription).map(XUiVocabulary::normalize)
    private fun usable(n: NodeSnapshot) = n.visible && n.enabled && !n.editable && n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top
    private fun matches(raw: String, words: Set<String>): Boolean = words.any {
        VerifiedFollowPolicy.matchesAction(raw, setOf(it)) || raw == it || raw.startsWith("$it,") || raw.startsWith("$it.")
    }
    private fun positive(a: Action) = when(a) {
        Action.LIKE -> XUiVocabulary.likeActions
        Action.REPOST -> XUiVocabulary.repostActions
        Action.BOOKMARK -> XUiVocabulary.bookmarkActions + setOf("yer işareti", "yer işareti ekle", "gönderiyi kaydet", "add bookmark")
    }
    private fun negative(a: Action) = when(a) {
        Action.LIKE -> XUiVocabulary.unlikeActions + setOf("beğenildi", "liked")
        Action.REPOST -> XUiVocabulary.undoRepostActions + setOf("yeniden gönderildi", "retweeted", "reposted")
        Action.BOOKMARK -> XUiVocabulary.removeBookmarkActions + setOf("kaydedildi", "bookmarked", "yer işaretini kaldır", "remove from bookmarks")
    }
    private fun kind(n: NodeSnapshot, a: Action): Boolean {
        if (!usable(n)) return false
        val id = n.viewId.orEmpty().substringAfterLast('/').lowercase()
        val ids = when(a) { Action.LIKE -> listOf("toolbar_like", "like_button", "favorite", "unlike", "liked")
            Action.REPOST -> listOf("toolbar_retweet", "retweet", "repost")
            Action.BOOKMARK -> listOf("bookmark", "save_button") }
        val widget = n.clickable || n.className.orEmpty().contains("button", true) || n.className.orEmpty().contains("image", true)
        return (ids.any(id::contains) && !id.contains("count") && !id.contains("stat")) ||
            (widget && labels(n).any { matches(it, positive(a) + negative(a)) })
    }
    fun control(nodes: List<NodeSnapshot>, action: Action): Control? {
        val all = nodes.indices.filter { i -> Action.entries.any { kind(nodes[i], it) } }
        val top = all.minByOrNull { nodes[it].bounds.top } ?: return null
        val first = nodes[top].bounds
        val row = all.filter { i -> maxOf(nodes[i].bounds.top, first.top) < minOf(nodes[i].bounds.bottom, first.bottom) }
        if (Action.entries.count { a -> row.any { kind(nodes[it], a) } } < 2) return null
        val candidates = row.filter { kind(nodes[it], action) }
        val index = candidates.minByOrNull { (nodes[it].bounds.right - nodes[it].bounds.left).toLong() * (nodes[it].bounds.bottom - nodes[it].bounds.top) } ?: return null
        val done = candidates.any { i -> nodes[i].selected || nodes[i].checked || labels(nodes[i]).any { matches(it, negative(action)) } }
        return Control(index, done)
    }
    private val quote = setOf("alıntı", "alıntıla", "quote", "quote post", "quote tweet")
    private fun sheetRows(nodes: List<NodeSnapshot>): List<Int> = nodes.indices.filter { i ->
        val r = nodes[i]
        usable(r) && labels(r).any { it in XUiVocabulary.repostActions || it in XUiVocabulary.undoRepostActions } &&
            nodes.any { q -> usable(q) && labels(q).any { it in quote } && q.bounds.top >= r.bounds.bottom &&
                kotlin.math.abs(q.bounds.left - r.bounds.left) <= maxOf(q.bounds.bottom - q.bounds.top, r.bounds.bottom - r.bounds.top) }
    }
    fun repostSheet(nodes: List<NodeSnapshot>): Boolean = sheetRows(nodes).isNotEmpty()
    fun repostConfirmation(nodes: List<NodeSnapshot>): Int? {
        return sheetRows(nodes).filter { i -> labels(nodes[i]).any { it in XUiVocabulary.repostActions } }
            .maxByOrNull { nodes[it].bounds.top }
    }
    fun undoRepostSheet(nodes: List<NodeSnapshot>): Boolean = sheetRows(nodes).any { i -> labels(nodes[i]).any { it in XUiVocabulary.undoRepostActions } }
}
