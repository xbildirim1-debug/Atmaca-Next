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

/**
 * X bir listeyi scrollable olarak yayınlamadığında görünür gerçek kullanıcı
 * satırlarından türetilen, ekran boyutundan bağımsız kaydırma hareketi.
 */
object ListGesture {
    fun forward(service: AccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        swipe(service, root, forward = true)

    fun backward(service: AccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        swipe(service, root, forward = false)

    fun discoveryForward(service: AccessibilityService, root: AccessibilityNodeInfo?, recoveryAttempt: Int = 0): Boolean =
        discoverySwipe(service, root, forward = true, recoveryAttempt = recoveryAttempt)

    fun discoveryBackward(service: AccessibilityService, root: AccessibilityNodeInfo?, recoveryAttempt: Int = 0): Boolean =
        discoverySwipe(service, root, forward = false, recoveryAttempt = recoveryAttempt)

    fun left(service: AccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        swipe(service, root, forward = true, horizontal = true)

    fun right(service: AccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        swipe(service, root, forward = false, horizontal = true)

    /**
     * Profile media consumes centre-screen swipes on X. Use the narrow left gutter,
     * which stays outside inline players and the lower-right compose button.
     */
    private fun discoverySwipe(
        service: AccessibilityService,
        root: AccessibilityNodeInfo?,
        forward: Boolean,
        recoveryAttempt: Int,
    ): Boolean {
        if (root == null) return false
        val bounds = Rect().also(root::getBoundsInScreen)
        if (bounds.width() <= 0 || bounds.height() <= 0) return false
        val x = bounds.left + bounds.width() * DiscoveryScrollGesturePolicy.xRatio(recoveryAttempt)
        val startRatio = if (forward) DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO
            else DiscoveryScrollGesturePolicy.BACKWARD_START_Y_RATIO
        val endRatio = if (forward) DiscoveryScrollGesturePolicy.FORWARD_END_Y_RATIO
            else DiscoveryScrollGesturePolicy.BACKWARD_END_Y_RATIO
        return dispatchSwipe(
            service,
            x,
            bounds.top + bounds.height() * startRatio,
            x,
            bounds.top + bounds.height() * endRatio,
            durationMs = 620L,
        )
    }

    private fun swipe(
        service: AccessibilityService,
        root: AccessibilityNodeInfo?,
        forward: Boolean,
        horizontal: Boolean = false,
    ): Boolean {
        if (root == null) return false
        val rootBounds = Rect().also(root::getBoundsInScreen)
        if (rootBounds.width() <= 0 || rootBounds.height() <= 0) return false

        val rows = XListInspector.visibleHandleRows(root)
            .map { it.bounds }
            .filterNot(Rect::isEmpty)
            .sortedBy { it.top }
        val top = rows.firstOrNull()?.centerY()
            ?: (rootBounds.top + rootBounds.height() * 0.30f).toInt()
        val bottom = rows.lastOrNull()?.centerY()
            ?: (rootBounds.top + rootBounds.height() * 0.78f).toInt()
        val minimumTravel = (rootBounds.height() * 0.24f).toInt().coerceAtLeast(1)
        val adaptiveTop = if (bottom - top >= minimumTravel) top else rootBounds.top + (rootBounds.height() * 0.30f).toInt()
        val adaptiveBottom = if (bottom - top >= minimumTravel) bottom else rootBounds.top + (rootBounds.height() * 0.78f).toInt()
        if (adaptiveBottom <= adaptiveTop) return false

        val x = rootBounds.exactCenterX()
        val startY = if (forward) adaptiveBottom.toFloat() else adaptiveTop.toFloat()
        val endY = if (forward) adaptiveTop.toFloat() else adaptiveBottom.toFloat()
        val path = Path().apply {
            if (horizontal) {
                val y = rootBounds.top + rootBounds.height() * 0.55f
                moveTo(rootBounds.left + rootBounds.width() * (if (forward) 0.82f else 0.18f), y)
                lineTo(rootBounds.left + rootBounds.width() * (if (forward) 0.18f else 0.82f), y)
            } else {
                moveTo(x, startY)
                lineTo(x, endY)
            }
        }
        return dispatch(service, path)
    }

    private fun dispatchSwipe(
        service: AccessibilityService,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 420L,
    ): Boolean {
        val path = Path().apply { moveTo(startX, startY); lineTo(endX, endY) }
        return dispatch(service, path, durationMs)
    }

    private fun dispatch(service: AccessibilityService, path: Path, durationMs: Long = 420L): Boolean {
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        return try {
            val completed = AtomicBoolean(false)
            val latch = CountDownLatch(1)
            val accepted = service.dispatchGesture(
                GestureDescription.Builder().addStroke(stroke).build(),
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        completed.set(true)
                        latch.countDown()
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        latch.countDown()
                    }
                },
                null,
            )
            if (!accepted) return false
            if (Looper.myLooper() == Looper.getMainLooper()) return true
            latch.await(1_800L, TimeUnit.MILLISECONDS) && completed.get()
        } catch (_: Throwable) {
            false
        }
    }
}
