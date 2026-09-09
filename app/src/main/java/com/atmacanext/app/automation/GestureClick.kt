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

/** ACTION_CLICK başarısızsa node'un gerçek ekran sınırlarının merkezine Accessibility gesture gönderir. */
object GestureClick {
    /** Click only this exact semantic node; never climb into a row/card parent. */
    fun clickNodeOnly(service: AccessibilityService, node: AccessibilityNodeInfo): Boolean {
        if (node.isEnabled && node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        return gestureTap(service, node)
    }

    fun click(service: AccessibilityService, node: AccessibilityNodeInfo): Boolean {
        if (node.isEnabled && node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        var parent: AccessibilityNodeInfo? = node.parent
        repeat(6) {
            val cur = parent ?: return@repeat
            if (cur.isEnabled && cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            parent = cur.parent
        }
        return gestureTap(service, node)
    }

    fun gestureTap(service: AccessibilityService, node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect().also(node::getBoundsInScreen)
        if (bounds.width() <= 0 || bounds.height() <= 0) return false
        return gestureTapAt(service, bounds.exactCenterX(), bounds.exactCenterY())
    }

    /** Tap the leading part of a wide author header instead of its card-like centre. */
    fun gestureTapLeading(service: AccessibilityService, node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect().also(node::getBoundsInScreen)
        if (bounds.width() <= 0 || bounds.height() <= 0) return false
        return gestureTapAt(service, bounds.left + bounds.width() * 0.22f, bounds.exactCenterY())
    }

    /** Stay in the upper text, away from a trailing inline Show more span. */
    fun gestureTapText(service: AccessibilityService, node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect().also(node::getBoundsInScreen)
        if (!node.isVisibleToUser || !node.isEnabled || bounds.width() <= 0 || bounds.height() <= 0) return false
        return gestureTapAt(service, bounds.left + bounds.width() * 0.55f, bounds.top + bounds.height() * 0.15f)
    }

    fun tapAtRatio(
        service: AccessibilityService,
        root: AccessibilityNodeInfo?,
        xRatio: Float,
        yRatio: Float,
    ): Boolean {
        if (root == null || xRatio !in 0f..1f || yRatio !in 0f..1f) return false
        val bounds = Rect().also(root::getBoundsInScreen)
        if (bounds.width() <= 0 || bounds.height() <= 0) return false
        val x = bounds.left + bounds.width() * xRatio
        val y = bounds.top + bounds.height() * yRatio
        return gestureTapAt(service, x, y)
    }

    private fun gestureTapAt(service: AccessibilityService, x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 60L)
        return try {
            val result = AtomicBoolean(false)
            val latch = CountDownLatch(1)
            val accepted = service.dispatchGesture(
                GestureDescription.Builder().addStroke(stroke).build(),
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        result.set(true)
                        latch.countDown()
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        latch.countDown()
                    }
                },
                null,
            )
            if (!accepted) return false
            // The automation snapshot actor runs off the main looper, so it can safely wait for
            // Android's real completion callback. Main-thread callers still rely on postcondition.
            if (Looper.myLooper() == Looper.getMainLooper()) return true
            latch.await(1_500L, TimeUnit.MILLISECONDS) && result.get()
        } catch (_: Throwable) { false }
    }
}
