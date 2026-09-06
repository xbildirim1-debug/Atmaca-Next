package com.atmacanext.app.automation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

enum class ScrollAttemptResult { SCROLLED, NO_SCROLL_CONTAINER, ACTION_REJECTED }

data class PendingListScroll(
    val signatureBefore: String,
    val direction: ScrollDirection = ScrollDirection.FORWARD,
    val startedAt: Long = System.currentTimeMillis(),
)

enum class ScrollDirection { FORWARD, BACKWARD }

/** Coordinate-free X list movement using the real scrollable accessibility container. */
object ListViewportController {
    fun signature(root: AccessibilityNodeInfo?): String {
        val snapshots = AccessibilityTree.snapshots(root, maxNodes = 420)
        if (snapshots.isEmpty()) return "empty"
        val material = snapshots.asSequence()
            .mapNotNull { node ->
                val label = listOfNotNull(node.text, node.contentDescription)
                    .joinToString(" ").trim().lowercase(Locale.ROOT)
                    .takeIf { it.isNotBlank() } ?: return@mapNotNull null
                "$label@${node.bounds.top}:${node.bounds.bottom}"
            }
            .take(100)
            .joinToString("|")
        return material.hashCode().toString(16)
    }

    fun tryScrollForward(root: AccessibilityNodeInfo?): ScrollAttemptResult =
        scroll(root, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)

    fun tryScrollBackward(root: AccessibilityNodeInfo?): ScrollAttemptResult =
        scroll(root, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)

    /**
     * X'in ilişki ekranında hem yatay sekme pager'ı hem de dikey kullanıcı
     * listesi scrollable olarak yayınlanabiliyor. Görünür bir kullanıcı
     * satırından yukarı çıkarak en yakın dikey kapsayıcıyı hedeflemek, genel
     * ekran taramasının Aboneler sekmesine kaymasını engeller.
     */
    fun tryScrollUserRowsForward(root: AccessibilityNodeInfo?): ScrollAttemptResult =
        scrollUserRows(root, forward = true)

    fun tryScrollUserRowsBackward(root: AccessibilityNodeInfo?): ScrollAttemptResult =
        scrollUserRows(root, forward = false)

    private fun scrollUserRows(root: AccessibilityNodeInfo?, forward: Boolean): ScrollAttemptResult {
        var current = XListInspector.visibleHandleRows(root).firstOrNull()?.row
            ?: return ScrollAttemptResult.NO_SCROLL_CONTAINER

        repeat(12) {
            val node = current ?: return ScrollAttemptResult.NO_SCROLL_CONTAINER
            val actionIds = node.actionList.asSequence().map { it.id }.toSet()
            val scrollDown = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id
            val scrollUp = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id
            val scrollLeft = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id
            val scrollRight = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id
            val hasVerticalAction = scrollDown in actionIds || scrollUp in actionIds
            val hasHorizontalAction = scrollLeft in actionIds || scrollRight in actionIds
            val hasGenericAction = AccessibilityNodeInfo.ACTION_SCROLL_FORWARD in actionIds ||
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD in actionIds

            if (node.isEnabled && (node.isScrollable || hasVerticalAction || hasGenericAction)) {
                val marker = listOfNotNull(node.className?.toString(), node.viewIdResourceName)
                    .joinToString(" ")
                    .lowercase(Locale.ROOT)
                val collection = node.collectionInfo
                val rows = collection?.rowCount ?: 0
                val columns = collection?.columnCount ?: 0
                val pagerLike = marker.contains("pager") || marker.contains("tablayout") ||
                    marker.contains("horizontal")
                val listLike = marker.contains("recycler") || marker.contains("list") ||
                    marker.contains("scrollview") || marker.contains("timeline")
                val verticalCollection = rows > 1 && columns <= 1
                val horizontalCollection = columns > 1 && rows <= 1

                if (pagerLike || (hasHorizontalAction && !hasVerticalAction) ||
                    (horizontalCollection && !verticalCollection)
                ) {
                    OperationLog.w(
                        "NAV",
                        "UNFOLLOW_SCROLL yatay kapsayıcı reddedildi class=$marker rows=$rows cols=$columns",
                    )
                    return ScrollAttemptResult.ACTION_REJECTED
                }

                val directionalAction = if (forward) scrollDown else scrollUp
                if (directionalAction in actionIds && node.performAction(directionalAction)) {
                    OperationLog.i(
                        "NAV",
                        "UNFOLLOW_SCROLL kullanıcı satırının dikey kapsayıcısı hareket ettirildi",
                    )
                    return ScrollAttemptResult.SCROLLED
                }

                // RecyclerView/ListView gibi klasik listeler yalnızca genel
                // ileri/geri eylemini yayınlayabilir. Bu eyleme sadece satır
                // atasının dikey liste olduğuna dair kanıt varsa izin ver.
                if (listLike || verticalCollection || hasVerticalAction) {
                    val genericAction = if (forward) {
                        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    } else {
                        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                    }
                    if (genericAction in actionIds && node.performAction(genericAction)) {
                        OperationLog.i(
                            "NAV",
                            "UNFOLLOW_SCROLL doğrulanmış liste kapsayıcısı genel eylemle hareket ettirildi",
                        )
                        return ScrollAttemptResult.SCROLLED
                    }
                }

                OperationLog.w(
                    "NAV",
                    "UNFOLLOW_SCROLL dikey kapsayıcı eylemi reddetti class=$marker rows=$rows cols=$columns",
                )
                return ScrollAttemptResult.ACTION_REJECTED
            }
            current = node.parent
        }

        OperationLog.w("NAV", "UNFOLLOW_SCROLL kullanıcı satırı üstünde kaydırılabilir kapsayıcı bulunamadı")
        return ScrollAttemptResult.NO_SCROLL_CONTAINER
    }

    private fun scroll(root: AccessibilityNodeInfo?, action: Int): ScrollAttemptResult {
        val container = bestScrollableContainer(root) ?: return ScrollAttemptResult.NO_SCROLL_CONTAINER
        return if (container.performAction(action)) ScrollAttemptResult.SCROLLED else ScrollAttemptResult.ACTION_REJECTED
    }

    private fun bestScrollableContainer(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        return AccessibilityTree.nodes(root, maxNodes = 520)
            .asSequence()
            .filter { it.isEnabled && it.isScrollable }
            .map { it to score(it) }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun score(node: AccessibilityNodeInfo): Int {
        val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
        val clazz = node.className?.toString()?.lowercase(Locale.ROOT).orEmpty()
        val bounds = Rect().also(node::getBoundsInScreen)
        val area = (bounds.width().coerceAtLeast(0) * bounds.height().coerceAtLeast(0)).coerceAtMost(2_000_000)
        var score = area / 10_000
        if (id.contains("recycler") || id.contains("list") || id.contains("scroll") || id.contains("timeline")) score += 220
        if (clazz.contains("recyclerview") || clazz.contains("listview") || clazz.contains("scrollview")) score += 140
        return score
    }
}
