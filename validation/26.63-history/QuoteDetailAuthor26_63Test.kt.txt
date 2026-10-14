package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class QuoteDetailAuthor26_63Test {
    private val body = "Ana gönderinin yorum yazılacak gerçek metni, alt yorumlardan ayrı okunmalıdır."
    private val expected = DiscoveryTweetOpenRecovery.Attempt("post-key", "pusholder", body, 1, 0L)
    private fun n(text: String?, top: Int, bottom: Int = top + 40, left: Int = 20, right: Int = 680,
        id: String? = null, edit: Boolean = false, visible: Boolean = true) = NodeSnapshot(
        text, null, id?.let { "com.twitter.android:id/$it" }, null, false, true,
        Rect().apply { this.left = left; this.top = top; this.right = right; this.bottom = bottom },
        editable = edit, visible = visible,
    )
    private fun splitDetail() = listOf(
        n("Gönderi", 60, 110), n("@pusholder", 170, right = 290),
        n("7 dk", 170, left = 450), n(body, 260, id = "tweet_text"),
        n("08:30 · 07 Eki 26 · 24,4K Görüntüleme", 350),
        n("Alakalı", 410), n("Başka @other · 1 dk", 470),
        n("Alt yorum", 530, id = "tweet_text"), n("Yanıtını gönder", 900, id = "tweet_box", edit = true),
    )

    @Test fun clockAndFullDetailMetadataAreNotAParsedRelativeAge() {
        assertNull(XTweetInspector.parseAgeMinutes("08:30"))
        assertNull(XTweetInspector.parseAgeMinutes("08:30 · 07 Eki 26 · 24,4K Görüntüleme"))
        assertNull(TweetContentEvidence.header("Pusholder @pusholder"))
    }
    @Test fun theStrictFeedRuleReproducesTheTimedIndexRejectionAndDetailModeReadsTheAuthor() {
        val nodes = splitDetail()
        assertTrue(FeedRowEvidence.rows(nodes).any { it.headerIndex == 1 })
        assertNull(CommentDetailEvidence.header(nodes))
        assertEquals("pusholder", CommentDetailEvidence.header(nodes, excludeFeedTimedHeaders = false)?.handle)
    }
    @Test fun aSeparatedTimeOnTheMainAuthorLineDoesNotBlockTargetReadiness() {
        val nodes = splitDetail()
        assertEquals("pusholder", ReplyComposerEvidence.postHeader(nodes)?.handle)
        assertTrue(QuoteReplyFlowPolicy.detailReady(nodes, ScreenDetector.detect(nodes), expected))
        assertTrue(ReplyComposerEvidence.ready(nodes, XScreen.UNKNOWN))
    }
    @Test fun aMergedMainHandleOverlappingTheTimeStillUsesItsCompactHeaderChild() {
        val nodes = splitDetail().mapIndexed { i, node -> if (i == 1) node.copy(bounds = n(null, 160, 700).bounds) else node } +
            n("@pusholder", 215, 250, right = 290)
        assertTrue(FeedRowEvidence.rows(nodes).any { it.headerIndex == 1 })
        assertNull(CommentDetailEvidence.header(nodes))
        assertEquals(9, ReplyComposerEvidence.postHeader(nodes)?.index)
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, expected))
    }
    @Test fun aMergedMainHandleWithoutACompactChildCannotHideItsBody() {
        val nodes = splitDetail().mapIndexed { i, node -> if (i == 1) node.copy(bounds = n(null, 160, 700).bounds) else node }
        assertEquals(1, ReplyComposerEvidence.postHeader(nodes)?.index)
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, expected))
    }
    @Test fun aCompleteCombinedTimedMainHeaderIsAcceptedOnlyInsideTheMainPostBand() {
        val nodes = splitDetail().filterIndexed { i, _ -> i != 2 }.map {
            if (it.text == "@pusholder") it.copy(text = "Pusholder @pusholder · 7 dk") else it
        }
        assertNull(CommentDetailEvidence.header(nodes))
        assertEquals("pusholder", ReplyComposerEvidence.postHeader(nodes)?.handle)
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, expected))
    }
    @Test fun aTruncatedTimedHeaderCannotBecomeAnExactDetailIdentity() {
        val nodes = splitDetail().filterIndexed { i, _ -> i != 2 }.map {
            if (it.text == "@pusholder") it.copy(text = "Pusholder @pushol… · 7 dk") else it
        }
        assertNull(ReplyComposerEvidence.postHeader(nodes))
        assertFalse(ReplyComposerEvidence.matchesPost(nodes, expected))
    }
    @Test fun aWrongTimeBearingMainAuthorWithIdenticalBodyIsRejected() {
        val nodes = splitDetail().map { if (it.text == "@pusholder") it.copy(text = "@wrong") else it }
        assertEquals("wrong", ReplyComposerEvidence.postHeader(nodes)?.handle)
        assertFalse(QuoteReplyFlowPolicy.detailReady(nodes, XScreen.TWEET_DETAIL, expected))
        assertFalse(QuoteReplyFlowPolicy.sameThread(nodes, XScreen.TWEET_DETAIL, expected, expected.key))
    }
    @Test fun aDifferentPostByTheSameTimeBearingAuthorIsRejected() {
        assertFalse(ReplyComposerEvidence.matchesPost(splitDetail(), expected.copy(text = "Aynı yazarın başka gönderisi")))
    }
    @Test fun aMissingMainAuthorCannotBorrowALowerReplyWithTheSameAuthorAndText() {
        val nodes = splitDetail().filterIndexed { i, _ -> i !in setOf(1, 2) }.map {
            when (it.text) { "Başka @other · 1 dk" -> it.copy(text = "Pusholder @pusholder · 1 dk")
                "Alt yorum" -> it.copy(text = body)
                else -> it }
        }
        assertNull(ReplyComposerEvidence.postHeader(nodes))
        assertFalse(QuoteReplyFlowPolicy.detailReady(nodes, XScreen.TWEET_DETAIL, expected))
    }
    @Test fun metadataBoundsExcludeASplitLowerReplyEvenWhenBodyIdsAreAbsent() {
        val nodes = listOf(n("Gönderi", 60, 110), n(body, 260),
            n("08:30 · 07 Eki 26 · 24,4K Görüntüleme", 350),
            n("@pusholder", 470, right = 290), n("1 dk", 470, left = 450), n(body, 530))
        assertNull(ReplyComposerEvidence.postHeader(nodes))
        assertFalse(ReplyComposerEvidence.matchesPost(nodes, expected))
    }
    @Test fun anUnboundedTimedReplyIsStillRejectedWhenOnlyTheDetailTitleRemains() {
        val nodes = listOf(n("Gönderi", 60, 110), n("@pusholder", 470, right = 290), n("1 dk", 470, left = 450), n(body, 530))
        assertNull(CommentDetailEvidence.header(nodes, excludeFeedTimedHeaders = false))
        assertFalse(ReplyComposerEvidence.matchesPost(nodes, expected))
    }
    @Test fun aBodyMentionCannotReplaceTheCompactMainHeader() {
        val nodes = splitDetail().mapIndexed { i, node -> if (i == 1) node.copy(bounds = n(null, 160, 700).bounds) else node } +
            n("@pusholder", 300, 330, right = 280, id = "tweet_text")
        assertEquals(1, ReplyComposerEvidence.postHeader(nodes)?.index)
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, expected))
    }
    @Test fun theTimeBearingTargetAdvancesToWritingAndWaitsForTheCompleteCommentBeforeSubmitting() {
        val nodes = splitDetail()
        assertTrue(QuoteReplyFlowPolicy.detailReady(nodes, XScreen.TWEET_DETAIL, expected))
        assertEquals(QuoteReplyFlowPolicy.Decision.WRITE, QuoteReplyFlowPolicy.decide(
            XFlowStage.FILL_COMPOSER, ReplyComposerEvidence.ready(nodes, XScreen.TWEET_DETAIL),
            ReplyComposerEvidence.contains(nodes, "Gerçek yorum"), true, false, 0, 200))
        val filled = nodes.map { if (it.editable) it.copy(text = "Gerçek yorum") else it }
        assertEquals(QuoteReplyFlowPolicy.Decision.PREPARE_SUBMIT, QuoteReplyFlowPolicy.decide(
            XFlowStage.FILL_COMPOSER, true, ReplyComposerEvidence.contains(filled, "Gerçek yorum"), true, true, 1, 500))
        assertEquals(QuoteReplyFlowPolicy.Decision.WAIT, QuoteReplyFlowPolicy.decide(
            XFlowStage.SUBMIT_COMPOSER, true, false, true, true, 1, 500))
    }
}
