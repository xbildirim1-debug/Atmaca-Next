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
     * Profile/tweet media can consume centre-screen swipes on X. Use a narrow left
     * gutter outside inline players. On a tweet detail, once X exposes its semantic
     * end-of-replies marker, never scroll into recommendation content; the runtime
     * will observe the stable viewport and return to the target post/profile.
     */
    private fun discoverySwipe(
        service: AccessibilityService,
        root: AccessibilityNodeInfo?,
        forward: Boolean,
        recoveryAttempt: Int,
    ): Boolean {
        if (root == null) return false
        val screen = ScreenDetector.detect(root)
        if (screen != XScreen.TWEET_DETAIL) {
            // A guard belongs only to the reply viewport that armed it. Never let
            // it leak into profile discovery or an engagement-list task.
            CommenterViewportPolicy.clearScrollGuard()
        }
        if (forward && screen == XScreen.TWEET_DETAIL) {
            if (ReplyThreadEndEvidence.visible(root)) {
                OperationLog.i("COMMENT_END", "Yorum sonu işareti görüldü; Daha fazla keşfet alanına kaydırılmadı")
                return false
            }
            val snapshots = AccessibilityTree.snapshots(root)
            val visibleAuthors = XTweetInspector.visibleReplyAuthors(root)
            val signature = DiscoveryViewportEvidence.signature(snapshots)
            if (!CommenterViewportPolicy.allowScrollAfterStableEmpty(visibleAuthors, signature)) {
                OperationLog.i(
                    "COMMENT_DRAIN_WAIT",
                    "Yorum görünümü Back sonrası henüz kesin boş değil; görünür adaylar tekrar okunmadan kaydırılmadı",
                )
                return false
            }
        }
        val bounds = Rect().also(root::getBoundsInScreen)
        if (bounds.width() <= 0 || bounds.height() <= 0) return false
        val x = bounds.left + bounds.width() * DiscoveryScrollGesturePolicy.xRatio(recoveryAttempt)
        val path = DiscoveryScrollGesturePolicy.verticalPath(
            bounds,
            AccessibilityTree.snapshots(root),
            forward,
        ) ?: return false
        return dispatchSwipe(
            service,
            x,
            path.startY,
            x,
            path.endY,
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
