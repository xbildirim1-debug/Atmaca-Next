package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class TweetEngagement27_71Test {
    private fun n(text: String?, x: Int, y: Int, id: String? = null, done: Boolean = false, clickable: Boolean = true) =
        NodeSnapshot(text, null, id, "Button", clickable, true,
            Rect().apply { left = x; top = y; right = x + 60; bottom = y + 40 }, selected = done)
    private fun bar(y: Int = 460, liked: Boolean = false, reposted: Boolean = false, saved: Boolean = false) = listOf(
        n("Yeniden gönder", 180, y, "toolbar_retweet", reposted), n("Beğen", 330, y, "toolbar_like", liked), n("Kaydet", 490, y, "bookmark", saved))
    private fun sheet(undo: Boolean = false) = bar() + listOf(n(if (undo) "Yeniden göndermeyi geri al" else "Yeniden gönder", 98, 1260, clickable = false), n("Alıntı", 98, 1360, clickable = false))
    private fun control(nodes: List<NodeSnapshot>, a: TweetEngagementEvidence.Action) = TweetEngagementEvidence.control(nodes, a)!!
    private fun post(key: String, age: Long?, author: String? = "peer", pinned: Boolean = false, text: String = "Metin $key") =
        AccountEngagementTweetPolicy.Post(key, author, text, age, pinned)

    @Test fun screenshotUsesLikeRepostAndBookmarkControlsOnTheSameRow() {
        val b = bar(); assertEquals(1, control(b, TweetEngagementEvidence.Action.LIKE).index)
        assertEquals(0, control(b, TweetEngagementEvidence.Action.REPOST).index); assertEquals(2, control(b, TweetEngagementEvidence.Action.BOOKMARK).index)
    }
    @Test fun checkedLikeIsRecognizedWithoutTogglingItOff() { assertTrue(control(bar(liked = true), TweetEngagementEvidence.Action.LIKE).done) }
    @Test fun checkedRepostIsRecognizedWithoutUndo() { assertTrue(control(bar(reposted = true), TweetEngagementEvidence.Action.REPOST).done) }
    @Test fun savedBookmarkIsRecognizedWithoutRemovingIt() { assertTrue(control(bar(saved = true), TweetEngagementEvidence.Action.BOOKMARK).done) }
    @Test fun selectedReplyControlsCannotClaimTheMainPostIsDone() {
        val b = bar() + bar(900, true, true, true)
        TweetEngagementEvidence.Action.entries.forEach { assertFalse(control(b, it).done) }
    }
    @Test fun hiddenToolbarDoesNotStealTheVisibleMainPost() {
        val b = bar(200, true, true, true).map { it.copy(visible = false) } + bar()
        TweetEngagementEvidence.Action.entries.forEach { assertFalse(control(b, it).done) }
    }
    @Test fun disabledControlsAreNotClicked() { assertNull(TweetEngagementEvidence.control(bar().map { it.copy(enabled = false) }, TweetEngagementEvidence.Action.LIKE)) }
    @Test fun aSingleAmbiguousActionIsNotARealToolbar() { assertNull(TweetEngagementEvidence.control(listOf(n("Beğen", 330, 460)), TweetEngagementEvidence.Action.LIKE)) }
    @Test fun plainTextIsNotAnAction() {
        val b = listOf(n("Beğen", 0, 200, clickable = false).copy(className = "TextView")) + bar()
        assertEquals(2, control(b, TweetEngagementEvidence.Action.LIKE).index)
    }
    @Test fun statisticsAreNotTheActionRow() {
        val b = listOf(n("12", 100, 400, "retweet_count").copy(clickable = false)) + bar()
        assertEquals(2, control(b, TweetEngagementEvidence.Action.LIKE).index)
    }
    @Test fun labelsCanConfirmAlreadyDoneWithoutSelectedFlags() {
        val b = listOf(n("Yeniden gönderildi", 180, 460), n("Beğeniyi geri al", 330, 460), n("Yer işaretlerinden kaldır", 490, 460))
        TweetEngagementEvidence.Action.entries.forEach { assertTrue(control(b, it).done) }
    }
    @Test fun countsAndContentDescriptionsRemainActionEvidence() {
        val b = bar().map { it.copy(text = "0", contentDescription = it.text) }
        assertEquals(1, control(b, TweetEngagementEvidence.Action.LIKE).index)
    }
    @Test fun screenshotAlintiSheetSelectsYenidenGonderRatherThanQuoteOrToolbar() {
        assertTrue(TweetEngagementEvidence.repostSheet(sheet())); assertEquals(3, TweetEngagementEvidence.repostConfirmation(sheet()))
    }
    @Test fun oldSheetClassifierAlsoRecognizesTheScreenshotAlintiLabel() {
        assertTrue(DiscoveryScrollGesturePolicy.repostSheet(sheet().map { it.copy(clickable = true) }))
    }
    @Test fun undoMenuNeverProvidesAPositiveRepostConfirmation() {
        assertTrue(TweetEngagementEvidence.undoRepostSheet(sheet(true))); assertNull(TweetEngagementEvidence.repostConfirmation(sheet(true)))
    }
    @Test fun toolbarAloneCannotBeMistakenForTheConfirmationMenu() { assertFalse(TweetEngagementEvidence.repostSheet(bar())); assertNull(TweetEngagementEvidence.repostConfirmation(bar())) }
    @Test fun quoteTextInAnUnalignedReplyDoesNotBecomeARepostMenu() {
        assertFalse(TweetEngagementEvidence.repostSheet(bar() + n("Alıntı", 20, 900, clickable = false)))
    }
    @Test fun hiddenSheetIsNotClicked() { assertNull(TweetEngagementEvidence.repostConfirmation(sheet().map { if (it.bounds.top > 1000) it.copy(visible = false) else it })) }
    @Test fun englishQuoteMenuIsSupported() {
        assertEquals(0, TweetEngagementEvidence.repostConfirmation(listOf(n("Repost", 98, 1260), n("Quote", 98, 1360))))
    }
    @Test fun menuSelectionScalesWithWindowGeometry() {
        val scaled = sheet().map { it.copy(bounds = Rect().apply { left = it.bounds.left * 2; top = it.bounds.top * 2; right = it.bounds.right * 2; bottom = it.bounds.bottom * 2 }) }
        assertEquals(3, TweetEngagementEvidence.repostConfirmation(scaled))
    }
    @Test fun latestOwnPostHasNoTwoHourDiscoveryDelay() { assertEquals("fresh", AccountEngagementTweetPolicy.latest(listOf(post("old", 150), post("fresh", 0)), "peer")!!.key) }
    @Test fun anOldPinnedPostDoesNotOverrideTheNewOriginalPost() {
        assertEquals("new", AccountEngagementTweetPolicy.latest(listOf(post("pinned", 900, pinned = true), post("new", 1)), "peer")!!.key)
    }
    @Test fun aPinnedPostIsUsedIfItsTimestampReallyIsNewest() {
        assertEquals("pinned", AccountEngagementTweetPolicy.latest(listOf(post("pinned", 0, pinned = true), post("new", 1)), "peer")!!.key)
    }
    @Test fun republishedPostsFromOtherAuthorsAreExcluded() { assertEquals("own", AccountEngagementTweetPolicy.latest(listOf(post("other", 0, "other"), post("own", 10)), "peer")!!.key) }
    @Test fun unknownAuthorsTimesAndEmptyTextCannotBeGuessed() {
        assertNull(AccountEngagementTweetPolicy.latest(listOf(post("a", null), post("b", 1, null), post("c", -1), post("d", 1, text = "")), "peer"))
    }
    @Test fun profileAndExternalLinksAreRejectedForDirectEngagement() {
        for (u in listOf("https://x.com/peer", "https://x.com.evil.test/peer/status/123", "https://example.com/peer/status/123", "file:///peer/status/123"))
            assertFalse(u, AccountEngagementTweetPolicy.validTweetUrl(u))
    }
    @Test fun statusLinksWithShareParametersAreAccepted() {
        assertTrue(AccountEngagementTweetPolicy.validTweetUrl("https://x.com/peer/status/123456?s=20"))
        assertTrue(AccountEngagementTweetPolicy.validTweetUrl("https://twitter.com/i/status/123456"))
    }
    @Test fun linkedTargetRequiresTheOpenedPostAuthor() {
        val nodes = listOf(n("Gönderi", 0, 80), n("@peer", 100, 200, clickable = false).copy(className = "TextView")) + bar()
        assertTrue(AccountEngagementTweetPolicy.linkedTarget(nodes, "https://x.com/peer/status/123"))
        assertFalse(AccountEngagementTweetPolicy.linkedTarget(nodes, "https://x.com/other/status/123"))
    }
}
