package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo

/** X/Twitter screen detector using independent accessibility labels rather than fixed pixels. */
object ScreenDetector {
    fun detect(root: AccessibilityNodeInfo?): XScreen = detect(root, AccessibilityTree.snapshots(root))

    fun detect(root: AccessibilityNodeInfo?, nodes: List<NodeSnapshot>): XScreen {
        val selectedTab = if (PopupClassifier.classify(nodes) == PopupType.NONE)
            RelationshipTabInspector.selectedTab(root) else RelationshipTabInspector.NONE
        return detect(nodes, selectedTab)
    }

    internal fun detect(nodes: List<NodeSnapshot>): XScreen = detect(nodes, RelationshipTabInspector.NONE)

    internal fun detect(nodes: List<NodeSnapshot>, selectedRelationshipTab: Int): XScreen {
        if (nodes.isEmpty()) return XScreen.UNKNOWN
        val labels = nodes.flatMap { listOfNotNull(it.text, it.contentDescription) }
            .map(XUiVocabulary::normalize)
            .filter(String::isNotBlank)
        val corpus = labels.joinToString(" ")

        if (looksLikeDialog(nodes, corpus)) return XScreen.DIALOG

        val hasEditable = nodes.any { node ->
            node.editable || node.className?.contains("EditText", ignoreCase = true) == true
        }
        val hasComposerId = nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            id.contains("tweet_box") || id.contains("composer") || id.contains("post_text")
        }
        val hasSubmitId = nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            id.contains("tweet_button") || id.contains("post_button") || id.contains("reply_button")
        }
        val composerByText = labels.any { label -> XUiVocabulary.composerSignals.any { token -> label.contains(token) } }
        // A tweet detail keeps an inline editable "Yanıtını gönder" field on screen.
        // That field is not the full-screen composer: the detail title/back button and
        // the post action toolbar remain visible. 26.19 classified this real device
        // layout as COMPOSER and therefore waited forever instead of reading replies.
        val inlineReplyEntry = labels.any { it == "yanıtını gönder" || it == "post your reply" || it == "yanıt gönder" }
        val detailTitleAtTop = labels.any { it in XUiVocabulary.tweetDetailSignals }
        val detailBack = labels.any { it in XUiVocabulary.backSignals } || nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            id.endsWith("/back") || id.contains("toolbar_back") || id.contains("navigate_up")
        }
        val detailToolbar = nodes.count { node ->
            val id = node.viewId.orEmpty().lowercase()
            id.contains("toolbar_reply") || id.contains("toolbar_like") || id.contains("toolbar_retweet") || id.contains("bookmark")
        } >= 2
        val inlineTweetDetail = inlineReplyEntry && detailBack && (detailTitleAtTop || detailToolbar || labels.any(EngagementListEvidence::openLabel))
        if (inlineTweetDetail) return XScreen.TWEET_DETAIL
        if ((hasEditable && (hasComposerId || hasSubmitId || composerByText)) || (hasComposerId && composerByText)) {
            return XScreen.COMPOSER
        }

        val explicitHandles = labels.mapNotNull(XIdentityDetector::extractHandle).distinct()
        val handleCount = explicitHandles.size
        val scrollable = nodes.any { it.scrollable }
        val clickableCount = nodes.count { it.clickable }

        val switcherStrong = labels.any { it in XUiVocabulary.accountSwitcherSignals }
        val switcherTitle = labels.any { it in XUiVocabulary.accountSwitcherLabels }
        if ((switcherStrong && handleCount >= 1) || (switcherTitle && handleCount >= 2)) {
            return XScreen.ACCOUNT_SWITCHER
        }

        val drawerSignalCount = XUiVocabulary.drawerSignals.count { token ->
            labels.any { it == token || it.contains(token) }
        }
        if (drawerSignalCount >= 3 && clickableCount >= 3) return XScreen.ACCOUNT_DRAWER

        // The HOME pager also has a Following tab and tweet author handles. Resolve
        // its own tab strip before either raw-node or snapshot relationship heuristics.
        // Account overlays above must still win when the feed remains behind them.
        if (HomeTimelineEvidence.matches(nodes)) return XScreen.HOME
        if (EngagementListEvidence.title(nodes)) return XScreen.ENGAGEMENT_LIST
        // A scrolled profile retains Posts, even when recommendation cards contain Following.
        if (FeedRowEvidence.profileFeed(nodes)) return XScreen.PROFILE
        if (labels.any { it in setOf("yanıtını gönder", "post your reply", "yanıt gönder") } &&
            (labels.any(EngagementListEvidence::openLabel) || FeedRowEvidence.rows(nodes).isNotEmpty())) return XScreen.TWEET_DETAIL

        when (selectedRelationshipTab) {
            RelationshipTabInspector.OTHER -> return XScreen.UNKNOWN
            RelationshipTabInspector.VERIFIED -> return XScreen.VERIFIED_FOLLOWERS_LIST
            RelationshipTabInspector.FOLLOWING -> return XScreen.FOLLOWING_LIST
            RelationshipTabInspector.FOLLOWERS -> return XScreen.FOLLOWERS_LIST
        }

        val selectedSnapshotTab = nodes.firstNotNullOfOrNull { node ->
            val selected = node.visible && (node.selected || node.checked ||
                node.contentDescription.orEmpty().let { it.contains("selected", true) || it.contains("seçili", true) })
            if (!selected) null else RelationshipTabInspector.classifySelectedLabels(listOfNotNull(node.text, node.contentDescription))
                .takeIf { it != RelationshipTabInspector.NONE }
        }
        // Tab identity is independent of row loading and relationship buttons.
        // Apply the same rule to snapshots as to the raw accessibility tree.
        when (selectedSnapshotTab) {
            RelationshipTabInspector.OTHER -> return XScreen.UNKNOWN
            RelationshipTabInspector.VERIFIED -> return XScreen.VERIFIED_FOLLOWERS_LIST
            RelationshipTabInspector.FOLLOWERS -> return XScreen.FOLLOWERS_LIST
            RelationshipTabInspector.FOLLOWING -> return XScreen.FOLLOWING_LIST
        }

        // The selected tab wins over labels of neighboring, unselected tabs.
        if (nodes.any { isSelectedRelationshipTab(it, XUiVocabulary.followingHeaders) } && listEvidence(labels, handleCount, scrollable)) return XScreen.FOLLOWING_LIST
        if (nodes.any { isSelectedRelationshipTab(it, XUiVocabulary.followersHeaders) } && listEvidence(labels, handleCount, scrollable)) return XScreen.FOLLOWERS_LIST


        // X aynı üst sekme şeridinde Followers, Following, Subscribers ve
        // Subscriptions etiketlerini birlikte tutuyor. Ekran türünü yalnızca etiketin
        // varlığıyla seçmek yanlış sekmeyi başarı sayar. Önce Android'in seçili/checked
        // sekme kanıtını veya erişilebilirlik açıklamasındaki selected/seçili durumunu oku.
        val followingHeader = labels.any { label ->
            XUiVocabulary.followingHeaders.any { token -> containsToken(label, token) }
        }
        val followersHeader = labels.any { label ->
            XUiVocabulary.followersHeaders.any { token -> containsToken(label, token) }
        }
        val selectedFollowingTab = nodes.any { node ->
            isSelectedRelationshipTab(node, XUiVocabulary.followingHeaders)
        }
        val selectedFollowersTab = nodes.any { node ->
            isSelectedRelationshipTab(node, XUiVocabulary.followersHeaders)
        }
        if (selectedFollowingTab && listEvidence(labels, handleCount, scrollable)) return XScreen.FOLLOWING_LIST
        if (selectedFollowersTab && listEvidence(labels, handleCount, scrollable)) return XScreen.FOLLOWERS_LIST

        // Profile must win over list heuristics: profile pages can themselves be scrollable and contain
        // "Following" / "Followers" stats, which CP9 could mistake for a list.
        val ownProfile = labels.any { it in XUiVocabulary.ownProfileSignals }
        val hasHandle = handleCount >= 1
        // Following/Followers listelerindeki düz sekme başlıklarını profil sayacı
        // sanma. Profil kanıtı için aynı erişilebilirlik düğümünde sayı + başlık veya
        // açık bir count view-id gerekir.
        val hasFollowersStat = hasNumberedProfileStat(nodes, XUiVocabulary.followersHeaders, followers = true)
        val hasFollowingStat = hasNumberedProfileStat(nodes, XUiVocabulary.followingHeaders, followers = false)
        val hasJoined = XUiVocabulary.joinedSignals.any(corpus::contains)
        if (ProfileSurfaceEvidence.read(nodes) != null || ownProfile || (hasHandle && hasFollowersStat && hasFollowingStat && (hasJoined || !scrollable || clickableCount >= 2))) {
            return XScreen.PROFILE
        }

        // Eski X varyantlarında sekme şeridi yalnızca açık sekmenin etiketini
        // yayınlayabilir. İki başlık birden görünüyorsa seçili durum kanıtı olmadan
        // Following/Followers tahmini yapma.
        if (followingHeader && !followersHeader && listEvidence(labels, handleCount, scrollable)) return XScreen.FOLLOWING_LIST
        if (followersHeader && !followingHeader && listEvidence(labels, handleCount, scrollable)) return XScreen.FOLLOWERS_LIST

        val engagementHeader = labels.any { label -> XUiVocabulary.engagementSignals.any { token -> label == token || label.contains(token) } }
        if (engagementHeader && listEvidence(labels, handleCount, scrollable)) return XScreen.ENGAGEMENT_LIST

        // Home timelines contain many reply/like/repost/bookmark controls. Those controls must
        // never be sufficient to classify a feed as a single tweet detail screen.
        val homeSignalCount = XUiVocabulary.homeSignals.count { token -> labels.any { it == token } }
        val hasHomeNavigation = labels.any { it == "anasayfa" || it == "home" } &&
            labels.any { it == "ara" || it == "search" } &&
            labels.any { it == "bildirimler" || it == "notifications" }
        val hasHomeId = nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            id.contains("bottom_navigation") || id.contains("home_timeline") ||
                id.endsWith("/home") || id.contains("navigation_home")
        }
        if (hasHomeId || hasHomeNavigation || homeSignalCount >= 3) return XScreen.HOME

        val tweetActionCount = listOf(
            XUiVocabulary.replyActions,
            XUiVocabulary.likeActions,
            XUiVocabulary.unlikeActions,
            XUiVocabulary.repostActions,
            XUiVocabulary.undoRepostActions,
            XUiVocabulary.bookmarkActions,
            XUiVocabulary.removeBookmarkActions,
        ).sumOf { vocabulary -> labels.count { it in vocabulary } }
        val tweetIds = nodes.count { node ->
            val id = node.viewId.orEmpty().lowercase()
            id.contains("tweet") || id.contains("status") || id.contains("toolbar_like") || id.contains("toolbar_retweet")
        }
        val explicitDetailId = nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            id.contains("tweet_detail") || id.contains("status_detail") ||
                id.contains("tweet_permalink") || id.contains("detail_header")
        }
        val hasBack = labels.any { it in XUiVocabulary.backSignals } || nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            id.endsWith("/back") || id.contains("toolbar_back") || id.contains("navigate_up")
        }
        val hasDetailTitle = labels.any { it in XUiVocabulary.tweetDetailSignals }
        val detailReplyEntry = labels.any { it == "yanıtını gönder" || it == "post your reply" || it == "yanıt gönder" }
        val numberedActions = labels.count { label ->
            label.length < 100 && label.any(Char::isDigit) &&
                listOf("yanıt", "replies", "beğeni", "likes", "yeniden gönder", "reposts").any(label::contains)
        }
        val scrolledDetail = detailReplyEntry && labels.any(EngagementListEvidence::openLabel)
        if (explicitDetailId || scrolledDetail || (hasBack && hasDetailTitle &&
                ((tweetActionCount >= 2 && tweetIds >= 1) || detailReplyEntry || numberedActions >= 2))) {
            return XScreen.TWEET_DETAIL
        }

        return XScreen.UNKNOWN
    }

    private fun listEvidence(labels: List<String>, handleCount: Int, scrollable: Boolean): Boolean {
        if (handleCount >= 2) return true
        val actionCount = labels.count { it in XUiVocabulary.followActions || it in XUiVocabulary.followingActions }
        return handleCount >= 1 && (scrollable || actionCount >= 1)
    }

    private fun hasNumberedProfileStat(
        nodes: List<NodeSnapshot>,
        headers: Set<String>,
        followers: Boolean,
    ): Boolean {
        val strongIdTokens = if (followers) {
            listOf("followers_count", "follower_count")
        } else {
            listOf("following_count")
        }
        return nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            val nodeLabels = listOfNotNull(node.text, node.contentDescription).map(XUiVocabulary::normalize)
            strongIdTokens.any(id::contains) || nodeLabels.any { label ->
                label.any { character -> character.isDigit() } && headers.any { token -> containsToken(label, token) }
            }
        }
    }

    private fun isSelectedRelationshipTab(node: NodeSnapshot, headers: Set<String>): Boolean {
        val nodeLabels = listOfNotNull(node.text, node.contentDescription)
            .map(XUiVocabulary::normalize)
            .filter(String::isNotBlank)
        val labelMatches = nodeLabels.any { label ->
            headers.any { token -> containsToken(label, token) || label.contains(token) }
        }
        if (!labelMatches) return false
        val id = node.viewId.orEmpty().lowercase()
        val stateInLabel = nodeLabels.any { label ->
            listOf("selected", "seçili", "active", "aktif").any(label::contains)
        }
        return node.selected || node.checked || stateInLabel || id.contains("selected") || id.contains("active")
    }

    private fun looksLikeDialog(nodes: List<NodeSnapshot>, corpus: String): Boolean {
        val dismiss = setOf("iptal", "cancel", "tamam", "ok", "vazgeç", "tekrar dene", "try again", "şimdi değil", "not now", "kapat", "close")
        val control = nodes.any { node ->
            node.visible && node.enabled && (node.clickable || node.className.orEmpty().contains("button", true)) &&
                listOfNotNull(node.text, node.contentDescription).any { XUiVocabulary.normalize(it) in dismiss }
        }
        val container = nodes.any {
            it.visible && (it.className.orEmpty().contains("dialog", true) ||
                it.viewId.orEmpty().substringAfterLast('/') in setOf("alertTitle", "parentPanel", "dialog_title", "dialog_message"))
        }
        return control || container
    }

    private fun containsToken(label: String, token: String): Boolean =
        label == token || label.startsWith("$token ") || label.endsWith(" $token") || label.contains(" $token ")
}
