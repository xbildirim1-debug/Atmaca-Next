package com.atmacanext.app.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** A held release keeps the next viewport overlapping; a fling can skip uncounted people. */
object NonFollowerFollowingScroller {
    fun scroll(service: AccessibilityService, root: AccessibilityNodeInfo?, rows: List<NonFollowerFollowingInspector.Row>, forward: Boolean,
               snapshots: List<NodeSnapshot>? = null,
               onGestureStarted: (Long) -> Unit = {},
               onGestureFinished: (Boolean, Long) -> Unit = { _, _ -> }, precision: Boolean = false): Boolean {
        if (root == null) return false
        if (rows.isEmpty()) return false
        val display = service.resources.displayMetrics
        val bounds = ListGestureGeometry.viewport(Rect().also(root::getBoundsInScreen), display.widthPixels, display.heightPixels) ?: return false
        val viewport = AdaptiveNavigationEvidence.contentViewport(snapshots ?: AccessibilityTree.snapshots(root), bounds)
        val visible = rows.filter { it.bounds.top >= viewport.top && it.bounds.bottom <= viewport.bottom }
        val contentTop = maxOf(viewport.top, visible.firstOrNull()?.bounds?.top ?: viewport.top)
        val contentBottom = minOf(viewport.bottom, visible.lastOrNull()?.bounds?.bottom ?: viewport.bottom)
        val span = contentBottom - contentTop
        if (span < 32) return false
        val distance = NonFollowerScrollStride.distance(contentTop, contentBottom, visible.map { it.bounds.top }) *
            if (precision) .35f else 1f
        val x = viewport.left + viewport.width() * .18f
        val start = if (forward) contentBottom - span * .12f else contentTop + span * .12f
        val end = (if (forward) start - distance else start + distance).coerceIn(contentTop.toFloat(), contentBottom.toFloat())
        val stroke = if (distance > 0f) ListGestureGeometry.Stroke(x, start, x, end)
            else ListGestureGeometry.swipe(viewport, rows.map { it.bounds }, forward)
        if (stroke == null || !stroke.valid()) return false
        return try {
            val path = Path().apply { moveTo(stroke.startX, stroke.startY); lineTo(stroke.endX, stroke.endY) }
            onGestureStarted(System.currentTimeMillis())
            val drag = GestureDescription.StrokeDescription(path, 0, HeldDragSequence.MOVE_MS, true)
            fun dispatch(stroke: GestureDescription.StrokeDescription, done: (Boolean) -> Unit): Boolean =
                service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(),
                    object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) = done(true)
                        override fun onCancelled(gestureDescription: GestureDescription?) = done(false)
                    }, null)
            val accepted = HeldDragSequence(
                move = { dispatch(drag, it) },
                hold = { done ->
                    val release = Path().apply { moveTo(stroke.endX, stroke.endY); lineTo(stroke.endX, stroke.endY) }
                    dispatch(drag.continueStroke(release, 0, HeldDragSequence.HOLD_MS, false), done)
                },
                finished = { completed ->
                    OperationLog.i("NONFOLLOWER_SCROLL", "hareket ve sabit bırakma tamam=$completed forward=$forward")
                    onGestureFinished(completed, System.currentTimeMillis())
                },
            ).start()
            OperationLog.i("NONFOLLOWER_SCROLL", "dikey hareket kabul=$accepted forward=$forward rows=${visible.size}")
            accepted
        } catch (error: RuntimeException) {
            onGestureFinished(false, System.currentTimeMillis())
            OperationLog.w("NONFOLLOWER_SCROLL", "dikey hareket gönderilemedi: ${error.javaClass.simpleName}")
            false
        }
    }
}
