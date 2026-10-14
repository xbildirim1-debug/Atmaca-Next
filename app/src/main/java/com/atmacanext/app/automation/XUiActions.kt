package com.atmacanext.app.automation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Versioned semantic selectors for the official X Android app.
 * Every call receives the current root; no AccessibilityNodeInfo is cached across events.
 */
object XUiActions {
    data class RelationshipTarget(
        val handle: String,
        val button: AccessibilityNodeInfo,
        val labelBefore: String,
    )

    fun clickDrawer(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        return XNavigator.execute(
            service = service,
            root = root,
            command = NavigationCommand.OPEN_ACCOUNT_DRAWER,
            targetUsername = "",
            detectedUsername = null,
        )
    }

    fun clickAccountSwitcher(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        targetUsername: String = "",
    ): Boolean {
        return XNavigator.execute(
            service = service,
            root = root,
            command = NavigationCommand.OPEN_ACCOUNT_SWITCHER,
            targetUsername = targetUsername,
            detectedUsername = null,
        )
    }

    fun clickSourceProfile(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, username: String): Boolean {
        val nodes = AccessibilityTree.nodes(root)
        val index = SourceProfileTarget.index(nodes.map { it.toSnapshot() }, username) ?: return false
        // A generic clickable ancestor may include Follow/Following. Tap only the
        // live username bounds; the runtime must still verify the resulting profile.
        val accepted = GestureClick.gestureTap(service, nodes[index])
        return accepted
    }

    fun clickExactHandle(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, username: String): Boolean {
        return XNavigator.clickExactHandle(service, root, username)
    }

    fun findRelationshipTarget(
        root: AccessibilityNodeInfo?,
        acceptedLabels: Set<String>,
        excludedHandles: Set<String>,
        fromBottom: Boolean,
    ): RelationshipTarget? {
        if (root == null) return null
        val accepted = acceptedLabels.map(XUiVocabulary::normalize).toSet()
        val strictMatches = AccessibilityTree.nodes(root, maxNodes = 1_000).mapNotNull { node ->
            if (!isRelationshipActionNode(node)) return@mapNotNull null
            if (accepted == VerifiedFollowPolicy.availableFollowLabels &&
                !VerifiedFollowPolicy.isAvailableFollow(labels(node))) return@mapNotNull null
            if (accepted == VerifiedFollowPolicy.plainFollowLabels && !VerifiedFollowPolicy.isPlainFollow(labels(node))) return@mapNotNull null
            val label = normalizedLabel(node) ?: return@mapNotNull null
            if (!matchesActionLabel(label, accepted)) return@mapNotNull null

            // The clickable ancestor is used only to discover the owning user row.
            // Never store it as the action target: on X/Compose that ancestor may be
            // the whole row, and ACTION_CLICK on it can open the profile/list instead
            // of pressing the visible Follow button.
            val rowAnchor = clickableAncestor(node) ?: node.takeIf { it.isEnabled } ?: return@mapNotNull null
            if (accepted == VerifiedFollowPolicy.availableFollowLabels &&
                !VerifiedFollowPolicy.isAvailableFollow(labels(node) + labels(clickableAncestor(node) ?: node))) return@mapNotNull null
            if (accepted == VerifiedFollowPolicy.plainFollowLabels &&
                !VerifiedFollowPolicy.isPlainFollow(labels(node) + labels(rowAnchor))) return@mapNotNull null
            val row = userRow(rowAnchor) ?: return@mapNotNull null
            val handles = rowHandles(row)
            if (handles.size != 1) return@mapNotNull null
            val handle = handles.single()
            if (handle in excludedHandles) return@mapNotNull null
            val bounds = android.graphics.Rect().also(row::getBoundsInScreen)
            Triple(bounds.top, handle, RelationshipTarget(handle, node, label))
        }.distinctBy { it.second }
        val relaxedMatches = relaxedRelationshipTargets(root, accepted, excludedHandles)
            .map { target ->
                val bounds = android.graphics.Rect().also(target.button::getBoundsInScreen)
                Triple(bounds.top, target.handle, target)
            }
        val matches = (strictMatches + relaxedMatches).distinctBy { it.second }
        return if (fromBottom) matches.maxByOrNull { it.first }?.third else matches.minByOrNull { it.first }?.third
    }

    fun clickRelationship(service: AtmacaAccessibilityService, target: RelationshipTarget): Boolean =
        // Tap the real action node bounds. GestureClick.click() is intentionally not
        // used here because its parent climb is useful for generic controls but is
        // unsafe for Compose relationship rows where the parent itself opens a profile.
        GestureClick.gestureTap(service, target.button)

    fun rowHasAny(root: AccessibilityNodeInfo?, handle: String, vocabulary: Set<String>): Boolean {
        val wanted = XIdentityDetector.normalizeUsername(handle)
        val normalized = vocabulary.map(XUiVocabulary::normalize).toSet()
        val handles = AccessibilityTree.nodes(root, maxNodes = 1_000).filter { node ->
            node.isVisibleToUser && labels(node).any { AccountSwitcherInspector.dedicatedHandle(it) == wanted }
        }
        for (handleNode in handles) {
            var row: AccessibilityNodeInfo? = handleNode
            repeat(7) {
                val current = row ?: return@repeat
                val descendants = AccessibilityTree.nodes(current, maxNodes = 100)
                if (descendants.size >= 100 || rowHandles(current) != setOf(wanted)) {
                    row = null
                    return@repeat
                }
                val actions = descendants.filter { isRelationshipActionNode(it, observation = true) }
                if (actions.isNotEmpty()) {
                    return actions.any { action ->
                        if (normalized == VerifiedFollowPolicy.availableFollowLabels) VerifiedFollowPolicy.isAvailableFollow(labels(action) + (clickableAncestor(action)?.let(::labels) ?: emptyList()))
                        else if (normalized == VerifiedFollowPolicy.plainFollowLabels) VerifiedFollowPolicy.isPlainFollow(labels(action) + (clickableAncestor(action)?.let(::labels) ?: emptyList()))
                        else labels(action).any { matchesActionLabel(XUiVocabulary.normalize(it), normalized) }
                    }
                }
                row = current.parent
            }
        }
        return relaxedRelationshipTargets(root, normalized, emptySet(), observation = true).any { it.handle == wanted }
    }

    private fun isRelationshipActionNode(node: AccessibilityNodeInfo, observation: Boolean = false): Boolean =
        RelationshipActionEvidence.matches(node.toSnapshot(), node.parent?.isClickable == true, observation)

    fun clickUnfollowConfirmation(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        clickByIdOrLabel(service, root, listOf("unfollow"), XUiVocabulary.unfollowConfirmationActions)

    fun clickLike(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        clickByIdOrLabel(service, root, listOf("toolbar_like", "like_button", "favorite"), XUiVocabulary.likeActions)

    fun isLiked(root: AccessibilityNodeInfo?): Boolean =
        hasIdOrLabel(root, listOf("unlike", "liked"), XUiVocabulary.unlikeActions) || selectedNode(root, listOf("like", "favorite"))

    fun clickRepost(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        clickByIdOrLabel(service, root, listOf("toolbar_retweet", "retweet", "repost"), XUiVocabulary.repostActions)

    fun clickRepostConfirmation(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val node = findBottomExactAction(root, XUiVocabulary.repostActions) ?: return false
        return GestureClick.click(service, node)
    }

    fun isReposted(root: AccessibilityNodeInfo?): Boolean =
        hasIdOrLabel(root, listOf("undo_retweet", "undo_repost", "retweeted"), XUiVocabulary.undoRepostActions) || selectedNode(root, listOf("retweet", "repost"))

    fun clickBookmark(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        clickByIdOrLabel(service, root, listOf("bookmark", "save"), XUiVocabulary.bookmarkActions)

    fun isBookmarked(root: AccessibilityNodeInfo?): Boolean =
        hasIdOrLabel(root, listOf("remove_bookmark", "bookmarked"), XUiVocabulary.removeBookmarkActions) || selectedNode(root, listOf("bookmark"))

    fun clickReply(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        clickByIdOrLabel(service, root, listOf("toolbar_reply", "reply"), XUiVocabulary.replyActions)

    fun openReplyComposer(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, retry: Boolean = false,
        capturedNodes: List<AccessibilityNodeInfo>? = null): Boolean {
        val nodes = capturedNodes ?: AccessibilityTree.nodes(root, maxNodes = 2_000)
        val index = ReplyComposerEvidence.openIndex(nodes.map { it.toSnapshot() }, retry) ?: return false
        val node = nodes[index]
        if (listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).any(ReplyComposerEvidence::isEntry))
            return GestureClick.gestureTapLeading(service, node)
        return (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) || GestureClick.gestureTap(service, node)
    }

    fun focusReplyEditor(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?,
        capturedNodes: List<AccessibilityNodeInfo>? = null): Boolean {
        val nodes = capturedNodes ?: AccessibilityTree.nodes(root, maxNodes = 2_000)
        val node = ReplyComposerEvidence.focusIndex(nodes.map { it.toSnapshot() })?.let(nodes::get) ?: return false
        // ACTION_FOCUS can return true without opening Compose's real input connection.
        return node.isFocused || GestureClick.gestureTapLeading(service, node)
    }

    fun setReplyText(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, value: String,
        capturedNodes: List<AccessibilityNodeInfo>? = null): Boolean =
        writeReplyText(service, root, value, capturedNodes) in setOf(ReplyTextTransfer.Result.ALREADY_PRESENT,
            ReplyTextTransfer.Result.INPUT_CONNECTION, ReplyTextTransfer.Result.SET_TEXT, ReplyTextTransfer.Result.PASTE)

    fun replyTextVerified(service: AtmacaAccessibilityService, captured: List<AccessibilityNodeInfo>, value: String): Boolean {
        val snapshots = captured.map { it.toSnapshot() }
        if (ReplyComposerEvidence.contains(snapshots, value)) return true
        val node = ReplyComposerEvidence.editorIndex(snapshots)?.let(captured::get) ?: return false
        return ReplyTextTransfer.matches(ReplyInputConnection.bind(service, node)?.readFullText(), value)
    }

    internal fun writeReplyText(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, value: String,
        capturedNodes: List<AccessibilityNodeInfo>? = null, attempt: Int = 1): ReplyTextTransfer.Result {
        val nodes = capturedNodes ?: AccessibilityTree.nodes(root, maxNodes = 2_000)
        val index = ReplyComposerEvidence.editorIndex(nodes.map { it.toSnapshot() }) ?: return ReplyTextTransfer.Result.WAIT
        val node = nodes[index]
        val editor = object : ReplyTextTransfer.Editor {
            override val focused get() = node.isFocused
            override val text get() = node.text?.toString().orEmpty()
            override val connection get() = ReplyInputConnection.bind(service, node)
            override fun focus() = GestureClick.gestureTapLeading(service, node)
            override fun setText(value: String) = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) })
            override fun pasteReplacing(value: String): Boolean {
                val previous = node.text?.toString().orEmpty()
                val selected = previous.isEmpty() || ReplyComposerEvidence.isEntry(previous) ||
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, previous.length)
                    })
                if (!selected || !node.isFocused) return false
                return runCatching {
                    val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return@runCatching false
                    clipboard.setPrimaryClip(ClipData.newPlainText("Yorum", value))
                    // Some X/Compose versions handle paste without advertising the action.
                    node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                }.getOrDefault(false)
            }
        }
        return ReplyTextTransfer.write(editor, value, attempt)
    }

    internal fun writeTweetText(service: AtmacaAccessibilityService, nodes: List<AccessibilityNodeInfo>, value: String, attempt: Int): ReplyTextTransfer.Result {
        val node = tweetField(nodes) ?: return ReplyTextTransfer.Result.WAIT
        val editor = object : ReplyTextTransfer.Editor {
            override val focused get() = node.isFocused
            override val text get() = node.text?.toString().orEmpty().let { raw ->
                val plain = raw.trim().lowercase(Locale.ROOT)
                if (plain == "neler oluyor?" || plain == "neler oluyor" || plain.startsWith("what is happening") ||
                    plain.startsWith("what's happening")) "" else raw
            }
            override val connection get() = ReplyInputConnection.bind(service, node)
            override fun focus() = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS) ||
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK) || GestureClick.gestureTap(service, node)
            override fun setText(value: String) = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) })
            override fun pasteReplacing(value: String): Boolean {
                if (!node.isFocused) return false
                return runCatching {
                    val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return@runCatching false
                    clipboard.setPrimaryClip(ClipData.newPlainText("Gönderi", value))
                    node.performAction(AccessibilityNodeInfo.ACTION_PASTE) || node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                        Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) })
                }.getOrDefault(false)
            }
        }
        val result = ReplyTextTransfer.write(editor, value, attempt)
        OperationLog.i("TWEET_WRITE", "result=$result focused=${node.isFocused} editable=${node.isEditable} " +
            "id=${node.viewIdResourceName} class=${node.className} hint=${node.hintText} text=${node.text}")
        return result
    }

    fun tweetTextPresent(service: AtmacaAccessibilityService, nodes: List<AccessibilityNodeInfo>, value: String): Boolean {
        val node = tweetField(nodes) ?: return false
        if (ReplyTextTransfer.matches(node.text?.toString(), value)) return true
        return ReplyTextTransfer.matches(ReplyInputConnection.bind(service, node)?.readFullText(), value)
    }

    private fun tweetField(nodes: List<AccessibilityNodeInfo>): AccessibilityNodeInfo? {
        fun blob(node: AccessibilityNodeInfo) = listOfNotNull(node.text, node.contentDescription, node.hintText)
            .joinToString(" ").lowercase(Locale.ROOT)
        val hinted = nodes.filter { node ->
            node.isVisibleToUser && (blob(node).contains("neler oluyor") || blob(node).contains("what is happening") ||
                blob(node).contains("what's happening"))
        }
        val editable = nodes.filter { node ->
            node.isVisibleToUser && (node.isEditable || node.className?.toString()?.contains("EditText", true) == true)
        }
        return hinted.firstOrNull { it.isFocused }
            ?: editable.firstOrNull { it.isFocused }
            ?: nodes.firstOrNull { it.isFocused && it.isVisibleToUser && it.className?.toString()?.contains("Button", true) != true }
            ?: hinted.maxByOrNull { android.graphics.Rect().also(it::getBoundsInScreen).height() }
            ?: editable.firstOrNull()
    }

    fun submitReply(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?,
        capturedNodes: List<AccessibilityNodeInfo>? = null): Boolean {
        val nodes = capturedNodes ?: AccessibilityTree.nodes(root, maxNodes = 2_000)
        val index = ReplyComposerEvidence.submitIndex(nodes.map { it.toSnapshot() }) ?: return false
        val node = nodes[index]
        val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
        val wide = bounds.width() > 300
        val tall = bounds.height() > 150
        if (!wide && !tall) return GestureClick.gestureTap(service, node)
        val x = bounds.left + bounds.width() * 0.86f
        val y = bounds.top + bounds.height() * if (tall) 0.78f else 0.5f
        return GestureClick.gestureTapAt(service, x, y)
    }

    fun clickQuote(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val node = findBottomExactAction(root, XUiVocabulary.quoteActions) ?: return false
        return GestureClick.click(service, node)
    }

    fun hasQuoteAction(root: AccessibilityNodeInfo?): Boolean =
        hasIdOrLabel(root, emptyList(), XUiVocabulary.quoteActions)

    fun clickDirectFollow(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val nodes = AccessibilityTree.nodes(root)
        val snapshots = nodes.map { it.toSnapshot() }
        val profile = ProfileSurfaceEvidence.read(snapshots) ?: return false
        val statY = NavigationSurfaceEvidence.profileActionEnd(snapshots, snapshots[profile.followersIndex].bounds.top)
        val node = nodes.firstOrNull { n -> val snap = n.toSnapshot()
            snap.visible && snap.enabled && snap.bounds.bottom <= statY &&
                VerifiedFollowPolicy.isPlainFollow(listOfNotNull(snap.text, snap.contentDescription))
        } ?: return false
        return GestureClick.gestureTap(service, node)
    }

    fun isDirectFollowing(root: AccessibilityNodeInfo?): Boolean = directFollowState(root, XUiVocabulary.followingActions)
    fun directFollowAvailable(root: AccessibilityNodeInfo?): Boolean = directFollowState(root, VerifiedFollowPolicy.plainFollowLabels)

    fun clickCommentDetailFollow(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, handle: String): Boolean {
        val nodes = AccessibilityTree.nodes(root)
        val snapshots = nodes.map { it.toSnapshot() }
        val exactIndex = CommentDetailEvidence.actionIndex(snapshots, handle, VerifiedFollowPolicy.plainFollowLabels)
        val index = CommentDetailEvidence.relationshipActionIndex(snapshots, handle, VerifiedFollowPolicy.plainFollowLabels) ?: run {
            OperationLog.w("COMMENT_CLICK", "@$handle için güncel başlık/düğme eşleşmedi; tıklama yapılmadı")
            return false
        }
        if (CommentDetailEvidence.relationshipActionIndex(snapshots, handle, XUiVocabulary.followingActions + XUiVocabulary.requestedActions) != null) return false
        val n = snapshots[index]
        if (!VerifiedFollowPolicy.isPlainFollow(listOfNotNull(n.text, n.contentDescription))) return false
        val node = nodes[index]
        val native = node.isEnabled && node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        // X Compose can expose the label as a row-wide semantic node. Its centre is
        // the author/header, while the actual relationship control is on the right.
        val result = native || GestureClick.gestureTapTrailing(service, node)
        OperationLog.i("COMMENT_CLICK", "@$handle mode=${if (exactIndex != null) "exact" else "top-right"} bounds=${n.bounds} clickable=${n.clickable} accepted=$result; sonuç ayrıca doğrulanacak")
        return result
    }

    fun clickNewPost(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val metrics = service.resources.displayMetrics
        val nodes = AccessibilityTree.nodes(root, maxNodes = 1_200)
        var screenRight = metrics.widthPixels
        var screenBottom = metrics.heightPixels
        nodes.forEach { node ->
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            if (bounds.right > screenRight) screenRight = bounds.right
            if (bounds.bottom > screenBottom) screenBottom = bounds.bottom
        }
        val tabLabels = setOf(
            "ana sayfa", "anasayfa", "home", "ara", "search", "keşfet", "explore",
            "bildirimler", "notifications", "sohbet", "mesajlar", "messages", "grok",
        )
        val navTop = nodes.mapNotNull { node ->
            val label = normalizedLabel(node) ?: return@mapNotNull null
            if (label.length > 22 || label !in tabLabels) return@mapNotNull null
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            if (bounds.centerY() < screenBottom * 0.84 || bounds.height() > (96 * metrics.density)) return@mapNotNull null
            bounds.top
        }.minOrNull()
        val min = (32 * metrics.density).toInt()
        val max = (96 * metrics.density).toInt()
        val fab = nodes.mapNotNull { node ->
            if (!node.isVisibleToUser) return@mapNotNull null
            val label = normalizedLabel(node).orEmpty()
            if (tabLabels.any { label == it || label.startsWith("$it ") }) return@mapNotNull null
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            val width = bounds.width()
            val height = bounds.height()
            val square = width in min..max && height in min..max && kotlin.math.abs(width - height) <= width / 3
            if (!square || bounds.centerX() < screenRight * 0.80) return@mapNotNull null
            val aboveBar = if (navTop != null) {
                val gap = navTop - bounds.bottom
                gap in -12..(180)
            } else bounds.centerY() > screenBottom * 0.80 && bounds.centerY() < screenBottom * 0.95
            if (!aboveBar) null else node to bounds
        }.maxByOrNull { it.second.centerX() }
        if (fab == null) {
            OperationLog.i("TWEET_OPEN", "+ bulunamadı navTop=${navTop ?: -1} ekran=${screenRight}x$screenBottom")
            return false
        }
        val x = fab.second.exactCenterX()
        val y = fab.second.exactCenterY()
        val clicked = GestureClick.gestureTapAt(service, x, y)
        OperationLog.i(
            "TWEET_OPEN",
            "+ =$clicked nokta=${x.toInt()},${y.toInt()} navTop=${navTop ?: -1} ekran=${screenRight}x$screenBottom kutu=${
                fab?.second?.let { "${it.left},${it.top} ${it.width()}x${it.height()}" } ?: "koordinat"
            }",
        )
        return clicked
    }

    fun messagesOpen(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val height = serviceHeight(root)
        return AccessibilityTree.nodes(root, maxNodes = 400).any { node ->
            val label = normalizedLabel(node).orEmpty()
            val top = android.graphics.Rect().also(node::getBoundsInScreen).centerY() < height * 0.22
            (top && (label == "sohbet" || label == "messages")) || label.contains("x sohbet") || label.contains("şifre oluştur")
        }
    }

    private fun serviceHeight(root: AccessibilityNodeInfo): Int {
        val bounds = android.graphics.Rect().also(root::getBoundsInScreen)
        return bounds.height().takeIf { it > 0 } ?: 2000
    }

    fun clickComposerPost(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val metrics = service.resources.displayMetrics
        val button = AccessibilityTree.nodes(root, maxNodes = 800).mapNotNull { node ->
            if (!node.isVisibleToUser || !node.isEnabled) return@mapNotNull null
            val label = normalizedLabel(node) ?: return@mapNotNull null
            if (label != "gönderi" && label != "post") return@mapNotNull null
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            if (bounds.centerY() > metrics.heightPixels * 0.28 || bounds.centerX() < metrics.widthPixels * 0.5) return@mapNotNull null
            node to bounds.centerX()
        }.maxByOrNull { it.second }?.first ?: return false
        val clicked = GestureClick.click(service, button)
        OperationLog.i("TWEET_OPEN", "Gönderi =$clicked")
        return clicked
    }

    fun clickBottomHome(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val home = findBottomHome(service, root) ?: return false
        val bounds = android.graphics.Rect().also(home::getBoundsInScreen)
        val clicked = GestureClick.gestureTap(service, home)
        OperationLog.i("TWEET_OPEN", "alt ana sayfa =$clicked kutu=${bounds.left},${bounds.top} ${bounds.width()}x${bounds.height()}")
        return clicked
    }

    fun bottomHomeVisible(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        findBottomHome(service, root) != null

    /** Toolbar up control on a list or tweet when it has no "Geri" label. Not used on Home. */
    fun clickToolbarUp(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val window = android.graphics.Rect().also(root::getBoundsInScreen)
        if (window.width() <= 0 || window.height() <= 0) return false
        val topLimit = window.top + window.height() * 0.14f
        val leftLimit = window.left + window.width() * 0.22f
        val node = AccessibilityTree.nodes(root, maxNodes = 400).map { it to android.graphics.Rect().also(it::getBoundsInScreen) }
            .filter { (candidate, bounds) ->
                if (!candidate.isVisibleToUser) return@filter false
                val raw = listOfNotNull(candidate.text?.toString(), candidate.contentDescription?.toString())
                val blob = raw.joinToString(" ")
                if (AccountSwitcherInspector.dedicatedHandle(blob) != null) return@filter false
                if (RelationshipTabInspector.classifySelectedLabels(raw) != RelationshipTabInspector.NONE) return@filter false
                val norm = raw.map { XUiVocabulary.normalize(it) }
                val tabLabel = norm.any { label ->
                    (XUiVocabulary.verifiedFollowersHeaders + XUiVocabulary.followersHeaders + XUiVocabulary.followingHeaders).any {
                        label == it || label.startsWith("$it,") || label.startsWith("$it ") || label.startsWith("$it sekme")
                    }
                }
                if (tabLabel) return@filter false
                val clazz = candidate.className?.toString()?.lowercase(java.util.Locale.ROOT).orEmpty()
                val generic = clazz == "android.view.view"
                val control = clazz.contains("image") || clazz.contains("button") || generic
                val xLimit = if (generic) window.left + window.width() * 0.12f else leftLimit
                control && bounds.centerY() <= topLimit && bounds.centerX() <= xLimit &&
                    bounds.width() in 1..(window.width() * 0.18f).toInt() &&
                    bounds.height() in 1..(window.height() * 0.10f).toInt()
            }
            .minByOrNull { it.second.centerX() }
            ?.first ?: return false
        return GestureClick.gestureTap(service, node)
    }

    private fun findBottomHome(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val metrics = service.resources.displayMetrics
        val height = metrics.heightPixels
        val maxSide = (96 * metrics.density).toInt()
        return AccessibilityTree.nodes(root, maxNodes = 800).mapNotNull { node ->
            if (!node.isVisibleToUser || !node.isEnabled) return@mapNotNull null
            val label = normalizedLabel(node) ?: return@mapNotNull null
            val homeLabel = label == "ana sayfa" || label == "anasayfa" || label == "home" ||
                label.startsWith("ana sayfa") || label.startsWith("home ")
            if (!homeLabel) return@mapNotNull null
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            if (bounds.centerY() < height * 0.84) return@mapNotNull null
            if (bounds.height() <= 0 || bounds.height() > maxSide || bounds.width() > maxSide) return@mapNotNull null
            node to bounds
        }.minByOrNull { it.second.centerX() }?.first
    }

    fun composerOpen(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val nodes = AccessibilityTree.nodes(root, maxNodes = 600)
        val screen = android.graphics.Rect().also { root.getBoundsInScreen(it) }
        val hint = nodes.any { node ->
            val label = listOfNotNull(node.text?.toString(), node.contentDescription?.toString(), node.hintText?.toString())
                .joinToString(" ").lowercase(Locale.ROOT)
            label.contains("neler oluyor") || label.contains("what is happening") || label.contains("what's happening")
        }
        val post = nodes.any { node ->
            val label = normalizedLabel(node) ?: return@any false
            if (label != "gönderi" && label != "post") return@any false
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            bounds.centerY() < screen.height() * 0.22 && bounds.centerX() > screen.width() * 0.5
        }
        return hint || post
    }

    fun setComposerText(root: AccessibilityNodeInfo?, value: String): Boolean {
        if (root == null || value.isBlank()) return false
        val editable = AccessibilityTree.nodes(root, maxNodes = 800).firstOrNull { node ->
            val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
            node.isEditable || id.contains("tweet_box") || id.contains("composer") || id.contains("post_text")
        } ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun composerContains(root: AccessibilityNodeInfo?, value: String): Boolean {
        if (value.isBlank()) return false
        val wanted = value.trim()
        return AccessibilityTree.nodes(root, maxNodes = 800).any { node ->
            node.isEditable && node.text?.toString()?.trim() == wanted
        }
    }

    fun clickSubmit(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        clickByIdOrLabel(
            service,
            root,
            listOf("tweet_button", "post_button", "send_tweet", "reply_button"),
            XUiVocabulary.postActions,
        )

    fun clickFollowersTab(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val tab = AccessibilityTree.nodes(root, maxNodes = 900).firstOrNull { node ->
            node.isVisibleToUser && node.isEnabled &&
                RelationshipTabInspector.classifySelectedLabels(labels(node)) == RelationshipTabInspector.FOLLOWERS
        } ?: return false
        return GestureClick.click(service, tab)
    }

    fun clickVerifiedTab(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val tab = AccessibilityTree.nodes(root, maxNodes = 900).firstOrNull { node ->
            if (!node.isVisibleToUser || !node.isEnabled) return@firstOrNull false
            val raw = labels(node)
            val id = node.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
            val fullHeader = raw.any { label ->
                val value = XUiVocabulary.normalize(label)
                XUiVocabulary.fullVerifiedFollowersHeaders.any {
                    value == it || value.startsWith("$it,") || value.startsWith("$it sekme") || value.startsWith("$it tab")
                }
            }
            fullHeader || ((id.contains("tab") || node.className.toString().contains("tab", true)) &&
                RelationshipTabInspector.classifySelectedLabels(raw) == RelationshipTabInspector.VERIFIED)
        } ?: return false
        return GestureClick.click(service, tab)
    }

    fun clickProfileFollowers(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        XNavigator.clickProfileStat(service, root, followers = true)

    fun clickProfileFollowing(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        XNavigator.clickProfileStat(service, root, followers = false)

    fun clickEngagementList(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, quotes: Boolean): Boolean {
        val nodes = AccessibilityTree.nodes(root)
        val list = nodes.map { it.toSnapshot() }
        val node = nodes.firstOrNull { n -> n.isVisibleToUser && n.isEnabled &&
            listOfNotNull(n.text?.toString(), n.contentDescription?.toString()).any {
                if (EngagementListEvidence.title(list)) EngagementListEvidence.isReposts(it)
                else EngagementListEvidence.openLabel(it)
            }
        } ?: return false
        return GestureClick.gestureTap(service, node)
    }

    fun directRequested(root: AccessibilityNodeInfo?): Boolean =
        directFollowState(root, XUiVocabulary.requestedActions)

    private fun directFollowState(root: AccessibilityNodeInfo?, accepted: Set<String>): Boolean {
        val nodes = AccessibilityTree.snapshots(root)
        val profile = ProfileSurfaceEvidence.read(nodes) ?: return false
        val statY = NavigationSurfaceEvidence.profileActionEnd(nodes, nodes[profile.followersIndex].bounds.top)
        return nodes.any { n -> n.visible && n.bounds.bottom <= statY &&
            listOfNotNull(n.text, n.contentDescription).any { VerifiedFollowPolicy.matchesAction(it, accepted) } }
    }

    fun clickVisibleBack(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        clickVisibleBack(
            service,
            root,
            AutomationController.state.value.flowStage,
            AutomationController.state.value.activeScreen,
        )

    /** [flowStage] and [screen] are explicit so account handoff can still see the arrow
     *  after a discovery return has suppressed profile backs. */
    fun clickVisibleBack(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        flowStage: XFlowStage,
        screen: XScreen,
    ): Boolean {
        val nodes = AccessibilityTree.nodes(root)
        val index = NavigationSurfaceEvidence.backIndex(nodes.map { it.toSnapshot() }, flowStage, screen) ?: return false
        val node = nodes[index]
        return (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) || GestureClick.gestureTap(service, node)
    }

    private fun clickByIdOrLabel(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        idTokens: List<String>,
        labels: Set<String>,
    ): Boolean {
        val node = findByIdOrLabel(root, idTokens, labels) ?: return false
        return GestureClick.click(service, node)
    }

    private fun hasIdOrLabel(root: AccessibilityNodeInfo?, idTokens: List<String>, expectedLabels: Set<String>): Boolean =
        findByIdOrLabel(root, idTokens, expectedLabels) != null

    private fun findByIdOrLabel(
        root: AccessibilityNodeInfo?,
        idTokens: List<String>,
        labels: Set<String>,
    ): AccessibilityNodeInfo? {
        if (root == null) return null
        val normalized = labels.map(XUiVocabulary::normalize).toSet()
        return AccessibilityTree.nodes(root, maxNodes = 1_000).asSequence()
            .mapNotNull { node ->
                val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
                val label = normalizedLabel(node)
                val idScore = idTokens.count { id.contains(it.lowercase(Locale.ROOT)) } * 100
                val labelScore = if (label?.let { matchesActionLabel(it, normalized) } == true) 60 else 0
                val score = idScore + labelScore
                if (score <= 0 || !node.isEnabled) null else (clickableAncestor(node) ?: node) to score
            }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun selectedNode(root: AccessibilityNodeInfo?, idTokens: List<String>): Boolean =
        AccessibilityTree.nodes(root, maxNodes = 900).any { node ->
            val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
            idTokens.any(id::contains) && (node.isSelected || node.isChecked)
        }

    private fun labels(node: AccessibilityNodeInfo): List<String> =
        listOfNotNull(node.text?.toString(), node.contentDescription?.toString())

    private fun normalizedLabel(node: AccessibilityNodeInfo): String? = labels(node)
        .firstOrNull(String::isNotBlank)
        ?.let(XUiVocabulary::normalize)

    private fun matchesActionLabel(label: String, expected: Set<String>): Boolean =
        VerifiedFollowPolicy.matchesAction(label, expected)

    private fun findBottomExactAction(root: AccessibilityNodeInfo?, labels: Set<String>): AccessibilityNodeInfo? {
        if (root == null) return null
        val normalized = labels.map(XUiVocabulary::normalize).toSet()
        return AccessibilityTree.nodes(root, maxNodes = 1_000).asSequence()
            .mapNotNull { node ->
                val label = normalizedLabel(node) ?: return@mapNotNull null
                if (!matchesActionLabel(label, normalized) || !node.isEnabled) return@mapNotNull null
                val target = clickableAncestor(node) ?: node
                val bounds = android.graphics.Rect().also(target::getBoundsInScreen)
                target to bounds.centerY()
            }
            .maxByOrNull { it.second }
            ?.first
    }

    /**
     * X/Compose kullanıcı satırını tek bir erişilebilirlik grubu olarak
     * yayınlamazsa ilişki düğmesini aynı görsel banttaki açık @handle ile bağlar.
     * Koordinatlar sabit değildir; her event'teki gerçek node sınırlarından gelir.
     */
    private fun relaxedRelationshipTargets(
        root: AccessibilityNodeInfo?,
        accepted: Set<String>,
        excludedHandles: Set<String>,
        observation: Boolean = false,
    ): List<RelationshipTarget> {
        if (root == null) return emptyList()
        val nodes = AccessibilityTree.nodes(root, maxNodes = 1_100)
        val handleNodes = nodes.asSequence().mapNotNull { node ->
            if (!node.isVisibleToUser) return@mapNotNull null
            val handle = labels(node).asSequence().mapNotNull(AccountSwitcherInspector::dedicatedHandle).firstOrNull()
                ?: return@mapNotNull null
            val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
            if (bounds.isEmpty) null else Triple(handle, node, bounds)
        }.toList()

        return nodes.asSequence().mapNotNull { node ->
            val label = labels(node).map(XUiVocabulary::normalize).firstOrNull { matchesActionLabel(it, accepted) }
                ?: return@mapNotNull null
            if (!isRelationshipActionNode(node, observation)) return@mapNotNull null
            if (accepted == VerifiedFollowPolicy.availableFollowLabels &&
                !VerifiedFollowPolicy.isAvailableFollow(labels(node))) return@mapNotNull null
            if (accepted == VerifiedFollowPolicy.plainFollowLabels && !VerifiedFollowPolicy.isPlainFollow(labels(node))) return@mapNotNull null
            val rowAnchor = clickableAncestor(node) ?: node
            if (accepted == VerifiedFollowPolicy.availableFollowLabels &&
                !VerifiedFollowPolicy.isAvailableFollow(labels(node) + labels(clickableAncestor(node) ?: node))) return@mapNotNull null
            if (accepted == VerifiedFollowPolicy.plainFollowLabels &&
                !VerifiedFollowPolicy.isPlainFollow(labels(node) + labels(rowAnchor))) return@mapNotNull null
            val actionBounds = android.graphics.Rect().also(node::getBoundsInScreen)
            if (actionBounds.isEmpty) return@mapNotNull null
            val directHandle = (labels(node) + labels(rowAnchor)).asSequence()
                .mapNotNull(XIdentityDetector::extractHandle)
                .firstOrNull()
            val handle = directHandle ?: handleNodes.asSequence()
                .filter { (_, _, bounds) -> sameVisualRow(bounds, actionBounds) }
                .minByOrNull { (_, _, bounds) -> kotlin.math.abs(bounds.centerY() - actionBounds.centerY()) }
                ?.first
                ?: return@mapNotNull null
            if (handle in excludedHandles) return@mapNotNull null
            RelationshipTarget(handle, node, label)
        }.distinctBy { it.handle }.toList()
    }

    private fun sameVisualRow(first: android.graphics.Rect, second: android.graphics.Rect): Boolean {
        return RelationshipRowGeometry.matches(
            first.left, first.top, first.right, first.bottom,
            second.left, second.top, second.right, second.bottom,
        )
    }

    private fun clickableAncestor(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var node: AccessibilityNodeInfo? = start
        for (index in 0 until 6) {
            val current = node ?: break
            if (current.isEnabled && current.isClickable) return current
            node = current.parent
        }
        return null
    }

    private fun userRow(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var node: AccessibilityNodeInfo? = start
        for (index in 0 until 7) {
            val current = node ?: break
            val descendants = AccessibilityTree.nodes(current, maxNodes = 100)
            if (descendants.size < 100 && rowHandles(current).size == 1) return current
            node = current.parent
        }
        return null
    }

    private fun rowHandles(row: AccessibilityNodeInfo): Set<String> =
        AccessibilityTree.nodes(row, maxNodes = 100)
            .flatMap(::labels)
            .mapNotNull(XIdentityDetector::extractHandle)
            .toSet()
}
