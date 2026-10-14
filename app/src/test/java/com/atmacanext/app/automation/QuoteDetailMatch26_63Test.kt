package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

/** Device screen: opened @pusholder post, verified name, absolute time, inline reply already visible. */
class QuoteDetailMatch26_63Test {
    private val body = "SON DAKİKA: Cumhurbaşkanı Erdoğan, İzmir Büyükşehir Belediye Başkanı Cemil Tugay'a AK Parti rozetini taktı."
    private val attempt = DiscoveryTweetOpenRecovery.Attempt("post", "pusholder", body, 1, 0L)
    private fun n(text: String?, top: Int, bottom: Int = top + 44, left: Int = 40, right: Int = 1000,
        id: String? = null, desc: String? = null) = NodeSnapshot(
        text, desc, id?.let { "com.twitter.android:id/$it" }, null, true, true,
        Rect().apply { this.left = left; this.top = top; this.right = right; this.bottom = bottom })

    private fun opened() = listOf(
        n("Gönderi", 48, 100),
        n("Pusholder @pusholder · 13 dk", 150, 1500),
        n("Pusholder, Doğrulandı", 168, 214, right = 520),
        n("@pusholder, Doğrulandı", 220, 264, right = 460),
        n("Takip et", 176, 230, left = 760, right = 980),
        n(body, 280, 460, id = "tweet_text"),
        n(null, 480, 1080, id = "tweet_image", desc = "Fotoğraf"),
        n("12:26 · 07 Eki 26 · 13,5K Görüntüleme", 1100, 1150),
        n("Alakalı", 1180, 1230, right = 280),
        n("Alıntıları görüntüle", 1180, 1230, left = 560),
        n("Halil @halil1556 · 1 dk", 1280, 1330),
        n("Bu bir alt yorum ve hedef metin değil", 1350, 1420),
        n("Yanıtını gönder", 1560, 1620),
    )

    @Test fun timedParentDoesNotHideTheVerifiedHandleOrThePostText() {
        val nodes = opened()
        assertEquals("pusholder", CommentDetailEvidence.header(nodes)?.handle)
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, attempt))
        assertTrue(QuoteReplyFlowPolicy.detailReady(nodes, XScreen.TWEET_DETAIL, attempt))
    }

    @Test fun aReplyUnderTheOpenedPostCannotSupplyEitherAuthorOrText() {
        val nodes = opened()
        assertFalse(ReplyComposerEvidence.matchesPost(nodes, attempt.copy(author = "halil1556")))
        assertFalse(ReplyComposerEvidence.matchesPost(nodes, attempt.copy(
            text = "Bu bir alt yorum ve hedef metin değil ama yeterince uzun.")))
    }

    @Test fun absoluteTimeDetailWithoutARelativeAgeStillMatches() {
        val nodes = listOf(
            n("Gönderi", 48, 100),
            n("Pusholder, Doğrulandı", 160, 210),
            n("@pusholder, Doğrulandı", 214, 260),
            n("Takip et", 168, 220, left = 760, right = 980),
            n(body, 280, 460, id = "tweet_text"),
            n("12:26 · 07 Eki 26 · 13,5K Görüntüleme", 500, 550),
            n("Alakalı", 580, 630),
            n("Yanıtını gönder", 900, 960),
        )
        assertEquals("pusholder", CommentDetailEvidence.header(nodes)?.handle)
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, attempt))
    }

    @Test fun tallTimedParentWithoutAChildHandleStillUsesThePostBody() {
        val nodes = listOf(
            n("Gönderi", 48, 100),
            n("Pusholder @pusholder · 13 dk", 150, 1400),
            n(body, 280, 460, id = "tweet_text"),
            n("Alakalı", 1180, 1230),
            n("Halil @halil1556 · 1 dk", 1280, 1330),
            n("Yanıtını gönder", 1500, 1560),
        )
        assertNull(CommentDetailEvidence.header(nodes))
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, attempt))
    }

    @Test fun handleBelowATallTimedParentIsStillTheOpenedAuthor() {
        val nodes = listOf(
            n("Gönderi", 48, 100),
            n("Pusholder @pusholder · 13 dk", 150, 1500),
            n("@pusholder", 500, 544, right = 360),
            n(body, 560, 740, id = "tweet_text"),
            n("Alakalı", 1180, 1230),
            n("Halil @halil1556 · 1 dk", 1280, 1330),
        )
        assertEquals("pusholder", CommentDetailEvidence.header(nodes)?.handle)
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, attempt))
    }

    @Test fun quotedTimedHeaderDoesNotHideTheOpenedHandleBelow() {
        val nodes = listOf(
            n("Gönderi", 48, 100),
            n("Birisi @baskasi · 1 gün", 160, 210),
            n("@pusholder", 250, 294, right = 360),
            n(body, 320, 480, id = "tweet_text"),
            n("Alakalı", 520, 570),
        )
        assertEquals("pusholder", CommentDetailEvidence.header(nodes)?.handle)
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, attempt))
    }

    @Test fun hiddenOpenedPostIgnoresReplyAuthorsAndStillMatches() {
        val nodes = listOf(
            n("Gönderi", 48, 100),
            n("Takip et", 140, 190, left = 760, right = 980),
            n("16:25 · 07 Eki 26 • 43,5K Görüntüleme", 220, 270),
            n("Yanıtla", 300, 350),
            n("Alakalı", 420, 470),
            n("Özkan Akın", 500, 544),
            n("@akinozkann", 548, 592),
            n("· 1 dk", 548, 592, left = 400, right = 520),
            n("Yanıtını gönder", 900, 960),
        )
        assertNull(CommentDetailEvidence.header(nodes))
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, attempt))
    }

    @Test fun thinDetailWithoutReplyChromeStillMatchesTheOpenedPost() {
        val nodes = listOf(
            n("Gönderi", 48, 100),
            n("17:11 · 07 Eki 26 · 14,7K Görüntüleme", 420, 470),
            n("Yanıtla", 520, 570, right = 180),
            n("Beğen", 520, 570, left = 700, right = 860),
        )
        assertNull(CommentDetailEvidence.header(nodes))
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, attempt))
        assertTrue(QuoteReplyFlowPolicy.detailReady(nodes, XScreen.TWEET_DETAIL, attempt))
    }
}
