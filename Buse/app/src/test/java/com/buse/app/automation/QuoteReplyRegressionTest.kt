package com.buse.app.automation

import android.graphics.Rect
import com.buse.app.domain.model.ScheduledTask
import com.buse.app.domain.model.TaskType
import org.junit.Assert.*
import org.junit.Test

class QuoteReplyRegressionTest {
    private val body = "Hedef profilin yorum yapılacak gerçek gönderisi, farklı yorum satırlarından ayrı okunur."
    private val attempt = DiscoveryTweetOpenRecovery.Attempt("post-key", "hedef", body, 1, 0L)
    private fun n(text: String?, y: Int, id: String? = null, edit: Boolean = false, visible: Boolean = true,
        enabled: Boolean = true, x: Int = 40, right: Int = 1000, clazz: String? = null) = NodeSnapshot(
        text, null, id?.let { "com.twitter.android:id/" + it }, clazz,
        clickable = true, enabled = enabled, bounds = Rect().apply { left = x; top = y; this.right = right; bottom = y + 50 },
        editable = edit, visible = visible,
    )
    private fun detail() = listOf(n("Gönderi", 0), n("@hedef", 70), n(body, 140), n("Yanıtla", 260, "toolbar_reply", x = 40, right = 90))
    private fun editor(text: String? = "Yanıtını gönder", enabled: Boolean = true) = n(text, 850, "tweet_box", edit = true, enabled = enabled)
    private fun send(enabled: Boolean = true) = n("Yanıtla", 20, "tweet_button", enabled = enabled, x = 800, right = 1040)

    @Test fun inlineReplyOnTweetDetailIsReadyWithoutWaitingForSeparateComposer() {
        val nodes = detail() + editor()
        assertEquals(XScreen.TWEET_DETAIL, ScreenDetector.detect(nodes))
        assertTrue(ReplyComposerEvidence.ready(nodes, XScreen.TWEET_DETAIL))
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, attempt))
    }
    @Test fun staticReplyEntryOpensWithoutBroadReplyIdMatch() {
        val nodes = listOf(n("Gönderi", 0), n("Yanıtını gönder", 850), n("Yanıtın alt metni", 400, "reply_container"))
        assertEquals(1, ReplyComposerEvidence.openIndex(nodes))
    }
    @Test fun originalToolbarIsPreferredOverLowerReplyToolbarAndRetryUsesInlineEntry() {
        val nodes = detail() + n("Reply", 500, "toolbar_reply") + n("Yanıtını gönder", 850)
        assertEquals(3, ReplyComposerEvidence.openIndex(nodes))
        assertEquals(5, ReplyComposerEvidence.openIndex(nodes, retry = true))
    }
    @Test fun hiddenCachedEditorCannotReceiveText() {
        val nodes = listOf(n("old", 50, "tweet_box", edit = true, visible = false), editor())
        assertEquals(1, ReplyComposerEvidence.editorIndex(nodes))
        assertFalse(ReplyComposerEvidence.contains(nodes, "old"))
    }
    @Test fun multipleVisibleEditorsAreAmbiguous() {
        assertNull(ReplyComposerEvidence.editorIndex(listOf(editor(), n("other", 200, "post_text", edit = true))))
    }
    @Test fun editTextClassSupportsLayoutsWithoutEditableFlag() {
        assertTrue(ReplyComposerEvidence.ready(listOf(n("", 250, "tweet_box", clazz = "android.widget.EditText")), XScreen.COMPOSER))
    }
    @Test fun writeAcceptanceDoesNotVerifyDifferentOrEmptyText() {
        assertFalse(ReplyComposerEvidence.contains(listOf(editor("")), "Yorum"))
        assertFalse(ReplyComposerEvidence.contains(listOf(editor("Yorum farklı")), "Yorum"))
        assertTrue(ReplyComposerEvidence.contains(listOf(editor("Yorum\nmetni")), "Yorum metni"))
    }
    @Test fun submitNeverClicksTweetOrLowerReplyToolbar() {
        val nodes = detail() + editor("Yorum") + send()
        assertEquals(5, ReplyComposerEvidence.submitIndex(nodes))
        assertNull(ReplyComposerEvidence.submitIndex(detail() + editor("Yorum")))
    }
    @Test fun disabledSendDoesNotAuthorizeSubmission() {
        assertNull(ReplyComposerEvidence.submitIndex(detail() + editor("Yorum") + send(false)))
    }
    @Test fun visibleUnsentInlineTextIsNotSuccessfulJustBecauseScreenIsTweetDetail() {
        assertFalse(ReplyComposerEvidence.inlineCleared(detail() + editor("Yorum") + send(), "Yorum"))
        assertTrue(ReplyComposerEvidence.inlineCleared(detail() + editor("") + send(false), "Yorum"))
    }
    @Test fun originalUntimedAuthorIsNotConfusedWithLowerTimedReplyAuthor() {
        val nodes = detail() + n("Başka @baska · 1 dk", 450) + n("Alt yorum", 520) + editor()
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, attempt))
    }
    @Test fun wrongOpenedAuthorWithSameTextIsRejected() {
        val nodes = listOf(n("Gönderi", 0), n("@baska", 70), n(body, 140), editor())
        assertFalse(ReplyComposerEvidence.matchesPost(nodes, attempt))
    }
    @Test fun changedPostBySameAuthorIsRejected() {
        assertFalse(ReplyComposerEvidence.matchesPost(detail(), attempt.copy(text = "Farklı bir gönderi")))
    }
    @Test fun latestYoungPostCanRetryOnlyInQuoteTask() {
        val candidate = DiscoveryTweetOpenRecovery.Candidate("post-key", "hedef", body, 2L)
        assertFalse(DiscoveryTweetOpenRecovery.matches(attempt, candidate))
        assertTrue(DiscoveryTweetOpenRecovery.matches(attempt, candidate, requireOlderPost = false))
        assertFalse(DiscoveryTweetOpenRecovery.matches(attempt, candidate.copy(author = "baska"), requireOlderPost = false))
    }
    @Test fun limitOneAppliesToEveryTargetAndNotJustFirstTarget() {
        assertEquals(5, QuoteTargetProgressPolicy.perCycle(1, 5))
        assertEquals(1, QuoteTargetProgressPolicy.targetIndex(1, 1, 5))
        assertTrue(QuoteTargetProgressPolicy.completedTarget(1, 0, 0, 1))
        assertFalse(QuoteTargetProgressPolicy.completedTarget(1, 0, 1, 1))
        assertEquals(0, QuoteTargetProgressPolicy.targetIndex(5, 1, 5))
    }
    @Test fun partialResumeUsesCorrectTargetWithinCycle() {
        assertEquals(2, QuoteTargetProgressPolicy.targetIndex(8, 3, 5))
        assertEquals(1, QuoteTargetProgressPolicy.targetIndex(19, 3, 5))
        assertFalse(QuoteTargetProgressPolicy.completedTarget(19, 15, 1, 3))
    }
    @Test fun runtimeUsesAllTargetsAndCarriesConfirmedPostKeysOnPause() {
        val controller = AutomationController
        controller.stop()
        try {
            val task = ScheduledTask("quote", "1", "hesabim", type = TaskType.COMMENT_QUOTE_TARGETS,
                limit = 1, repeatCount = 2, progress = 1, contentText = "Yorum",
                quoteTargets = "hedef\nhedef2", quotePostedKeys = "done-key")
            assertTrue(controller.start(task))
            assertEquals(4, controller.state.value.limit)
            assertEquals(2, controller.state.value.perCycleLimit)
            val index = controller.javaClass.getDeclaredField("discoveryTargetIndex").apply { isAccessible = true }.get(controller)
            assertEquals(1, index)
            assertEquals("done-key", controller.state.value.quotePostedKeys)
        } finally { controller.stop() }
    }
    @Test fun unresolvedPreviousSubmissionPausesAndCannotBeSilentlyResubmitted() {
        val controller = AutomationController
        controller.stop()
        try {
            val task = ScheduledTask("uncertain", "1", "hesabim", type = TaskType.COMMENT_QUOTE_TARGETS,
                limit = 1, contentText = "Yorum", quoteTargets = "hedef", quotePendingKey = "pending-key")
            assertTrue(controller.start(task))
            assertEquals(RuntimeStatus.PAUSED, controller.state.value.status)
            controller.resume()
            assertEquals(RuntimeStatus.PAUSED, controller.state.value.status)
            assertEquals(0, controller.state.value.verifiedCount)
        } finally { controller.stop() }
    }
}
