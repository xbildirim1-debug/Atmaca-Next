package com.atmacanext.app.automation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/** X/Twitter navigation: semantic node first; gesture is only a fallback on the selected node bounds. */
object XNavigator {
    fun execute(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        command: NavigationCommand,
        targetUsername: String,
        detectedUsername: String?,
    ): Boolean = when (command) {
        NavigationCommand.OPEN_X_HOME -> service.launchXHome()
        NavigationCommand.OPEN_ACCOUNT_DRAWER -> clickAccountMenu(service, root)
        NavigationCommand.OPEN_PROFILE_FROM_DRAWER ->
            clickExactLabel(service, root, XUiVocabulary.profileEntryLabels) || clickFirstDedicatedHandle(service, root)
        NavigationCommand.OPEN_ACCOUNT_SWITCHER -> clickAccountSheetTrigger(service, root)
        NavigationCommand.SELECT_TARGET_ACCOUNT -> clickExactHandle(service, root, targetUsername)
        NavigationCommand.OPEN_FOLLOWERS -> clickProfileStat(service, root, followers = true) || service.launchXFollowers(targetUsername)
        NavigationCommand.OPEN_FOLLOWING -> clickProfileStat(service, root, followers = false) || service.launchXFollowing(targetUsername)
        NavigationCommand.OPEN_VERIFIED_FOLLOWERS -> clickExactLabel(service, root, XUiVocabulary.verifiedFollowersHeaders)
        NavigationCommand.BACK -> service.pressBack()
        NavigationCommand.WAIT_FOR_UI, NavigationCommand.READY -> false
    }

    fun clickExactHandle(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, username: String): Boolean {
        val node = findExactHandleNode(root, username) ?: return false
        return GestureClick.click(service, node)
    }

    /** Test/legacy-compatible ACTION_CLICK-only overload. */
    fun clickExactHandle(root: AccessibilityNodeInfo?, username: String): Boolean {
        val node = findExactHandleNode(root, username) ?: return false
        val clickable = safeRowClickableAncestor(node) ?: node
        return clickable.isEnabled && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findExactHandleNode(root: AccessibilityNodeInfo?, username: String): AccessibilityNodeInfo? {
        val wanted = XIdentityDetector.normalizeUsername(username)
        if (root == null || wanted.isBlank()) return null
        return AccessibilityTree.nodes(root, maxNodes = 800).firstOrNull { node ->
            val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
            node.isVisibleToUser && listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).any { label ->
                val t = label.trim()
                AccountSwitcherInspector.dedicatedHandle(t) == wanted ||
                    (XIdentityDetector.isStrongUsernameId(id) && XIdentityDetector.extractBareHandleCandidate(t) == wanted)
            }
        }?.let { safeRowClickableAncestor(it) ?: it }
    }

    fun clickProfileStat(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, followers: Boolean): Boolean {
        val target = findProfileStat(root, followers) ?: return false
        return GestureClick.click(service, target)
    }

    fun clickProfileStat(root: AccessibilityNodeInfo?, followers: Boolean): Boolean {
        val target = findProfileStat(root, followers) ?: return false
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findProfileStat(root: AccessibilityNodeInfo?, followers: Boolean): AccessibilityNodeInfo? {
        if (root == null) return null
        val labels = if (followers) XUiVocabulary.followersHeaders else XUiVocabulary.followingHeaders
        val idTokens = if (followers) listOf("followers", "follower_count") else listOf("following", "following_count")
        val candidates = AccessibilityTree.nodes(root, maxNodes = 650).asSequence().mapNotNull { node ->
            val clickable = safeClickableAncestor(node) ?: node
            val subtree = AccessibilityTree.nodes(clickable, maxNodes = 40)
            if (subtree.size >= 40) return@mapNotNull null
            val subtreeLabels = subtree.flatMap { child -> listOfNotNull(child.text?.toString(), child.contentDescription?.toString()) }.map(XUiVocabulary::normalize)
            val corpus = subtreeLabels.joinToString(" ")
            val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
            val labelScore = labels.count { token -> containsWholeLabel(corpus, token) } * 100
            val idScore = idTokens.count(id::contains) * 160
            val numberScore = if (Regex("\\d").containsMatchIn(corpus)) 40 else 0
            val actionOnly = subtreeLabels.size <= 2 && subtreeLabels.any { it in XUiVocabulary.followActions || it in XUiVocabulary.followingActions }
            val hasForbidden = subtreeLabels.any { label -> XUiVocabulary.forbiddenProfilePhrases.any(label::contains) }
            if (actionOnly || hasForbidden || (labelScore == 0 && idScore == 0) || (idScore == 0 && numberScore == 0)) return@mapNotNull null
            clickable to (labelScore + idScore + numberScore)
        }.distinctBy { System.identityHashCode(it.first) }.sortedByDescending { it.second }.toList()
        return candidates.firstOrNull()?.first
    }

    fun clickAccountMenu(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        if (root == null) { OperationLog.w("NAV", "drawer click: root null"); return false }
        val labeled = NodeSelector.best(root, NodeSelector.Query(
            // Generic profile images also belong to tweet authors; only dedicated menu
            // semantics may bypass the toolbar-bounds check in clickTopLeftAvatar.
            viewIdContains = listOf("navigation_drawer", "drawer_menu", "home_drawer"),
            contentDescriptions = XUiVocabulary.accountMenuLabels - setOf("profil fotoğrafı", "profile photo", "profile picture"),
            exactTexts = XUiVocabulary.accountMenuLabels - setOf("profil fotoğrafı", "profile photo", "profile picture"),
            requireClickable = false,
        ))?.node
        if (labeled != null && !TargetVerifier.containsForbiddenProfilePhrase(labeled) && GestureClick.click(service, labeled)) {
            OperationLog.i("NAV", "drawer labeled tiklandi")
            return true
        }

        val tokens = listOf("gezinti çekmecesini göster", "gezinti çekmecesi", "çekmece", "gezinti", "navigation drawer", "navigation menu", "show navigation", "open navigation")
        val byToken = AccessibilityTree.nodes(root, maxNodes = 800).firstOrNull { node ->
            val blob = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" ").lowercase(Locale.ROOT)
            tokens.any(blob::contains) && !XUiVocabulary.forbiddenProfilePhrases.any(blob::contains)
        }
        if (byToken != null && GestureClick.click(service, byToken)) {
            OperationLog.i("NAV", "drawer token tiklandi desc=${byToken.contentDescription} text=${byToken.text}")
            return true
        }
        if (clickTopLeftAvatar(service, root)) { OperationLog.i("NAV", "drawer topleft avatar"); return true }
        OperationLog.w("NAV", "drawer click basarisiz")
        return false
    }

    private fun clickTopLeftAvatar(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo): Boolean {
        if (ScreenDetector.detect(root) != XScreen.HOME) return false
        val rootBounds = Rect().also(root::getBoundsInScreen)
        if (rootBounds.width() <= 0 || rootBounds.height() <= 0) return false
        val topLimit = rootBounds.top + rootBounds.height() * 0.16f
        val leftLimit = rootBounds.left + rootBounds.width() * 0.22f
        val candidates = AccessibilityTree.nodes(root, maxNodes = 600).asSequence()
            .map { it to Rect().also(it::getBoundsInScreen) }
            .filter { (node,b) ->
                val clazz = node.className?.toString()?.lowercase(Locale.ROOT).orEmpty()
                val blob = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
                    .joinToString(" ").lowercase(Locale.ROOT)
                val avatarLike = clazz.contains("image") || clazz.contains("button") ||
                    listOf("avatar", "profil", "profile", "hesap", "account").any(blob::contains)
                b.width() > 0 && b.height() > 0 && b.centerY() <= topLimit && b.centerX() <= leftLimit &&
                    b.width() <= rootBounds.width()*0.22f && b.height() <= rootBounds.height()*0.13f &&
                    node.isVisibleToUser && node.isEnabled && avatarLike && !XUiVocabulary.forbiddenProfilePhrases.any(blob::contains)
            }
            .sortedWith(compareBy<Pair<AccessibilityNodeInfo,Rect>> { it.second.centerX() }.thenBy { it.second.centerY() }).toList()
        for ((node,_) in candidates) if (GestureClick.click(service,node)) return true
        // X can expose the avatar as a non-semantic canvas node. A ratio-based fallback is safe
        // only after HOME is independently verified; the next snapshot must still prove DRAWER.
        return GestureClick.tapAtRatio(service, root, xRatio = 0.075f, yRatio = 0.065f)
    }

    private fun clickAccountSheetTrigger(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val labeled = NodeSelector.best(root, NodeSelector.Query(
            viewIdContains = listOf("switch_account", "account_switch", "account_switcher", "accounts"),
            contentDescriptions = XUiVocabulary.accountSwitcherLabels,
            exactTexts = XUiVocabulary.accountSwitcherLabels,
            requireClickable = false,
        ))?.node
        if (labeled != null && GestureClick.click(service,labeled)) {
            OperationLog.i("NAV", "account switcher semantik tetikleyici tıklandı")
            return true
        }
        if (ScreenDetector.detect(root) != XScreen.ACCOUNT_DRAWER) return false
        val rootBounds=Rect().also(root::getBoundsInScreen)
        if (rootBounds.width()<=0 || rootBounds.height()<=0) return false
        val candidates=AccessibilityTree.nodes(root,maxNodes=500).asSequence().map { it to Rect().also(it::getBoundsInScreen) }
            .filter { (node,b) ->
                val blob=listOfNotNull(node.text?.toString(),node.contentDescription?.toString()).joinToString(" ").trim()
                val clazz=node.className?.toString()?.lowercase(Locale.ROOT).orEmpty()
                b.centerY()<=rootBounds.top+rootBounds.height()*0.20f && b.centerX()>=rootBounds.left+rootBounds.width()*0.60f &&
                    b.width()<=rootBounds.width()*0.25f && b.height()<=rootBounds.height()*0.15f && node.isEnabled &&
                    (blob.isBlank() || XUiVocabulary.accountSwitcherLabels.any { blob.contains(it,true) }) &&
                    (clazz.contains("image") || clazz.contains("button") || clazz.contains("view"))
            }.sortedByDescending { it.second.centerX() }.toList()
        // Do not guess if many unrelated top-right controls exist. Gesture fallback is allowed only
        // on an independently verified account drawer and the caller validates the next screen.
        for ((node,_) in candidates.take(2)) {
            if (GestureClick.click(service,node)) {
                OperationLog.i("NAV", "account switcher doğrulanmış avatar kümesiyle açıldı")
                return true
            }
        }
        // Konum yedeği yalnızca bağımsız olarak ACCOUNT_DRAWER olduğu kanıtlanan
        // ekranda kullanılır; çağıran aşama bir sonraki snapshot'ta ACCOUNT_SWITCHER
        // kanıtı arar ve başka bir yüzeyi başarı saymaz.
        if (GestureClick.tapAtRatio(service, root, xRatio = 0.795f, yRatio = 0.10f)) {
            OperationLog.i("NAV", "account switcher uyarlanabilir STEP-2 bölgesiyle açıldı")
            return true
        }
        OperationLog.w("NAV", "account switcher açılamadı: semantik, avatar kümesi ve STEP-2 başarısız")
        return false
    }

    private fun clickExactLabel(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, labels: Set<String>): Boolean {
        val match=NodeSelector.best(root,NodeSelector.Query(exactTexts=labels,contentDescriptions=labels,requireClickable=false))?.node ?: return false
        if (TargetVerifier.containsForbiddenProfilePhrase(match)) return false
        return GestureClick.click(service,match)
    }

    private fun clickFirstDedicatedHandle(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        if (root==null) return false
        val node=AccessibilityTree.nodes(root,maxNodes=450).firstOrNull { n ->
            listOfNotNull(n.text?.toString(),n.contentDescription?.toString()).any { AccountSwitcherInspector.dedicatedHandle(it)!=null }
        } ?: return false
        return GestureClick.click(service,node)
    }

    private fun safeRowClickableAncestor(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo?=start
        repeat(6) {
            val node=current ?: return@repeat
            if (TargetVerifier.isSafeClickable(node)) {
                val descendants=AccessibilityTree.nodes(node,maxNodes=80)
                val handles=descendants.flatMap { child -> listOfNotNull(child.text?.toString(),child.contentDescription?.toString()) }.mapNotNull(XIdentityDetector::extractHandle).distinct()
                if (descendants.size<80 && handles.size==1) return node
            }
            current=node.parent
        }
        return null
    }
    private fun safeClickableAncestor(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo?=start
        repeat(5) { val node=current ?: return@repeat; if (node.isEnabled && node.isClickable && !TargetVerifier.containsForbiddenProfilePhrase(node)) return node; current=node.parent }
        return null
    }
    private fun containsWholeLabel(corpus:String,label:String)=Regex("(^|\\s)${Regex.escape(label)}($|\\s)").containsMatchIn(corpus)
}
