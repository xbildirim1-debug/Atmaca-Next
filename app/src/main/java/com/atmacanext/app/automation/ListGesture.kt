package com.atmacanext.app.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * X bir listeyi scrollable olarak yayınlamadığında görünür gerçek kullanıcı
 * satırlarından türetilen, ekran boyutundan bağımsız kaydırma hareketi.
 */
object ListGesture {
    fun forward(service: AccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        swipe(service, root, forward = true)

    fun backward(service: AccessibilityService, root: AccessibilityNodeInfo?): Boolean =
        swipe(service, root, forward = false)

    fun steady(service: AccessibilityService, root: AccessibilityNodeInfo?, forward: Boolean): Boolean =
        swipe(service, root, forward = forward)

    fun discoveryForward(service: AccessibilityService, root: AccessibilityNodeInfo?, recoveryAttempt: Int = 0): Boolean =
        discoverySwipe(service, root, forward = true, recoveryAttempt = recoveryAttempt)

    fun discoveryBackward(service: AccessibilityService, root: AccessibilityNodeInfo?, recoveryAttempt: Int = 0): Boolean =
        discoverySwipe(service, root, forward = false, recoveryAttempt = recoveryAttempt)

    fun discoveryStep(service: AccessibilityService, root: AccessibilityNodeInfo?, forward: Boolean,
                      recoveryAttempt: Int, finished: (Boolean) -> Unit): Boolean =
        discoverySwipe(service, root, forward, recoveryAttempt, finished)

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
        finished: ((Boolean) -> Unit)? = null,
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
        val nodes = AccessibilityTree.snapshots(root)
        val xRatio = bounds.left + bounds.width() * DiscoveryScrollGesturePolicy.xRatio(recoveryAttempt)
        // Tweet detail keeps the finger above the reply field, GIF row and Grok.
        // Profile stays above the bottom navigation rail. The 0.88 constant is unchanged.
        val maxStart = if (screen == XScreen.TWEET_DETAIL) 0.62f else 0.76f
        val raw = DiscoveryScrollGesturePolicy.verticalPath(bounds, nodes, forward, maxStart) ?: return false
        val x = DiscoveryScrollGesturePolicy.safeX(bounds, nodes, xRatio, raw.startY, raw.endY)
        val short = if (finished != null) {
            val distance = minOf(kotlin.math.abs(raw.startY - raw.endY), bounds.height() * DiscoveryPaginationPace.TRAVEL_RATIO)
            DiscoveryScrollGesturePolicy.VerticalPath(raw.startY, raw.startY + if (forward) -distance else distance)
        } else raw
        val path = DiscoveryScrollGesturePolicy.liftOffControls(bounds, nodes, x, short) ?: return false
        if (finished != null) {
            return try {
                val dragPath = Path().apply { moveTo(x, path.startY); lineTo(x, path.endY) }
                val drag = GestureDescription.StrokeDescription(dragPath, 0, DiscoveryPaginationPace.MOVE_MS, true)
                fun dispatch(stroke: GestureDescription.StrokeDescription, done: (Boolean) -> Unit): Boolean =
                    service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(),
                        object : AccessibilityService.GestureResultCallback() {
                            override fun onCompleted(gestureDescription: GestureDescription?) = done(true)
                            override fun onCancelled(gestureDescription: GestureDescription?) = done(false)
                        }, null)
                HeldDragSequence({ dispatch(drag, it) }, { done ->
                    val hold = Path().apply { moveTo(x, path.endY); lineTo(x, path.endY) }
                    dispatch(drag.continueStroke(hold, 0, DiscoveryPaginationPace.HOLD_MS, false), done)
                }, finished).start()
            } catch (_: RuntimeException) { finished(false); false }
        }
        return dispatchSwipe(
            service,
            x,
            path.startY,
            x,
            path.endY,
            durationMs = 180L,
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
        durationMs: Long = 420L,
    ): Boolean {
        if (!ListGestureGeometry.Stroke(startX, startY, endX, endY).valid()) return false
        val path = Path().apply { moveTo(startX, startY); lineTo(endX, endY) }
        return dispatch(service, path, durationMs)
    }

    private fun dispatch(service: AccessibilityService, path: Path, durationMs: Long = 420L): Boolean {
        return try {
            val stroke = GestureDescription.StrokeDescription(path, 0L, (durationMs / 5L).coerceAtLeast(24L))
            val accepted = service.dispatchGesture(
                GestureDescription.Builder().addStroke(stroke).build(),
                null,
                null,
            )
            if (!accepted) return false
            // Do not wait for the completion callback. It does not arrive before the
            // 1.8s timeout, so every profile and comment swipe sat still. The next
            // snapshot checks whether the list actually moved.
            true
        } catch (error: RuntimeException) {
            OperationLog.w("GESTURE_RETRY", "Kaydırma oluşturulamadı/gönderilemedi: ${error.javaClass.simpleName}; yeni ekran okunacak")
            false
        }
    }
}
