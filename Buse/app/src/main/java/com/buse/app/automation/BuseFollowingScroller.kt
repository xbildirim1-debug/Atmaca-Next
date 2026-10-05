package com.buse.app.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Fast overlapping drags end with a stationary touch instead of launching an inertial fling. */
object BuseFollowingScroller {
    fun scroll(service: AccessibilityService, root: AccessibilityNodeInfo?, rows: List<BuseFollowingInspector.Row>, forward: Boolean): Boolean {
        if (root == null || rows.isEmpty()) return false
        val display = service.resources.displayMetrics
        val bounds = ListGestureGeometry.viewport(Rect().also(root::getBoundsInScreen), display.widthPixels, display.heightPixels) ?: return false
        val viewport = AdaptiveNavigationEvidence.contentViewport(AccessibilityTree.snapshots(root), bounds)
        val visible = rows.filter { it.bounds.top >= viewport.top && it.bounds.bottom <= viewport.bottom }
        if (visible.isEmpty()) return false
        val contentTop = maxOf(viewport.top, visible.first().bounds.top)
        val contentBottom = minOf(viewport.bottom, visible.last().bounds.bottom)
        val span = contentBottom - contentTop
        if (span < 32) return false
        val distance = minOf(span * .40f, visible.minOf { it.bounds.height() }.coerceAtLeast(32) * 1.5f)
        val x = viewport.left + viewport.width() * .12f
        val start = if (forward) contentBottom - span * .10f else contentTop + span * .10f
        val end = if (forward) start - distance else start + distance
        if (!ListGestureGeometry.Stroke(x, start, x, end).valid()) return false
        return try {
            val path = Path().apply { moveTo(x, start); lineTo(x, end) }
            val completed = AtomicBoolean(false)
            val latch = CountDownLatch(1)
            val drag = GestureDescription.StrokeDescription(path, 0, 22L, true)
            val releasePath = Path().apply { moveTo(x, end) }
            val release = drag.continueStroke(releasePath, 0L, 120L, false)
            val releaseGesture = GestureDescription.Builder().addStroke(release).build()
            val releaseCallback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) { completed.set(true); latch.countDown() }
                override fun onCancelled(gestureDescription: GestureDescription?) { latch.countDown() }
            }
            val accepted = service.dispatchGesture(GestureDescription.Builder().addStroke(drag).build(),
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        try {
                            if (!service.dispatchGesture(releaseGesture, releaseCallback, null)) latch.countDown()
                        } catch (_: RuntimeException) { latch.countDown() }
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) { latch.countDown() }
                }, null)
            accepted && (Looper.myLooper() == Looper.getMainLooper() || (latch.await(1800, TimeUnit.MILLISECONDS) && completed.get()))
        } catch (_: RuntimeException) { false }
        catch (_: InterruptedException) { Thread.currentThread().interrupt(); false }
    }
}
