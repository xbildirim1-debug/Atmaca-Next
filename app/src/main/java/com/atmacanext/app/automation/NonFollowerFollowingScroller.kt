package com.atmacanext.app.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** A short flick was cancelled by the next read. Drag long enough for X to move the following list. */
object NonFollowerFollowingScroller {
    fun scroll(service: AccessibilityService, root: AccessibilityNodeInfo?, rows: List<NonFollowerFollowingInspector.Row>, forward: Boolean,
               snapshots: List<NodeSnapshot>? = null): Boolean {
        if (root == null) return false
        val access = if (forward) ListViewportController.tryScrollUserRowsForward(root)
            else ListViewportController.tryScrollUserRowsBackward(root)
        if (access == ScrollAttemptResult.SCROLLED) return true
        if (rows.isEmpty()) return false
        val display = service.resources.displayMetrics
        val bounds = ListGestureGeometry.viewport(Rect().also(root::getBoundsInScreen), display.widthPixels, display.heightPixels) ?: return false
        val viewport = AdaptiveNavigationEvidence.contentViewport(snapshots ?: AccessibilityTree.snapshots(root), bounds)
        val visible = rows.filter { it.bounds.top >= viewport.top && it.bounds.bottom <= viewport.bottom }
        if (visible.isEmpty()) return ListGesture.steady(service, root, forward)
        val contentTop = maxOf(viewport.top, visible.first().bounds.top)
        val contentBottom = minOf(viewport.bottom, visible.last().bounds.bottom)
        val span = contentBottom - contentTop
        if (span < 32) return ListGesture.steady(service, root, forward)
        val distance = NonFollowerScrollStride.distance(contentTop, contentBottom, visible.map { it.bounds.top })
        if (distance <= 0f) return ListGesture.steady(service, root, forward)
        val x = viewport.left + viewport.width() * .18f
        val start = if (forward) contentBottom - span * .12f else contentTop + span * .12f
        val end = if (forward) start - distance else start + distance
        if (!ListGestureGeometry.Stroke(x, start, x, end).valid()) return ListGesture.steady(service, root, forward)
        return try {
            val path = Path().apply { moveTo(x, start); lineTo(x, end) }
            val completed = AtomicBoolean(false)
            val latch = CountDownLatch(1)
            val drag = GestureDescription.StrokeDescription(path, 0, 280L, false)
            val accepted = service.dispatchGesture(GestureDescription.Builder().addStroke(drag).build(),
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) { completed.set(true); latch.countDown() }
                    override fun onCancelled(gestureDescription: GestureDescription?) { latch.countDown() }
                }, null)
            if (!accepted) return ListGesture.steady(service, root, forward)
            if (Looper.myLooper() == Looper.getMainLooper()) return true
            (latch.await(1800, TimeUnit.MILLISECONDS) && completed.get()) || ListGesture.steady(service, root, forward)
        } catch (_: RuntimeException) { ListGesture.steady(service, root, forward) }
        catch (_: InterruptedException) { Thread.currentThread().interrupt(); false }
    }
}
