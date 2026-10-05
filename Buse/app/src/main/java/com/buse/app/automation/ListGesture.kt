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
            // The runtime waits for the post-return tree before trying a gesture.
            // A settling wait must not be counted as an unchanged/end-of-list swipe.
        }
        val bounds = visibleViewport(service, root) ?: return false
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
            durationMs = 62L,
        )
    }

    private fun swipe(
        service: AccessibilityService,
        root: AccessibilityNodeInfo?,
        forward: Boolean,
        horizontal: Boolean = false,
    ): Boolean {
        if (root == null) return false
        val bounds = visibleViewport(service, root) ?: return false
        val stroke = ListGestureGeometry.swipe(bounds,
            XListInspector.visibleHandleRows(root).map { it.bounds }, forward, horizontal) ?: return false
        return dispatchSwipe(service, stroke.startX, stroke.startY, stroke.endX, stroke.endY)
    }

    private fun visibleViewport(service: AccessibilityService, root: AccessibilityNodeInfo): Rect? {
        val bounds = Rect().also(root::getBoundsInScreen)
        val display = service.resources.displayMetrics
        val viewport = ListGestureGeometry.viewport(bounds, display.widthPixels, display.heightPixels) ?: return null
        return AdaptiveNavigationEvidence.contentViewport(AccessibilityTree.snapshots(root), viewport)
    }

    private fun dispatchSwipe(
        service: AccessibilityService,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 42L,
    ): Boolean {
        if (!ListGestureGeometry.Stroke(startX, startY, endX, endY).valid()) return false
        val path = Path().apply { moveTo(startX, startY); lineTo(endX, endY) }
        return dispatch(service, path, durationMs)
    }

    private fun dispatch(service: AccessibilityService, path: Path, durationMs: Long = 42L): Boolean {
        return try {
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
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
        } catch (error: RuntimeException) {
            OperationLog.w("GESTURE_RETRY", "Kaydırma oluşturulamadı/gönderilemedi: ${error.javaClass.simpleName}; yeni ekran okunacak")
            false
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }
}
