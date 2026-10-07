package com.atmacanext.app.automation

/** Selects the real reply controls, including X's inline editor on a post detail. */
internal object ReplyComposerEvidence {
    private val entries = setOf("yanıtını gönder", "yanıt gönder", "post your reply", "cevabını gönder")
    private val editorIds = setOf("tweet_box", "post_text", "reply_text", "composer_edit_text")
    private val submitIds = setOf("tweet_button", "post_button", "send_tweet", "reply_button")
    private fun id(node: NodeSnapshot) = node.viewId.orEmpty().substringAfterLast('/').lowercase()
    private fun labels(node: NodeSnapshot) = listOfNotNull(node.text, node.contentDescription).map(XUiVocabulary::normalize)
    private fun visible(node: NodeSnapshot) = node.visible && node.bounds.right > node.bounds.left && node.bounds.bottom > node.bounds.top
    private fun usable(node: NodeSnapshot) = visible(node) && node.enabled
    private fun editable(node: NodeSnapshot) = node.editable || node.className.orEmpty().contains("EditText", true) ||
        id(node) in setOf("tweet_box", "reply_text", "composer_edit_text")
    private fun normal(text: String) = text.replace(Regex("\\s+"), " ").trim()

    fun isEntry(raw: String): Boolean {
        val text = XUiVocabulary.normalize(raw).replace(Regex("\\s+"), " ")
        return entries.any { entry -> text == entry || Regex("^" + Regex.escape(entry) +
            "(?:[,.]\\s*(?:düzenleme kutusu|metin alanı|edit box|text field|düğme|button))+$").matches(text) }
    }

    private fun inlineIndex(nodes: List<NodeSnapshot>): Int? = nodes.indices.filter { i ->
        val node = nodes[i]
        val width = nodes.filter(::visible).maxOfOrNull { it.bounds.right } ?: 0
        usable(node) && labels(node).any(::isEntry) && node.bounds.bottom - node.bounds.top <= maxOf(120, width / 3)
    }.sortedWith(compareByDescending<Int> { nodes[it].bounds.top }
        .thenBy { (nodes[it].bounds.right - nodes[it].bounds.left).toLong() * (nodes[it].bounds.bottom - nodes[it].bounds.top) })
        .firstOrNull()

    fun focusIndex(nodes: List<NodeSnapshot>): Int? = editorIndex(nodes) ?: inlineIndex(nodes)

    /** A merged author parent can cover the whole post. Prefer its compact handle child. */
    fun postHeader(nodes: List<NodeSnapshot>): CommentDetailEvidence.Header? {
        val first = CommentDetailEvidence.header(nodes, excludeFeedTimedHeaders = false) ?: return null
        val origin = nodes[first.index].bounds
        val width = nodes.filter(::visible).maxOfOrNull { it.bounds.right } ?: return first
        val headerEnd = CommentDetailEvidence.headerBandEnd(nodes, origin.top)
        val compact = nodes.indices.filter { i ->
            val n = nodes[i]
            usable(n) && !editable(n) && n.bounds.top >= origin.top && n.bounds.top < headerEnd &&
                n.bounds.top <= origin.top + maxOf(100, width / 3) &&
                n.bounds.bottom - n.bounds.top < origin.bottom - origin.top &&
                listOfNotNull(n.text, n.contentDescription).any { AccountSwitcherInspector.dedicatedHandle(it) == first.handle }
        }.minByOrNull { (nodes[it].bounds.right - nodes[it].bounds.left).toLong() * (nodes[it].bounds.bottom - nodes[it].bounds.top) }
        return compact?.let { CommentDetailEvidence.Header(first.handle, it) } ?: first
    }

    fun editorIndex(nodes: List<NodeSnapshot>): Int? {
        val editors = nodes.indices.filter { i ->
            val node = nodes[i]
            usable(node) && editable(node) && !listOf("search", "password", "login", "username").any(id(node)::contains)
        }
        val strong = editors.filter { id(nodes[it]) in editorIds || labels(nodes[it]).any(::isEntry) }
        return strong.singleOrNull()
            ?: strong.groupBy { i -> nodes[i].let { n ->
                listOf(n.bounds.left, n.bounds.top, n.bounds.right, n.bounds.bottom) to normal(n.text.orEmpty()) }
            }.values.singleOrNull()?.maxByOrNull { if (nodes[it].editable) 2 else if (nodes[it].className.orEmpty().contains("EditText", true)) 1 else 0 }
            ?: editors.singleOrNull()
    }

    fun ready(nodes: List<NodeSnapshot>, screen: XScreen): Boolean =
        (screen in setOf(XScreen.COMPOSER, XScreen.TWEET_DETAIL) ||
            (screen == XScreen.UNKNOWN && postHeader(nodes) != null)) && editorIndex(nodes) != null

    fun contains(nodes: List<NodeSnapshot>, content: String): Boolean {
        val index = editorIndex(nodes) ?: return false
        return content.isNotBlank() && normal(nodes[index].text.orEmpty()) == normal(content)
    }

    fun openIndex(nodes: List<NodeSnapshot>, retry: Boolean = false): Int? {
        val inline = inlineIndex(nodes)
        val toolbar = nodes.indices.filter { i ->
            val n = nodes[i]
            usable(n) && !editable(n) && (id(n) == "toolbar_reply" ||
                ((n.clickable || n.className.orEmpty().contains("Button", true)) && labels(n).any { label ->
                    XUiVocabulary.replyActions.any { label == it || label.startsWith(it + ",") || label.startsWith(it + ".") }
                }))
        }.minByOrNull { nodes[it].bounds.top }
        // Retry the alternate control if an accepted toolbar click did not open a form.
        return if (retry) toolbar ?: inline else inline ?: toolbar
    }

    fun submitIndex(nodes: List<NodeSnapshot>): Int? {
        val editor = editorIndex(nodes)?.let(nodes::get) ?: return null
        val rightEdge = nodes.filter(::visible).maxOfOrNull { it.bounds.right } ?: return null
        val postHeader = postHeader(nodes)?.let { nodes[it.index] }
        val hasReplyRows = FeedRowEvidence.rows(nodes).isNotEmpty()
        return nodes.indices.filter { i ->
            val node = nodes[i]
            if (!usable(node) || editable(node) || id(node).startsWith("toolbar_")) return@filter false
            val knownId = id(node) in submitIds
            // Compose can put the label on a non-clickable child of the button.
            // A tap on that child's live rectangle reaches its owning button.
            val namedButton = labels(node).any { label ->
                XUiVocabulary.postActions.any { action -> label == action ||
                    Regex("^" + Regex.escape(action) + "(?:[,.]\\s*(?:düğme|button|etkin|enabled))+$").matches(label) }
            }
            // Submit is above the form at its right edge, or beside an inline editor.
            val topAction = node.bounds.bottom <= editor.bounds.top && node.bounds.left >= rightEdge / 2 &&
                (postHeader?.let { node.bounds.bottom <= it.bounds.top } ?: !hasReplyRows)
            val inlineAction = node.bounds.left >= (editor.bounds.left + editor.bounds.right) / 2 &&
                node.bounds.top <= editor.bounds.bottom && node.bounds.bottom >= editor.bounds.top
            (knownId || namedButton) && (topAction || inlineAction)
        }.maxByOrNull { if (id(nodes[it]) in submitIds) 2 else 1 }
    }

    fun matchesPost(nodes: List<NodeSnapshot>, expected: DiscoveryTweetOpenRecovery.Attempt): Boolean {
        val rows = FeedRowEvidence.rows(nodes)
        val header = postHeader(nodes)
        if (header != null) {
            if (header.handle != expected.author) return false
            val h = nodes[header.index].bounds
            val bodyTop = minOf(h.bottom, CommentDetailEvidence.headerBandEnd(nodes, h.top))
            val end = rows.firstOrNull { nodes[it.headerIndex].bounds.top > nodes[header.index].bounds.top }
                ?.let { nodes[it.headerIndex].bounds.top } ?: Int.MAX_VALUE
            val body = nodes.filter { visible(it) && !editable(it) && it.bounds.top >= bodyTop &&
                it.bounds.bottom <= end && !id(it).startsWith("toolbar_") &&
                labels(it).none { label -> label in XUiVocabulary.structuralLabels } }
            if (body.any { listOfNotNull(it.text, it.contentDescription).any { text -> sameText(expected.text, text) } }) return true
            // Long posts may expose each paragraph as a separate visible text node.
            // Join only this post's band, stopping before the first reply header.
            val pieces = body.sortedWith(compareBy<NodeSnapshot> { it.bounds.top }.thenBy { it.bounds.left })
                .mapNotNull { (it.text ?: it.contentDescription)?.takeIf(String::isNotBlank) }.distinct()
            return pieces.size > 1 && sameText(expected.text, pieces.joinToString(" "))
        }
        // A titled detail with no main author must not borrow its first reply.
        if (nodes.any { visible(it) && listOfNotNull(it.text, it.contentDescription).any(CommentDetailEvidence::isTitle) }) return false
        val first = rows.firstOrNull()
        if (first != null) {
            if (first.author != expected.author || first.authorTruncated) return false
            val body = first.bodyIndex?.let { nodes[it].text ?: nodes[it].contentDescription }.orEmpty()
            if (sameText(expected.text, body)) return true
        }
        return false
    }

    private fun sameText(before: String, after: String): Boolean {
        val a = normal(before).replace(Regex("(?i)\\s*(?:daha fazlasını göster|show more)\\s*$"), "").trimEnd('.', '…', ' ')
        val b = normal(after)
        return a.isNotBlank() && (a == b || (a.length >= 40 && b.startsWith(a)))
    }

    fun hasSentNotice(nodes: List<NodeSnapshot>): Boolean = nodes.any { node ->
        visible(node) && !editable(node) && !listOf("tweet_text", "tweet_content", "status_text").any(id(node)::contains) &&
            labels(node).any { label -> setOf(
            "yanıtın gönderildi", "yanıtınız gönderildi", "gönderin gönderildi", "gönderiniz gönderildi",
            "gönderi gönderildi", "your reply was sent", "your post was sent", "your tweet was sent", "reply sent", "post sent",
        ).any { notice -> label == notice || Regex("^" + Regex.escape(notice) + "(?:[,.]?\\s*(?:görüntüle|view|göster))?[.!]?$" ).matches(label) } }
    }

    fun ownReplyKeys(nodes: List<NodeSnapshot>, username: String, content: String): Set<String> =
        FeedRowEvidence.rows(nodes).filter { row ->
            row.author == XIdentityDetector.normalizeUsername(username) && !row.authorTruncated &&
                row.bodyIndex?.let { normal(nodes[it].text ?: nodes[it].contentDescription.orEmpty()) == normal(content) } == true
        }.mapTo(linkedSetOf()) { it.key }

    /** Inline forms may stay open after sending; merely seeing TWEET_DETAIL is not success. */
    fun inlineCleared(nodes: List<NodeSnapshot>, content: String): Boolean {
        if (contains(nodes, content)) return false
        val editor = editorIndex(nodes)?.let(nodes::get)
        val emptyEntry = editor?.let { it.text.isNullOrBlank() || isEntry(it.text.orEmpty()) } == true ||
            nodes.any { visible(it) && labels(it).any(::isEntry) }
        return emptyEntry && submitIndex(nodes) == null
    }
}
