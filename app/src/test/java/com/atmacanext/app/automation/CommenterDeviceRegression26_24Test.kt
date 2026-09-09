package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommenterDeviceRegression26_24Test {
    private fun n(
        text: String? = null,
        top: Int,
        left: Int = 20,
        width: Int = 300,
        height: Int = 50,
        id: String? = null,
        clazz: String? = null,
        desc: String? = null,
        editable: Boolean = false,
    ) = NodeSnapshot(
        text = text,
        contentDescription = desc,
        viewId = id,
        className = clazz,
        clickable = false,
        enabled = true,
        bounds = Rect(left, top, left + width, top + height),
        editable = editable,
        visible = true,
    )

    @Test
    fun physicalDeviceCommentDetailWithInlineReplyAndFollowIsTweetDetail() {
        val nodes = listOf(
            n("Gönderi", 60, width = 180),
            n("Zeyn-app", 180, left = 105, width = 240),
            n("@zeynepdemirr92", 235, left = 105, width = 260),
            n("Takip et", 205, left = 500, width = 145),
            n("Benim girerim diyenler girsinler de gebersinler bi", 290, width = 620, height = 90),
            n("08:25 · 09 Eyl 26 · 224 Görüntüleme", 390, width = 500),
            n("Yanıtını gönder", 1280, width = 570, editable = true),
        )
        assertEquals("zeynepdemirr92", CommentDetailEvidence.header(nodes)?.handle)
        assertEquals(XScreen.TWEET_DETAIL, ScreenDetector.detect(nodes))
        assertEquals(3, CommentDetailEvidence.actionIndex(nodes, "zeynepdemirr92", VerifiedFollowPolicy.plainFollowLabels))
    }

    @Test
    fun avatarDoesNotMakeTextReplyMedia() {
        val nodes = listOf(
            n(top = 100, left = 20, width = 72, height = 72, id = "profile_image", clazz = "android.widget.ImageView", desc = "Profil fotoğrafı"),
            n("@plainreply", 110, left = 105),
            n("yalnız metin", 190, left = 20, width = 600, height = 70),
        )
        assertFalse(ReplyMediaEvidence.hasMedia(nodes, 90, 350))
    }

    @Test
    fun largeTweetImageMakesReplyMedia() {
        val nodes = listOf(
            n("@mediareply", 110, left = 105),
            n(top = 200, left = 20, width = 620, height = 360, id = "tweet_photo", clazz = "android.widget.ImageView", desc = "Fotoğraf"),
        )
        assertTrue(ReplyMediaEvidence.hasMedia(nodes, 90, 600))
    }
}
