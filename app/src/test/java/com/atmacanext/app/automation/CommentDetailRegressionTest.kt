package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class CommentDetailRegressionTest {
    private fun n(text: String?, x: Int, y: Int, width: Int = 200, height: Int = 35) = NodeSnapshot(
        text, null, null, "TextView", true, true,
        Rect().apply { left = x; top = y; right = x + width; bottom = y + height })
    private fun detail(handle: String = "arya_krmzgl", action: String = "Takip et") = listOf(
        n("Gönderi", 100, 75), n("Arya", 100, 190), n("@$handle", 100, 230),
        n(action, 500, 207, 140, 45), n("Gönderinin metni", 20, 290, 620, 150),
        n("00:41 · 09 Eyl 26 · 320 Görüntülenme", 20, 470, 620),
        n("Bom Report @BomReport · 7 sa", 100, 720, 460), n("Takip et", 500, 810, 140),
        n("Yanıtını gönder", 100, 1320).copy(editable = true))

    @Test fun suppliedOpenedReplySelectsOwnTopRightFollow() {
        assertEquals("arya_krmzgl", CommentDetailEvidence.header(detail())?.handle)
        assertEquals(3, CommentDetailEvidence.actionIndex(detail(), "arya_krmzgl", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun arbitraryAuthorAndEnglishFollowUseSameSelector() {
        assertEquals(3, CommentDetailEvidence.actionIndex(detail("random_user42", "Follow"), "random_user42", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun targetMismatchCannotFollowMainPostAuthor() {
        assertNull(CommentDetailEvidence.actionIndex(detail("pusholder"), "arya_krmzgl", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun replyFollowCannotSubstituteMissingTopFollow() {
        assertNull(CommentDetailEvidence.actionIndex(detail().filterIndexed { i, _ -> i != 3 }, "arya_krmzgl", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun alreadyFollowingAndRequestedAreSeparateFromAvailable() {
        for (label in listOf("Takip ediliyor", "Beklemede", "Following", "Requested")) {
            assertTrue(CommentDetailEvidence.has(detail(action = label), "arya_krmzgl", XUiVocabulary.followingActions + XUiVocabulary.requestedActions))
            assertFalse(CommentDetailEvidence.has(detail(action = label), "arya_krmzgl", VerifiedFollowPolicy.plainFollowLabels))
        }
    }
    @Test fun hiddenAndDisabledFollowAreRejected() {
        assertNull(CommentDetailEvidence.actionIndex(detail().mapIndexed { i, n -> if (i == 3) n.copy(visible = false) else n }, "arya_krmzgl", VerifiedFollowPolicy.plainFollowLabels))
        assertNull(CommentDetailEvidence.actionIndex(detail().mapIndexed { i, n -> if (i == 3) n.copy(enabled = false) else n }, "arya_krmzgl", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun timedReplyHeaderIsNotOpenedDetailAuthor() {
        val nodes = listOf(n("Gönderi", 100, 75), n("Bom Report @BomReport · 7 sa", 100, 180),
            n("@body_mention", 100, 250), n("Takip et", 500, 250))
        assertNull(CommentDetailEvidence.header(nodes))
    }
    @Test fun sameChildScreenCannotBeAcceptedAsReturn() {
        assertFalse(DiscoveryViewportEvidence.returnedToParent("a", "b", listOf("reply"), listOf("reply"), "arya", "arya"))
        assertFalse(DiscoveryViewportEvidence.returnedToParent("a", "b", listOf("parent", "reply"), listOf("reply", "nested"), null, "arya"))
    }
    @Test fun originalViewportOrSameRowsProveReturn() {
        assertTrue(DiscoveryViewportEvidence.returnedToParent("a", "a", emptyList(), emptyList(), "pusholder", "arya"))
        assertTrue(DiscoveryViewportEvidence.returnedToParent("a", "b", listOf("parent", "reply"), listOf("parent", "reply"), null, "arya"))
    }
    @Test fun movingMediaIsProgressEvenWithoutTextChanges() {
        val first = listOf(n(null, 20, 200, 600, 1100))
        assertNotEquals(DiscoveryViewportEvidence.signature(first), DiscoveryViewportEvidence.signature(listOf(n(null, 20, 100, 600, 1100))))
    }
    @Test fun hiddenOldPagesDoNotChangeViewport() {
        val visible = listOf(n("Gönderi", 100, 75))
        assertEquals(DiscoveryViewportEvidence.signature(visible), DiscoveryViewportEvidence.signature(visible + n("hidden", 100, 500).copy(visible = false)))
    }
    @Test fun absentQuotesWaitsForRepeatedStableReadsOrScanBudget() {
        assertFalse(DiscoveryViewportEvidence.quotesExhausted(8, 0))
        assertFalse(DiscoveryViewportEvidence.quotesExhausted(2, 2))
        assertTrue(DiscoveryViewportEvidence.quotesExhausted(3, 3))
        assertTrue(DiscoveryViewportEvidence.quotesExhausted(12, 0))
    }
    @Test fun engagementTitleWinsOverRetainedInlineReplySurface() {
        val nodes = detail() + n("Gönderi Etkileşimleri", 100, 30) + n("307 tarafından yeniden gönderildi", 100, 100).copy(selected = true)
        assertEquals(XScreen.ENGAGEMENT_LIST, ScreenDetector.detect(nodes))
        assertTrue(EngagementListEvidence.selected(nodes))
    }
    @Test fun hiddenEngagementTitleDoesNotStealTweetDetail() {
        assertEquals(XScreen.TWEET_DETAIL, ScreenDetector.detect(detail() + n("Geri", 20, 75) + n("Gönderi Etkileşimleri", 100, 30).copy(visible = false)))
    }
    @Test fun scaledHeaderUsesMeasuredBounds() {
        val scaled = detail().map { node -> node.copy(bounds = Rect().apply {
            left = node.bounds.left * 2; top = node.bounds.top * 2
            right = node.bounds.right * 2; bottom = node.bounds.bottom * 2
        }) }
        assertEquals(3, CommentDetailEvidence.actionIndex(scaled, "arya_krmzgl", VerifiedFollowPolicy.plainFollowLabels))
    }
}
