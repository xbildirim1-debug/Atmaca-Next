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
        val first = CommentDetailEvidence.header(nodes) ?: return null
        val origin = nodes[first.index].bounds
        val width = nodes.filter(::visible).maxOfOrNull { it.bounds.right } ?: return first
        val compact = nodes.indices.filter { i ->
            val n = nodes[i]
            usable(n) && !editable(n) && n.bounds.top >= origin.top && n.bounds.top <= origin.top + maxOf(100, width / 3) &&
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

    fun ready(nodes: List<NodeSnapshot>, screen: XScreen): Boolean {
        if (editorIndex(nodes) == null) return false
        if (screen in setOf(XScreen.COMPOSER, XScreen.TWEET_DETAIL)) return true
        // The keyboard hides Gönderi/back, so the frame is UNKNOWN while the send button is still there.
        return screen == XScreen.UNKNOWN && submitIndex(nodes) != null
    }

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
        composerSendIndex(nodes)?.let { return it }
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
            // Beside the field, or on the row directly under it (X's blue Yanıtla).
            val inlineAction = node.bounds.left >= (editor.bounds.left + editor.bounds.right) / 2 &&
                node.bounds.bottom >= editor.bounds.top &&
                node.bounds.top <= editor.bounds.bottom + 220
            (knownId || namedButton) && (topAction || inlineAction)
        }.maxByOrNull { if (id(nodes[it]) in submitIds) 2 else 1 }
    }

    /** Blue Yanıtla sits on the GIF row, under the text. The tweet's own Yanıtla does not. */
    private fun composerSendIndex(nodes: List<NodeSnapshot>): Int? {
        val gifs = nodes.filter { node -> usable(node) && !editable(node) && labels(node).any(::isGifTool) }
        if (gifs.isEmpty()) return null
        return nodes.indices.filter { i ->
            val node = nodes[i]
            if (!usable(node) || editable(node) || id(node).startsWith("toolbar_")) return@filter false
            if (!labels(node).any(::isSubmitLabel)) return@filter false
            gifs.any { gif -> sameRow(node, gif) && node.bounds.right > gif.bounds.right + 24 }
        }.minByOrNull { i ->
            val n = nodes[i]
            (n.bounds.right - n.bounds.left).toLong() * (n.bounds.bottom - n.bounds.top)
        }
    }

    private fun isGifTool(label: String) = label == "gif" || label.startsWith("gif,") || label.startsWith("gif.")

    private fun isSubmitLabel(label: String) = XUiVocabulary.postActions.any { action ->
        label == action || Regex("^" + Regex.escape(action) + "(?:[,.]\\s*(?:düğme|button|etkin|enabled))+$").matches(label)
    }

    private fun sameRow(a: NodeSnapshot, b: NodeSnapshot): Boolean {
        val overlap = minOf(a.bounds.bottom, b.bounds.bottom) - maxOf(a.bounds.top, b.bounds.top)
        if (overlap > 0) return true
        val dy = (a.bounds.top + a.bounds.bottom) / 2 - (b.bounds.top + b.bounds.bottom) / 2
        return (if (dy < 0) -dy else dy) <= 140
    }

    fun matchesPost(nodes: List<NodeSnapshot>, expected: DiscoveryTweetOpenRecovery.Attempt): Boolean {
        val replyTop = replySectionTop(nodes)
        fun aboveReplies(top: Int) = replyTop == null || top < replyTop
        val rows = FeedRowEvidence.rows(nodes).filter { aboveReplies(nodes[it.headerIndex].bounds.top) }
        val header = postHeader(nodes)?.takeIf { aboveReplies(nodes[it.index].bounds.top) }
        if (header != null && header.handle != expected.author) return false
        if (header != null) {
            val headerNode = nodes[header.index]
            val textTop = if (headerNode.bounds.bottom - headerNode.bounds.top <= 220) headerNode.bounds.bottom
                else headerNode.bounds.top
            if (textInBand(nodes, textTop, bandEnd(nodes, rows, headerNode.bounds.top, header.handle), expected.text)) return true
        }
        val first = rows.firstOrNull()
        if (first != null) {
            if (first.author != expected.author || first.authorTruncated) return false
            val direct = first.bodyIndex?.let { nodes[it].text ?: nodes[it].contentDescription }.orEmpty()
            if (sameText(expected.text, direct)) return true
            val origin = nodes[first.headerIndex].bounds.top
            if (textInBand(nodes, origin, bandEnd(nodes, rows, origin, first.author), expected.text)) return true
            return false
        }
        if (header != null) return false
        // X often draws the opened name and text without exposing them. Handles under
        // Alakalı belong to replies, not the tweet that was just opened.
        return unlabeledOpenedPost(nodes, expected, replyTop)
    }

    private fun replySectionTop(nodes: List<NodeSnapshot>): Int? = nodes.filter { node ->
        visible(node) && !editable(node) && labels(node).any { label ->
            label in setOf("alakalı", "relevant", "en yeni", "latest", "alıntıları görüntüle", "view quotes")
        }
    }.minOfOrNull { it.bounds.top }

    private fun unlabeledOpenedPost(nodes: List<NodeSnapshot>, expected: DiscoveryTweetOpenRecovery.Attempt, replyTop: Int?): Boolean {
        val titled = nodes.any { visible(it) && !editable(it) && labels(it).any(CommentDetailEvidence::isTitle) }
        // The reply list and the composer are often still off this frame. The detail
        // we just opened is still the profile row that was tapped.
        if (replyTop == null && !titled) return false
        val above = nodes.filter { visible(it) && !editable(it) && (replyTop == null || it.bounds.top < replyTop) }
        val handles = above.flatMap { listOfNotNull(it.text, it.contentDescription) }.mapNotNull { raw ->
            TweetContentEvidence.header(raw)?.handle ?: AccountSwitcherInspector.dedicatedHandle(raw)
        }
        if (handles.any { it != expected.author }) return false
        val bodies = above.flatMap { listOfNotNull(it.text, it.contentDescription) }
            .map { it.trim() }
            .filter { text ->
                text.length >= 40 && TweetContentEvidence.header(text) == null &&
                    !Regex("^\\d{1,2}:\\d{2}\\b").containsMatchIn(text) &&
                    !text.contains("görüntüleme", true) && !text.contains("views", true)
            }
        if (bodies.isEmpty()) return true
        return bodies.any { sameText(expected.text, it) }
    }

    /** Text of this post only: after its author line, before the next other author or the reply chrome. */
    private fun bandEnd(nodes: List<NodeSnapshot>, rows: List<FeedRowEvidence.Row>, originTop: Int, author: String): Int {
        val nextAuthor = rows.firstOrNull { row ->
            nodes[row.headerIndex].bounds.top > originTop && row.author != author
        }?.let { nodes[it.headerIndex].bounds.top } ?: Int.MAX_VALUE
        val chrome = nodes.filter { node ->
            visible(node) && !editable(node) && node.bounds.top > originTop && labels(node).any { label ->
                label in XUiVocabulary.structuralLabels || isEntry(label) ||
                    label in setOf("alakalı", "relevant", "en yeni", "latest", "alıntıları görüntüle", "view quotes")
            }
        }.minOfOrNull { it.bounds.top } ?: Int.MAX_VALUE
        return minOf(nextAuthor, chrome)
    }

    private fun textInBand(nodes: List<NodeSnapshot>, originTop: Int, end: Int, expected: String): Boolean {
        val body = nodes.filter { node ->
            visible(node) && !editable(node) && node.bounds.top >= originTop && node.bounds.top < end &&
                node.bounds.bottom > originTop && !id(node).startsWith("toolbar_") &&
                labels(node).none { label -> label in XUiVocabulary.structuralLabels || isEntry(label) }
        }
        if (body.any { listOfNotNull(it.text, it.contentDescription).any { text -> sameText(expected, text) } }) return true
        val pieces = body.sortedWith(compareBy<NodeSnapshot> { it.bounds.top }.thenBy { it.bounds.left })
            .mapNotNull { (it.text ?: it.contentDescription)?.takeIf(String::isNotBlank) }
            .filter { TweetContentEvidence.header(it) == null }
            .distinct()
        return pieces.size > 1 && sameText(expected, pieces.joinToString(" "))
    }

    private fun sameText(before: String, after: String): Boolean {
        val a = normal(before).replace(Regex("(?i)\\s*(?:daha fazlasını göster|show more)\\s*$"), "").trimEnd('.', '…', ' ')
        val b = normal(after)
        return a.isNotBlank() && (a == b || (a.length >= 40 && (b.startsWith(a) || b.contains(a))))
    }

    fun hasSentNotice(nodes: List<NodeSnapshot>): Boolean = nodes.any { node ->
        visible(node) && !editable(node) && !listOf("tweet_text", "tweet_content", "status_text").any(id(node)::contains) &&
            labels(node).any { label -> sentNotices.any { notice ->
                label == notice || label.startsWith(notice) || (notice.length >= 12 && label.contains(notice))
            } }
    }

    private val sentNotices = listOf(
        "yanıtın gönderildi", "yanıtınız gönderildi", "yanıt gönderildi",
        "gönderin gönderildi", "gönderiniz gönderildi", "gönderi gönderildi",
        "your reply was sent", "your post was sent", "your tweet was sent", "reply sent", "post sent",
    )

    /**
     * Yanıtla was tapped and the draft left the field. X keeps the tweet's own
     * Yanıtla button, so a remaining toolbar is not proof the send failed.
     */
    fun replyLanded(nodes: List<NodeSnapshot>, content: String): Boolean {
        if (content.isBlank() || contains(nodes, content) || nodes.none(::visible)) return false
        val editor = editorIndex(nodes)?.let(nodes::get)
        if (editor != null) {
            val text = editor.text.orEmpty()
            return text.isBlank() || isEntry(text)
        }
        return nodes.any { node ->
            visible(node) && (labels(node).any(::isEntry) || labels(node).any(CommentDetailEvidence::isTitle))
        }
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
