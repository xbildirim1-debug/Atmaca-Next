package com.atmacanext.app.automation

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

    fun clickAccountSwitcher(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        return XNavigator.execute(
            service = service,
            root = root,
            command = NavigationCommand.OPEN_ACCOUNT_SWITCHER,
            targetUsername = "",
            detectedUsername = null,
        )
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
            if (accepted == VerifiedFollowPolicy.plainFollowLabels && !VerifiedFollowPolicy.isPlainFollow(labels(node))) return@mapNotNull null
            val label = normalizedLabel(node) ?: return@mapNotNull null
            if (!matchesActionLabel(label, accepted)) return@mapNotNull null
            val button = clickableAncestor(node) ?: node.takeIf { it.isEnabled } ?: return@mapNotNull null
            if (accepted == VerifiedFollowPolicy.plainFollowLabels &&
                !VerifiedFollowPolicy.isPlainFollow(labels(node) + labels(button))) return@mapNotNull null
            val row = userRow(button) ?: return@mapNotNull null
            val handles = rowHandles(row)
            if (handles.size != 1) return@mapNotNull null
            val handle = handles.single()
            if (handle in excludedHandles) return@mapNotNull null
            val bounds = android.graphics.Rect().also(row::getBoundsInScreen)
            Triple(bounds.top, handle, RelationshipTarget(handle, button, label))
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
        GestureClick.click(service, target.button)

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
                val actions = descendants.filter(::isRelationshipActionNode)
                if (actions.isNotEmpty()) {
                    return actions.any { action ->
                        if (normalized == VerifiedFollowPolicy.plainFollowLabels) VerifiedFollowPolicy.isPlainFollow(labels(action) + (clickableAncestor(action)?.let(::labels) ?: emptyList()))
                        else labels(action).any { matchesActionLabel(XUiVocabulary.normalize(it), normalized) }
                    }
                }
                row = current.parent
            }
        }
        return relaxedRelationshipTargets(root, normalized, emptySet()).any { it.handle == wanted }
    }

    private fun isRelationshipActionNode(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser || !node.isEnabled) return false
        val id = node.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
        val clazz = node.className?.toString().orEmpty().lowercase(Locale.ROOT)
        if (id.contains("tab") || clazz.contains("tab") || labels(node).any {
            val label = XUiVocabulary.normalize(it)
            label.contains("sekme") || label.contains(" tab")
        }) return false
        val relationLabels = XUiVocabulary.followActions + XUiVocabulary.followingActions
        return labels(node).any { matchesActionLabel(XUiVocabulary.normalize(it), relationLabels) } &&
            (node.isClickable || clazz.contains("button") || id.contains("follow") || node.parent?.isClickable == true)
    }

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

    fun clickQuote(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val node = findBottomExactAction(root, XUiVocabulary.quoteActions) ?: return false
        return GestureClick.click(service, node)
    }

    fun hasQuoteAction(root: AccessibilityNodeInfo?): Boolean =
        hasIdOrLabel(root, emptyList(), XUiVocabulary.quoteActions)

    fun clickDirectFollow(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val node = findByIdOrLabel(root, listOf("follow_button", "profile_follow"), setOf("takip et", "follow")) ?: return false
        return GestureClick.click(service, node)
    }

    fun isDirectFollowing(root: AccessibilityNodeInfo?): Boolean =
        hasIdOrLabel(root, listOf("following_button", "profile_following"), XUiVocabulary.followingActions)

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
                setOf("verified followers", "onaylı takipçiler", "doğrulanmış takipçiler").any {
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
        val labels = if (quotes) setOf("alıntılar", "alıntı", "quotes", "quote posts") else setOf("retweetler", "retweet", "reposts", "repost")
        val ids = if (quotes) listOf("quote", "quotes_count") else listOf("retweet_count", "repost_count")
        return clickByIdOrLabel(service, root, ids, labels)
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
            val label = normalizedLabel(node) ?: return@mapNotNull null
            if (!isRelationshipActionNode(node) || !matchesActionLabel(label, accepted)) return@mapNotNull null
            if (accepted == VerifiedFollowPolicy.plainFollowLabels && !VerifiedFollowPolicy.isPlainFollow(labels(node))) return@mapNotNull null
            val button = clickableAncestor(node) ?: node
            if (accepted == VerifiedFollowPolicy.plainFollowLabels &&
                !VerifiedFollowPolicy.isPlainFollow(labels(node) + labels(button))) return@mapNotNull null
            val buttonBounds = android.graphics.Rect().also(button::getBoundsInScreen)
            if (buttonBounds.isEmpty) return@mapNotNull null
            val directHandle = (labels(node) + labels(button)).asSequence()
                .mapNotNull(XIdentityDetector::extractHandle)
                .firstOrNull()
            val handle = directHandle ?: handleNodes.asSequence()
                .filter { (_, _, bounds) -> sameVisualRow(bounds, buttonBounds) }
                .minByOrNull { (_, _, bounds) -> kotlin.math.abs(bounds.centerY() - buttonBounds.centerY()) }
                ?.first
                ?: return@mapNotNull null
            if (handle in excludedHandles) return@mapNotNull null
            RelationshipTarget(handle, button, label)
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
