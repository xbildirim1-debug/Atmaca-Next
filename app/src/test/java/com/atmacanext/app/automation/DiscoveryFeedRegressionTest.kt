package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class DiscoveryFeedRegressionTest {
    private fun n(text: String, top: Int, id: String? = null, clazz: String = "TextView") = NodeSnapshot(
        text, null, id, clazz, true, true, Rect().apply { left = 20; right = 300; this.top = top; bottom = top + 40 })
    @Test fun combinedTurkishHeaderReadsAuthorAndTwoHours() {
        assertEquals(TweetContentEvidence.Header("pusholder", 120), TweetContentEvidence.header("Pusholder @pusholder · 2 sa"))
        assertEquals(TweetContentEvidence.Header("pusholder", 12), TweetContentEvidence.header("@pusholder · 12 dk"))
        assertFalse(XTweetInspector.eligibleAge(TweetContentEvidence.header("@pusholder · 12 dk")?.ageMinutes))
    }
    @Test fun englishHeaderAndReplyAuthorAreSupported() {
        assertEquals(TweetContentEvidence.Header("ceylocyln", 120), TweetContentEvidence.header("Ceyloo @Ceylocyln · 2 h"))
        assertEquals(TweetContentEvidence.Header("jokerfix0796", 60), TweetContentEvidence.header("ARDAN KADAM @jokerfix0796 · 1 sa"))
    }
    @Test fun bodyMentionsAreNotHeaderTimeEvidence() {
        assertNull(TweetContentEvidence.header("Haber @pusholder bu iş 2 saat sürdü"))
        assertNull(TweetContentEvidence.header("Haber\n@pusholder · 2 sa"))
        assertNull(TweetContentEvidence.header("@pusholder · yeni gönderi"))
    }
    @Test fun bodyIsChosenInsteadOfAuthorVideoAndToolbar() {
        val nodes = listOf(n("Pusholder @pusholder · 2 sa", 100),
            n("Kırıkkale'de trafik denetimini fark eden sürücü...", 160),
            n("Video oynat", 220, "video_player"), n("7 yanıt", 700, "toolbar_reply"))
        assertEquals(1, TweetContentEvidence.bodyIndex(nodes, 140))
    }
    @Test fun mediaOnlyPostDoesNotFallBackToWholeCard() {
        val nodes = listOf(n("@pusholder · 2 sa", 100), n("Video oynat", 160, "video_player"), n("Takip et", 220))
        assertNull(TweetContentEvidence.bodyIndex(nodes, 140))
    }
    @Test fun hiddenTextAndButtonsAreNotPostBody() {
        val nodes = listOf(n("Gizli haber metni", 160).copy(visible = false), n("Videoyu oynat", 200, clazz = "Button"))
        assertNull(TweetContentEvidence.bodyIndex(nodes, 140))
    }
    @Test fun scrolledReplyScreenDoesNotNeedOffscreenTitle() {
        val nodes = listOf(n("Yanıtını gönder", 900), n("Alıntıları görüntüle", 100), n("@Ceylocyln", 200), n("Para lazım, her türlü yakalarız", 250))
        assertEquals(XScreen.TWEET_DETAIL, ScreenDetector.detect(nodes))
    }
    @Test fun profileReplyTabIsNotReplyDetail() {
        val nodes = listOf(n("Gönderiler", 100), n("Yanıtlar", 100), n("@pusholder · 2 sa", 200))
        assertNotEquals(XScreen.TWEET_DETAIL, ScreenDetector.detect(nodes))
    }
    @Test fun suppliedReposterCountIsOnlyATabLabel() {
        assertTrue(EngagementListEvidence.isReposts("66 tarafından yeniden gönderildi"))
        assertFalse(EngagementListEvidence.selected(listOf(n("66 tarafından yeniden gönderildi", 100))))
        assertTrue(EngagementListEvidence.selected(listOf(n("66 tarafından yeniden gönderildi", 100).copy(selected = true))))
    }
}
