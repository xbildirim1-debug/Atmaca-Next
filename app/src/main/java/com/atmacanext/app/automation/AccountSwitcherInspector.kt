package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/** X hesap çekmecesi ve hesap seçicisini yalnız erişilebilirlik semantiğiyle okur. */
object AccountSwitcherInspector {
    data class ProfileStats(val followers: String? = null, val following: String? = null)

    data class DrawerAccountSnapshot(
        val username: String? = null,
        val displayName: String? = null,
        val followers: String? = null,
        val following: String? = null,
    ) {
        val statsComplete: Boolean
            get() = !followers.isNullOrBlank() && !following.isNullOrBlank()
    }

    private val drawerFollowingLabels = setOf("takip ediyor", "takip edilen", "following")
    private val drawerFollowerLabels = setOf("takipçiler", "takipçi", "followers", "follower")
    fun dedicatedHandle(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        if (text.length > 20) return null
        val extracted = XIdentityDetector.extractHandle(text) ?: return null
        return extracted.takeIf { text.equals("@$it", true) || text.equals(it, true) }
    }

    fun visibleAccountHandles(root: AccessibilityNodeInfo?): List<String> =
        switcherHandleNodes(root)
            .asSequence()
            .flatMap(::nodeLabels)
            .mapNotNull(::dedicatedHandle)
            .distinct()
            .take(10)
            .toList()

    fun isSelectedAccount(root: AccessibilityNodeInfo?, username: String): Boolean =
        isHandleSelected(root, username)

    fun isHandleSelected(root: AccessibilityNodeInfo?, username: String): Boolean {
        val handleNode = findSwitcherHandleNode(root, username) ?: return false
        var current: AccessibilityNodeInfo? = handleNode
        repeat(6) {
            val row = current ?: return@repeat
            val descendants = AccessibilityTree.snapshots(row, maxNodes = 900)
            // Stop before a shared list/container can lend another account's checkmark.
            val handles = descendants.flatMap { listOfNotNull(it.text, it.contentDescription) }
                .mapNotNull(::dedicatedHandle).toSet()
            if (descendants.size >= 900 || handles != setOf(XIdentityDetector.normalizeUsername(username))) return false
            if (AccountRowSelectionEvidence.isSelected(descendants, username)) return true
            current = row.parent
        }
        return false
    }

    fun hasAccountListEndActions(root: AccessibilityNodeInfo?): Boolean {
        val labels = rawLabels(root, maxNodes = 900).map(XUiVocabulary::normalize)
        return labels.any { own ->
            XUiVocabulary.accountSwitcherSignals.any { signal ->
                own == signal || own.contains(signal)
            }
        }
    }

    fun looksLikeAccountSwitcher(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val handles = switcherHandleNodes(root)
        if (handles.isEmpty()) return false
        return findSwitcherTitleNode(root) != null || hasAccountListEndActions(root) || handles.size > 1
    }

    fun looksLikeAccountDrawer(root: AccessibilityNodeInfo?): Boolean {
        if (root == null || looksLikeAccountSwitcher(root)) return false
        val labels = rawLabels(root, maxNodes = 550).map(XUiVocabulary::normalize)
        val signalCount = XUiVocabulary.drawerSignals.count { signal ->
            labels.any { own -> own == signal || own.contains(signal) }
        }
        val snapshot = readActiveDrawerAccount(root)
        return snapshot.username != null && (signalCount >= 2 || snapshot.statsComplete)
    }

    fun parseDrawerStats(rawLabels: List<String>): ProfileStats =
        ProfileStatParser.parse(rawLabels).let { ProfileStats(it.followers, it.following) }

    fun readActiveDrawerAccount(root: AccessibilityNodeInfo?): DrawerAccountSnapshot =
        readDrawerAccount(root)

    fun readDrawerAccount(root: AccessibilityNodeInfo?): DrawerAccountSnapshot {
        if (root == null) return DrawerAccountSnapshot()
        val headerNodes = AccessibilityTree.nodes(root, maxNodes = 260)
        val handleNode = headerNodes.firstOrNull { node -> nodeLabels(node).any { dedicatedHandle(it) != null } }
        val username = handleNode?.let { node -> nodeLabels(node).mapNotNull(::dedicatedHandle).firstOrNull() }
        val stats = readDrawerStats(root)
        return DrawerAccountSnapshot(
            username = username,
            displayName = findDisplayNameNearHandle(handleNode, headerNodes, username),
            followers = stats.followers,
            following = stats.following,
        )
    }

    fun readDrawerStats(root: AccessibilityNodeInfo?): ProfileStats =
        parseDrawerStats(rawLabels(root, maxNodes = 500))

    fun readProfileStats(root: AccessibilityNodeInfo?): ProfileStats {
        if (root == null) return ProfileStats()
        val parsed = ProfileStatParser.parse(rawLabels(root, 650))
        fun byId(tokens: List<String>): String? = AccessibilityTree.nodes(root, 650)
            .filter { node -> tokens.any { node.viewIdResourceName.orEmpty().endsWith("/" + it) } }
            .firstNotNullOfOrNull { node ->
                AccessibilityTree.nodes(node, 12).flatMap { nodeLabels(it).toList() }
                    .mapNotNull(ProfileStatParser::number).distinct().singleOrNull()
            }
        return ProfileStats(
            parsed.followers ?: byId(listOf("followers_count", "follower_count")),
            parsed.following ?: byId(listOf("following_count")),
        )
    }

    private fun findSwitcherTitleNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        return AccessibilityTree.nodes(root, maxNodes = 900).firstOrNull { node ->
            nodeLabels(node).map(XUiVocabulary::normalize).any { own ->
                XUiVocabulary.accountSwitcherLabels.any { label -> own == label || own.contains(label) }
            }
        }
    }

    private fun switcherHandleNodes(
        root: AccessibilityNodeInfo?,
        titleNode: AccessibilityNodeInfo? = findSwitcherTitleNode(root),
    ): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        val all = AccessibilityTree.nodes(root, maxNodes = 900)
        val candidates = all.filter { node -> node.isVisibleToUser && nodeLabels(node).any { dedicatedHandle(it) != null } }
        if (titleNode == null) return candidates

        // Başlık bulunduğunda onun üstündeki durum çubuğu/arka plan etiketlerini dışarıda bırak.
        val titleIndex = all.indexOf(titleNode)
        return candidates.filter { all.indexOf(it) > titleIndex }
    }

    private fun findSwitcherHandleNode(
        root: AccessibilityNodeInfo?,
        username: String,
    ): AccessibilityNodeInfo? {
        val wanted = XIdentityDetector.normalizeUsername(username)
        return switcherHandleNodes(root).firstOrNull { node ->
            nodeLabels(node).mapNotNull(::dedicatedHandle).any { it == wanted }
        }
    }

    private fun findDisplayNameNearHandle(
        handleNode: AccessibilityNodeInfo?,
        headerNodes: List<AccessibilityNodeInfo>,
        username: String?,
    ): String? {
        val nearby = linkedSetOf<String>()
        var current = handleNode?.parent
        repeat(3) {
            val node = current ?: return@repeat
            AccessibilityTree.nodes(node, maxNodes = 30).asSequence()
                .flatMap(::nodeLabels)
                .forEach(nearby::add)
            current = node.parent
        }
        if (nearby.isEmpty()) headerNodes.take(80).asSequence().flatMap(::nodeLabels).forEach(nearby::add)

        val normalizedUsername = username?.let(XIdentityDetector::normalizeUsername)
        return nearby.firstOrNull { raw ->
            val text = raw.trim()
            val normalized = XUiVocabulary.normalize(text)
            text.length in 1..80 &&
                dedicatedHandle(text) == null &&
                normalized != normalizedUsername &&
                normalized.none(Char::isDigit) &&
                XUiVocabulary.structuralLabels.none { token -> normalized == token }
        }
    }

    private fun rawLabels(root: AccessibilityNodeInfo?, maxNodes: Int): List<String> {
        if (root == null) return emptyList()
        // Depth-first order keeps a counter next to its label; the shared tree utility is breadth-first.
        val stack = java.util.ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        val labels = mutableListOf<String>()
        var visited = 0
        while (stack.isNotEmpty() && visited++ < maxNodes) {
            val node = stack.removeLast()
            if (node.isPassword) continue
            if (node.isVisibleToUser) labels.addAll(nodeLabels(node).map(String::trim).filter(String::isNotBlank).toList())
            for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let(stack::addLast)
        }
        return labels
    }

    private fun nodeLabels(node: AccessibilityNodeInfo): Sequence<String> =
        sequenceOf(node.text?.toString(), node.contentDescription?.toString()).filterNotNull().distinct()
}
