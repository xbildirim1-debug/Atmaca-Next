package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryRobustness26_34Test {
    private fun n(
        text: String? = null,
        desc: String? = null,
        top: Int,
        left: Int = 20,
        width: Int = 620,
        height: Int = 60,
        id: String? = null,
        clazz: String? = null,
        selected: Boolean = false,
        checked: Boolean = false,
    ) = NodeSnapshot(
        text = text,
        contentDescription = desc,
        viewId = id,
        className = clazz,
        clickable = false,
        enabled = true,
        selected = selected,
        checked = checked,
        bounds = Rect().apply {
            this.left = left
            this.top = top
            right = left + width
            bottom = top + height
        },
        editable = false,
        visible = true,
    )

    @Test
    fun anyEllipsizedFeedHeaderCanResolveToPreviouslyVerifiedTarget() {
        DiscoveryTargetIdentityCache.remember("uzunhedef_12345")
        val nodes = listOf(
            n("Örnek Hesap @uzunhed... · 3 sa", top = 300),
            n("Hedef gönderinin normal metni", top = 380),
        )
        val row = FeedRowEvidence.rows(nodes).single()
        assertEquals("uzunhedef_12345", row.author)
        assertFalse(row.authorTruncated)
        assertTrue(row.age >= 120L)
    }

    @Test
    fun truncatedPrefixCannotResolveToUnverifiedDifferentTarget() {
        DiscoveryTargetIdentityCache.remember("digerhedef_123")
        val header = TweetContentEvidence.header("Örnek Hesap @uzunhed... · 3 sa")!!
        assertTrue(header.truncated)
        assertFalse(TweetContentEvidence.matchesExpected(header, "digerhedef_123"))
    }

    @Test
    fun replyEndLabelsCoverTurkishVariantsAndEnglish() {
        assertTrue(ReplyThreadEndEvidence.isEndLabel("Daha fazla keşfet"))
        assertTrue(ReplyThreadEndEvidence.isEndLabel("Daha fazlasını keşfet"))
        assertTrue(ReplyThreadEndEvidence.isEndLabel("Daha fazlasını keşfedin, başlık"))
        assertTrue(ReplyThreadEndEvidence.isEndLabel("Discover more"))
        assertFalse(ReplyThreadEndEvidence.isEndLabel("Daha fazlasını göster"))
    }

    @Test
    fun discoverySignatureTracksMediaMovementAsScrollEvidence() {
        val base = listOf(
            n("Kullanıcı @deneme · 4 sa", top = 200),
            n("Yorum metni", top = 280),
            n(desc = "Video", top = 360, height = 300, id = "tweet_video", clazz = "android.widget.ImageView"),
        )
        val movedMedia = listOf(
            n("Kullanıcı @deneme · 4 sa", top = 200),
            n("Yorum metni", top = 280),
            n(desc = "Video", top = 370, height = 290, id = "tweet_video", clazz = "android.widget.ImageView"),
        )
        assertNotEquals(DiscoveryViewportEvidence.signature(base), DiscoveryViewportEvidence.signature(movedMedia))
    }

    @Test
    fun repostSurfaceMayBeProvenWithoutComposeSelectedFlag() {
        val repostOnly = listOf(
            n("Gönderi etkileşimleri", top = 60),
            n("Yeniden gönderenler", top = 140),
            n("@birinci", top = 240),
        )
        assertTrue(EngagementListEvidence.selected(repostOnly))

        val ambiguous = repostOnly + n("Alıntılar", top = 140, left = 330, width = 220)
        assertFalse(EngagementListEvidence.selected(ambiguous))
    }
}
