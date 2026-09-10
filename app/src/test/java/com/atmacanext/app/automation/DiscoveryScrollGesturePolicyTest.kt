package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryScrollGesturePolicyTest {
    private fun rect(left: Int, top: Int, right: Int, bottom: Int) = Rect().apply {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    private fun height(rect: Rect): Int = rect.bottom - rect.top

    private fun node(
        text: String? = null,
        top: Int,
        bottom: Int,
        editable: Boolean = false,
        id: String? = null,
    ) = NodeSnapshot(
        text = text,
        contentDescription = null,
        viewId = id,
        className = "TextView",
        clickable = true,
        enabled = true,
        bounds = rect(20, top, 680, bottom),
        editable = editable,
        visible = true,
    )

    @Test fun discoverySwipeStaysInLeftGutterAwayFromMediaAndCompose() {
        assertTrue(DiscoveryScrollGesturePolicy.xRatio(0) < 0.10f)
        assertTrue(DiscoveryScrollGesturePolicy.xRatio(1) < 0.15f)
        assertTrue(DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO > 0.85f)
        assertTrue(DiscoveryScrollGesturePolicy.FORWARD_END_Y_RATIO < 0.25f)
    }

    @Test fun swallowedSwipeAlternatesBetweenTwoSafeGutters() {
        assertTrue(DiscoveryScrollGesturePolicy.xRatio(0) != DiscoveryScrollGesturePolicy.xRatio(1))
        assertTrue(DiscoveryScrollGesturePolicy.xRatio(0) == DiscoveryScrollGesturePolicy.xRatio(2))
    }

    @Test fun noComposerKeepsExistingForwardAndBackwardPath() {
        val root = rect(0, 0, 720, 1536)
        val rootHeight = height(root)
        val forward = DiscoveryScrollGesturePolicy.verticalPath(root, emptyList(), true)!!
        val backward = DiscoveryScrollGesturePolicy.verticalPath(root, emptyList(), false)!!
        assertEquals(rootHeight * DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO, forward.startY, 0.5f)
        assertEquals(rootHeight * DiscoveryScrollGesturePolicy.FORWARD_END_Y_RATIO, forward.endY, 0.5f)
        assertEquals(forward.endY, backward.startY, 0.5f)
        assertEquals(forward.startY, backward.endY, 0.5f)
    }

    @Test fun inlineReplyLabelMovesGestureStartAboveComposer() {
        val root = rect(0, 0, 720, 1536)
        val composer = node(text = "Yanıtını gönder", top = 1300, bottom = 1360)
        val path = DiscoveryScrollGesturePolicy.verticalPath(root, listOf(composer), true)
        assertNotNull(path)
        assertTrue(path!!.startY < composer.bounds.top)
        assertTrue(path.startY < height(root) * DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO)
        assertTrue(path.endY < path.startY)
    }

    @Test fun editableReplyContainerAlsoCannotBeGestureOrigin() {
        val root = rect(0, 0, 1080, 2400)
        val composer = node(top = 1900, bottom = 2100, editable = true, id = "reply_composer_input")
        val path = DiscoveryScrollGesturePolicy.verticalPath(root, listOf(composer), true)
        assertNotNull(path)
        assertTrue(path!!.startY < composer.bounds.top)
    }
}
