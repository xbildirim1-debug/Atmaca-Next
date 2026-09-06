package com.atmacanext.app.automation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** Extracts explicit @handles from X follower/following rows and keeps their screen order. */
object XListInspector {
    data class HandleRow(
        val handle: String,
        val row: AccessibilityNodeInfo,
        val bounds: Rect,
    )

    fun visibleHandleRows(root: AccessibilityNodeInfo?): List<HandleRow> {
        if (root == null) return emptyList()
        val nodes = AccessibilityTree.nodes(root, maxNodes = 1_100)
        val strictRows = nodes
            .asSequence()
            .mapNotNull { node ->
                val handle = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
                    .mapNotNull(XIdentityDetector::extractHandle)
                    .firstOrNull() ?: return@mapNotNull null
                val row = findRow(node) ?: return@mapNotNull null
                val handles = rowHandles(row)
                if (handles != setOf(handle)) return@mapNotNull null
                val bounds = Rect().also(row::getBoundsInScreen)
                if (bounds.isEmpty) return@mapNotNull null
                HandleRow(handle, row, bounds)
            }
            .distinctBy { it.handle }
            .sortedBy { it.bounds.top }
            .toList()

        // X/Compose bazı sürümlerde kullanıcı satırını ayrı bir erişilebilirlik
        // grubu olarak yayınlamıyor. Böyle durumda açık @handle düğümünü, aynı
        // yatay banttaki gerçek ilişki düğmesiyle eşleyip satır olarak kullan.
        val relationshipBounds = nodes.asSequence()
            .filter { node ->
                val normalized = sequenceOf(node.text?.toString(), node.contentDescription?.toString())
                    .filterNotNull()
                    .map(XUiVocabulary::normalize)
                    .firstOrNull(String::isNotBlank)
                normalized != null && (XUiVocabulary.followingActions + setOf("takip et", "follow")).any { token ->
                    normalized == token || normalized.startsWith("$token @") || normalized.startsWith("$token, @")
                }
            }
            .map { node -> Rect().also(node::getBoundsInScreen) }
            .filterNot(Rect::isEmpty)
            .toList()

        val relaxedRows = nodes.asSequence()
            .mapNotNull { node ->
                val handle = sequenceOf(node.text?.toString(), node.contentDescription?.toString())
                    .filterNotNull()
                    .mapNotNull(XIdentityDetector::extractHandle)
                    .firstOrNull() ?: return@mapNotNull null
                val bounds = Rect().also(node::getBoundsInScreen)
                if (bounds.isEmpty || relationshipBounds.none { sameVisualRow(bounds, it) }) return@mapNotNull null
                HandleRow(handle, findRow(node) ?: node, bounds)
            }
            .distinctBy { it.handle }
            .toList()

        return (strictRows + relaxedRows)
            .distinctBy { it.handle }
            .sortedBy { it.bounds.top }
    }

    fun visibleHandles(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val rows = visibleHandleRows(root)
        val explicit = AccessibilityTree.nodes(root, maxNodes = 1_100).asSequence()
            .mapNotNull { node ->
                val bounds = Rect().also(node::getBoundsInScreen)
                if (bounds.isEmpty) return@mapNotNull null
                val handle = sequenceOf(node.text?.toString(), node.contentDescription?.toString())
                    .filterNotNull()
                    .mapNotNull(XIdentityDetector::extractHandle)
                    .firstOrNull() ?: return@mapNotNull null
                Triple(bounds.top, bounds.left, handle)
            }
            .sortedWith(compareBy<Triple<Int, Int, String>> { it.first }.thenBy { it.second })
            .map { it.third }
            .distinct()
            .toList()
        return (rows.map { it.handle } + explicit).distinct()
    }

    fun firstUnprocessedSource(
        root: AccessibilityNodeInfo?,
        ownHandle: String,
        processed: Set<String>,
    ): HandleRow? {
        val own = XIdentityDetector.normalizeUsername(ownHandle)
        return visibleHandleRows(root).firstOrNull { it.handle != own && it.handle !in processed }
    }

    fun clickRow(row: HandleRow): Boolean {
        val clickable = if (TargetVerifier.isSafeClickable(row.row)) row.row else clickableAncestor(row.row)
        return clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    private fun findRow(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = start
        for (index in 0 until 7) {
            val node = current ?: break
            val descendants = AccessibilityTree.nodes(node, maxNodes = 90)
            val handles = rowHandles(node)
            val wholeScreen = descendants.size >= 90
            if (handles.size == 1 && !wholeScreen) return node
            current = node.parent
        }
        return null
    }

    private fun rowHandles(row: AccessibilityNodeInfo): Set<String> =
        AccessibilityTree.nodes(row, maxNodes = 90)
            .asSequence()
            .map { n -> listOfNotNull(n.text?.toString(), n.contentDescription?.toString()) }
            .flatten()
            .mapNotNull(XIdentityDetector::extractHandle)
            .toSet()

    private fun clickableAncestor(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = start
        for (index in 0 until 6) {
            val node = current ?: break
            if (TargetVerifier.isSafeClickable(node) && rowHandles(node).size == 1) return node
            current = node.parent
        }
        return null
    }

    private fun sameVisualRow(first: Rect, second: Rect): Boolean {
        val overlap = minOf(first.bottom, second.bottom) - maxOf(first.top, second.top)
        if (overlap > 0) return true
        val distance = kotlin.math.abs(first.centerY() - second.centerY())
        return distance <= maxOf(first.height(), second.height()).coerceAtLeast(1)
    }
}
