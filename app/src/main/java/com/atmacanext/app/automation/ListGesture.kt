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

    private fun swipe(
        service: AccessibilityService,
        root: AccessibilityNodeInfo?,
        forward: Boolean,
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
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 420L)
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
