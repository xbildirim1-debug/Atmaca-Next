package com.atmacanext.app.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** A short flick was cancelled by the next read. Drag long enough for X to move the following list. */
object NonFollowerFollowingScroller {
    fun scroll(service: AccessibilityService, root: AccessibilityNodeInfo?, rows: List<NonFollowerFollowingInspector.Row>, forward: Boolean,
               snapshots: List<NodeSnapshot>? = null,
               onGestureStarted: (Long) -> Unit = {},
               onGestureFinished: (Boolean, Long) -> Unit = { _, _ -> }): Boolean {
        if (root == null) return false
        val access = if (forward) ListViewportController.tryScrollUserRowsForward(root)
            else ListViewportController.tryScrollUserRowsBackward(root)
        if (access == ScrollAttemptResult.SCROLLED) return true
        if (rows.isEmpty()) return false
        val display = service.resources.displayMetrics
        val bounds = ListGestureGeometry.viewport(Rect().also(root::getBoundsInScreen), display.widthPixels, display.heightPixels) ?: return false
        val viewport = AdaptiveNavigationEvidence.contentViewport(snapshots ?: AccessibilityTree.snapshots(root), bounds)
        val visible = rows.filter { it.bounds.top >= viewport.top && it.bounds.bottom <= viewport.bottom }
        val contentTop = maxOf(viewport.top, visible.firstOrNull()?.bounds?.top ?: viewport.top)
        val contentBottom = minOf(viewport.bottom, visible.lastOrNull()?.bounds?.bottom ?: viewport.bottom)
        val span = contentBottom - contentTop
        if (span < 32) return false
        val distance = NonFollowerScrollStride.distance(contentTop, contentBottom, visible.map { it.bounds.top })
        val x = viewport.left + viewport.width() * .18f
        val start = if (forward) contentBottom - span * .12f else contentTop + span * .12f
        val end = (if (forward) start - distance else start + distance).coerceIn(contentTop.toFloat(), contentBottom.toFloat())
        val stroke = if (distance > 0f) ListGestureGeometry.Stroke(x, start, x, end)
            else ListGestureGeometry.swipe(viewport, rows.map { it.bounds }, forward)
        if (stroke == null || !stroke.valid()) return false
        return try {
            val path = Path().apply { moveTo(stroke.startX, stroke.startY); lineTo(stroke.endX, stroke.endY) }
            onGestureStarted(System.currentTimeMillis())
            val drag = GestureDescription.StrokeDescription(path, 0, 280L, false)
            val accepted = service.dispatchGesture(GestureDescription.Builder().addStroke(drag).build(),
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        OperationLog.i("NONFOLLOWER_SCROLL", "dikey hareket tamamlandı forward=$forward")
                        onGestureFinished(true, System.currentTimeMillis())
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        OperationLog.w("NONFOLLOWER_SCROLL", "dikey hareket iptal edildi forward=$forward; liste sonu sayılmayacak")
                        onGestureFinished(false, System.currentTimeMillis())
                    }
                }, null)
            OperationLog.i("NONFOLLOWER_SCROLL", "dikey hareket kabul=$accepted forward=$forward rows=${visible.size}")
            if (!accepted) onGestureFinished(false, System.currentTimeMillis())
            accepted
        } catch (error: RuntimeException) {
            onGestureFinished(false, System.currentTimeMillis())
            OperationLog.w("NONFOLLOWER_SCROLL", "dikey hareket gönderilemedi: ${error.javaClass.simpleName}")
            false
        }
    }
}
