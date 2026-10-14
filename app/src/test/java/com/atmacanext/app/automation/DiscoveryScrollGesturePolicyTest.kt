package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun wideBottomReplyPhraseIsNotAScrollOrigin() {
        val root = rect(0, 0, 1080, 2400)
        val composer = node(text = "Tweetini yanıtla", top = 2140, bottom = 2260).copy(
            bounds = rect(40, 2140, 1000, 2260),
        )
        val path = DiscoveryScrollGesturePolicy.verticalPath(root, listOf(composer), true)
        assertNotNull(path)
        assertTrue(path!!.startY < composer.bounds.top)
    }

    @Test fun smallReplyIconsDoNotCancelTheProfileGesture() {
        val root = rect(0, 0, 1080, 2400)
        val reply = node(text = "Yanıtla", top = 900, bottom = 980).copy(bounds = rect(80, 900, 180, 980))
        val path = DiscoveryScrollGesturePolicy.verticalPath(root, listOf(reply), true)
        assertNotNull(path)
        assertEquals(2400f * DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO, path!!.startY, 0.5f)
    }

    @Test fun retweetIconPushesTheGutterOffTheActionRow() {
        val root = rect(0, 0, 1080, 2400)
        val reply = node(text = "Yanıtla", top = 1800, bottom = 1900).copy(bounds = rect(40, 1800, 140, 1900))
        val repost = node(text = "Yeniden gönder", top = 1800, bottom = 1900).copy(bounds = rect(180, 1800, 300, 1900))
        val preferred = 1080 * DiscoveryScrollGesturePolicy.xRatio(1)
        val safe = DiscoveryScrollGesturePolicy.safeX(root, listOf(reply, repost), preferred, 2100f, 500f)
        assertTrue(safe < reply.bounds.left)
        assertTrue(safe > 0f)
    }

    @Test fun replyComposerIsNotACommenterRow() {
        val composer = node(text = "Tweetini yanıtla", top = 2140, bottom = 2260).copy(
            bounds = rect(40, 2140, 1000, 2260),
        )
        val comment = node(text = "Yanıtla", top = 900, bottom = 980).copy(bounds = rect(80, 900, 180, 980))
        val ceiling = DiscoveryScrollGesturePolicy.composerCeiling(listOf(composer, comment))
        assertNotNull(ceiling)
        assertTrue(ceiling!! <= composer.bounds.top)
        assertTrue(comment.bounds.top < ceiling)
    }

    @Test fun repostMenuIsNotTheTweetActionIcon() {
        val sheet = listOf(
            node(text = "Yeniden gönder", top = 1600, bottom = 1700),
            node(text = "Alıntıla", top = 1720, bottom = 1820),
        )
        assertTrue(DiscoveryScrollGesturePolicy.repostSheet(sheet))
        assertFalse(DiscoveryScrollGesturePolicy.repostSheet(listOf(node(text = "Yeniden gönderildi", top = 100, bottom = 160))))
        assertFalse(DiscoveryScrollGesturePolicy.repostSheet(listOf(node(text = "Alıntılar", top = 100, bottom = 160))))
    }

    @Test fun tweetDetailStartStaysAboveGifGrokAndTheReplyField() {
        val root = rect(0, 0, 1080, 2400)
        val gif = node(text = "GIF", top = 2060, bottom = 2180).copy(bounds = rect(24, 2060, 150, 2180))
        val grok = node(text = "Grok", top = 2200, bottom = 2320).copy(bounds = rect(430, 2200, 560, 2320))
        val path = DiscoveryScrollGesturePolicy.verticalPath(root, listOf(gif, grok), true, maxStartRatio = 0.62f)
        assertNotNull(path)
        assertTrue(path!!.startY <= 2400f * 0.62f + 0.5f)
        assertTrue(path.startY < gif.bounds.top)
        assertTrue(path.startY < grok.bounds.top)
        val plain = DiscoveryScrollGesturePolicy.verticalPath(root, emptyList(), true)!!
        val lifted = DiscoveryScrollGesturePolicy.liftOffControls(root, listOf(gif, grok), 70f, plain)
        assertNotNull(lifted)
        assertTrue(lifted!!.startY < gif.bounds.top)
    }

    @Test fun commentAvatarPushesTheFingerOffTheRow() {
        val root = rect(0, 0, 1080, 2400)
        val avatar = node(text = "@ali", top = 1200, bottom = 1320).copy(bounds = rect(36, 1200, 150, 1314))
        val preferred = 1080 * DiscoveryScrollGesturePolicy.xRatio(0)
        assertTrue(preferred > avatar.bounds.left && preferred < avatar.bounds.right)
        val safe = DiscoveryScrollGesturePolicy.safeX(root, listOf(avatar), preferred, 1500f, 400f)
        assertTrue(safe < avatar.bounds.left.toFloat())
    }
}
