package com.atmacanext.app.automation

import android.graphics.Rect
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class QuoteReply26_61Test {
    private val body = "Hedef gönderinin uzun metni ilk ve ikinci paragrafta ayrı ayrı gösteriliyor."
    private val expected = DiscoveryTweetOpenRecovery.Attempt("post-key", "target", body, 1, 0L)
    private val comment = "Yazılacak gerçek yorum"
    private fun n(text: String?, y: Int, id: String? = null, edit: Boolean = false, click: Boolean = false,
        visible: Boolean = true, enabled: Boolean = true, x: Int = 40, right: Int = 1_000) = NodeSnapshot(
        text, null, id?.let { "com.twitter.android:id/$it" }, null, click, enabled,
        Rect().apply { left = x; top = y; this.right = right; bottom = y + 40 }, editable = edit, visible = visible)
    private fun detail() = listOf(n("Gönderi", 0), n("@target", 60), n(body, 120, "tweet_text"))
    private fun editor(text: String = "", edit: Boolean = true, visible: Boolean = true) = n(text, 800, "tweet_box", edit, visible = visible)
    private fun send(label: String = "Yanıtla", enabled: Boolean = true) = n(label, 0, enabled = enabled, x = 800)
    private fun d(stage: XFlowStage, nodes: List<NodeSnapshot>, attempts: Int = 0, elapsed: Long = 0,
        conflict: Boolean = false) = QuoteReplyFlowPolicy.decide(stage,
        ReplyComposerEvidence.ready(nodes, ScreenDetector.detect(nodes)), ReplyComposerEvidence.contains(nodes, comment),
        ReplyComposerEvidence.openIndex(nodes, true) != null, ReplyComposerEvidence.submitIndex(nodes) != null,
        attempts, elapsed, conflict)

    @Test fun theUnknownDetailSurfaceCanAdvanceWhenAuthorAndPostAreProven() {
        assertTrue(QuoteReplyFlowPolicy.detailReady(detail(), XScreen.UNKNOWN, expected))
    }
    @Test fun identicalContentOnTheWrongAuthorCannotOpenAReply() {
        assertFalse(QuoteReplyFlowPolicy.detailReady(detail().map { if (it.text == "@target") it.copy(text = "@other") else it }, XScreen.TWEET_DETAIL, expected))
    }
    @Test fun matchingTextOnAProfileIsNotAnOpenedDetail() {
        assertFalse(QuoteReplyFlowPolicy.detailReady(detail(), XScreen.PROFILE, expected))
    }
    @Test fun longBodyCanBeAssembledFromSeparateParagraphNodes() {
        val nodes = listOf(n("Gönderi", 0), n("@target", 60),
            n("Hedef gönderinin uzun metni", 120, "tweet_text"),
            n("ilk ve ikinci paragrafta ayrı ayrı gösteriliyor.", 170, "tweet_text"))
        assertTrue(ReplyComposerEvidence.matchesPost(nodes, expected))
    }
    @Test fun aLowerReplyCannotSupplyTheMissingParagraphOfTheOriginalPost() {
        val nodes = listOf(n("Gönderi", 0), n("@target", 60), n("Hedef gönderinin uzun metni", 120, "tweet_text"),
            n("Başka @other · 1 dk", 200), n("ilk ve ikinci paragrafta ayrı ayrı gösteriliyor.", 250, "tweet_text"))
        assertFalse(ReplyComposerEvidence.matchesPost(nodes, expected))
    }
    @Test fun knownComposeEditorCanWorkWithoutTheEditableFlag() {
        assertEquals(0, ReplyComposerEvidence.editorIndex(listOf(editor(edit = false))))
    }
    @Test fun theHiddenOldComposerCannotStealTheCurrentReplyText() {
        val nodes = listOf(editor("Eski", visible = false), editor(comment))
        assertEquals(1, ReplyComposerEvidence.editorIndex(nodes))
        assertTrue(ReplyComposerEvidence.contains(nodes, comment))
    }
    @Test fun aNonClickableSendLabelIsStillAValidMeasuredButtonTarget() {
        assertEquals(1, ReplyComposerEvidence.submitIndex(listOf(editor(comment), send())))
    }
    @Test fun turkishAndEnglishButtonRoleSuffixesAreAccepted() {
        for (label in listOf("Yanıtla, Düğme", "Reply. Button", "Gönder, Etkin"))
            assertNotNull(label, ReplyComposerEvidence.submitIndex(listOf(editor(comment), send(label))))
    }
    @Test fun disabledSubmitAndUnrelatedSentenceNeverBecomeSendControls() {
        assertNull(ReplyComposerEvidence.submitIndex(listOf(editor(comment), send(enabled = false))))
        assertNull(ReplyComposerEvidence.submitIndex(listOf(editor(comment), send("Yanıtla benim mesajımı"))))
    }
    @Test fun aReplyToolbarBelowThePostCannotBeUsedAsSubmit() {
        assertNull(ReplyComposerEvidence.submitIndex(detail() + editor(comment) + n("Yanıtla", 400, "toolbar_reply", x = 800)))
    }
    @Test fun anUnidentifiedLowerReplyButtonDoesNotBecomeTheTopSendButton() {
        assertNull(ReplyComposerEvidence.submitIndex(detail() + editor(comment) + n("Yanıtla", 400, x = 800)))
    }
    @Test fun aShortExplicitTweetBodyRemainsReadable() {
        val nodes = listOf(n("@target", 0), n("İyi", 60, "tweet_text"))
        assertEquals(1, TweetContentEvidence.bodyIndex(nodes, 40))
    }
    @Test fun aRealMultiFrameInlineReplyReachesSubmitOnlyAfterFreshTextIsRead() {
        val open = detail() + n("Yanıtını gönder", 800)
        assertEquals(QuoteReplyFlowPolicy.Decision.OPEN, d(XFlowStage.OPEN_COMPOSER, open))
        val empty = detail() + editor()
        assertEquals(QuoteReplyFlowPolicy.Decision.FILL, d(XFlowStage.OPEN_COMPOSER, empty))
        assertEquals(QuoteReplyFlowPolicy.Decision.WRITE, d(XFlowStage.FILL_COMPOSER, empty))
        val filled = detail() + editor(comment) + send()
        assertEquals(QuoteReplyFlowPolicy.Decision.PREPARE_SUBMIT, d(XFlowStage.FILL_COMPOSER, filled))
        assertEquals(QuoteReplyFlowPolicy.Decision.SUBMIT, d(XFlowStage.SUBMIT_COMPOSER, filled))
    }
    @Test fun anAcceptedWriteWithoutTheActualTextDoesNotAuthorizeSubmission() {
        assertEquals(QuoteReplyFlowPolicy.Decision.WRITE, d(XFlowStage.FILL_COMPOSER, detail() + editor("Farklı")))
        assertEquals(QuoteReplyFlowPolicy.Decision.WAIT, d(XFlowStage.SUBMIT_COMPOSER, detail() + editor() + send()))
    }
    @Test fun aMissingReplyControlWaitsUntilTheSameTargetRestartDeadline() {
        assertEquals(QuoteReplyFlowPolicy.Decision.WAIT, d(XFlowStage.OPEN_COMPOSER, detail(), elapsed = 9_999))
        assertEquals(QuoteReplyFlowPolicy.Decision.RESTART, d(XFlowStage.OPEN_COMPOSER, detail(), elapsed = 10_000))
    }
    @Test fun aDisabledSendRecoversInsteadOfDroppingTheSelectedPost() {
        val nodes = detail() + editor(comment) + send(enabled = false)
        assertEquals(QuoteReplyFlowPolicy.Decision.WAIT, d(XFlowStage.SUBMIT_COMPOSER, nodes, elapsed = 5_000))
        assertEquals(QuoteReplyFlowPolicy.Decision.RESTART, d(XFlowStage.SUBMIT_COMPOSER, nodes, elapsed = 10_000))
    }
    @Test fun freshProgressAtTheDeadlineWinsOverAnAutomaticRestart() {
        assertEquals(QuoteReplyFlowPolicy.Decision.FILL, d(XFlowStage.OPEN_COMPOSER, detail() + editor(), elapsed = 10_000))
    }
    @Test fun targetConflictNeverAuthorizesAWriteEvenWithAnEditor() {
        assertEquals(QuoteReplyFlowPolicy.Decision.WAIT, d(XFlowStage.FILL_COMPOSER, detail() + editor(), conflict = true))
    }
    @Test fun simplyOpeningAQuotePostDoesNotMarkItAsProcessed() {
        assertFalse(QuoteReplyFlowPolicy.openedIsProcessed(TaskType.COMMENT_QUOTE_TARGETS))
        assertTrue(QuoteReplyFlowPolicy.openedIsProcessed(TaskType.COMMENTER_FOLLOW))
    }
    @Test fun sentNoticesSupportTheViewLinkButNeverTheTweetBody() {
        assertTrue(ReplyComposerEvidence.hasSentNotice(listOf(n("Your post was sent. View", 900, "snackbar_text"))))
        assertTrue(ReplyComposerEvidence.hasSentNotice(listOf(n("Gönderin gönderildi. Görüntüle", 900, "snackbar_text"))))
        assertFalse(ReplyComposerEvidence.hasSentNotice(listOf(n("Gönderin gönderildi", 120, "tweet_text"))))
    }
    @Test fun clearingTheEditorAloneIsNotAConfirmedReply() {
        val nodes = detail() + editor()
        assertTrue(ReplyComposerEvidence.inlineCleared(nodes, comment))
        assertFalse(QuoteReplyFlowPolicy.newReplyVisible(nodes, XScreen.TWEET_DETAIL, expected, expected.key, "own", comment, emptySet()))
    }
    private fun ownReply() = listOf(n("Ben @own · 1 dk", 350), n(comment, 410, "tweet_text"))
    @Test fun aNewOwnReplyInTheOpenedThreadIsConfirmed() {
        assertTrue(QuoteReplyFlowPolicy.newReplyVisible(detail() + ownReply(), XScreen.TWEET_DETAIL, expected, expected.key, "own", comment, emptySet()))
    }
    @Test fun anOwnReplyThatExistedBeforeSendingIsNotCountedTwice() {
        val nodes = detail() + ownReply()
        val before = ReplyComposerEvidence.ownReplyKeys(nodes, "own", comment)
        assertFalse(QuoteReplyFlowPolicy.newReplyVisible(nodes, XScreen.TWEET_DETAIL, expected, expected.key, "own", comment, before))
    }
    @Test fun theOwnReplyCanBeReadAfterItsParentHasScrolledAboveTheViewport() {
        assertTrue(QuoteReplyFlowPolicy.newReplyVisible(ownReply(), XScreen.TWEET_DETAIL, expected, expected.key, "own", comment, emptySet()))
        assertFalse(QuoteReplyFlowPolicy.newReplyVisible(ownReply(), XScreen.PROFILE, expected, expected.key, "own", comment, emptySet()))
    }
    @Test fun aDifferentPendingPostCannotBorrowThisRepliesProof() {
        assertFalse(QuoteReplyFlowPolicy.newReplyVisible(detail() + ownReply(), XScreen.TWEET_DETAIL, expected, "other-key", "own", comment, emptySet()))
    }
    private fun field(name: String) = AutomationController.javaClass.getDeclaredField(name).apply { isAccessible = true }
    private fun reservation(issued: Boolean) {
        AutomationController.stop()
        assertTrue(AutomationController.start(ScheduledTask("quote", "1", "own", type = TaskType.COMMENT_QUOTE_TARGETS,
            contentText = comment, limit = 1, quoteTargets = "target")))
        @Suppress("UNCHECKED_CAST")
        val state = field("_state").get(AutomationController) as MutableStateFlow<AutomationRuntimeState>
        state.value = state.value.copy(status = RuntimeStatus.PAUSED, flowStage = XFlowStage.SUBMIT_COMPOSER, quotePendingKey = expected.key)
        field("quoteReplyPost").set(AutomationController, expected)
        field("quoteCheckpointToken").set(AutomationController, state.value.sessionId + ":" + expected.key)
        field("quoteSubmitIssued").setBoolean(AutomationController, issued)
    }
    @Test fun aManuallyPausedPreSubmitReservationCanResumeWithoutResettingTheTask() {
        try {
            reservation(false)
            val session = AutomationController.state.value.sessionId
            AutomationController.resume()
            assertEquals(RuntimeStatus.RUNNING, AutomationController.state.value.status)
            assertEquals(session, AutomationController.state.value.sessionId)
            assertEquals(expected.key, AutomationController.state.value.quotePendingKey)
        } finally { AutomationController.stop() }
    }
    @Test fun losingAPendingActionAfterAnIssuedTapDoesNotAllowResubmission() {
        try {
            reservation(true)
            AutomationController.resume()
            assertEquals(RuntimeStatus.PAUSED, AutomationController.state.value.status)
            assertEquals(0, AutomationController.state.value.verifiedCount)
        } finally { AutomationController.stop() }
    }
    @Test fun theRealSuccessPathPersistsThePostKeyAndCompletesOnlyTheConfirmedQuota() {
        try {
            reservation(true)
            @Suppress("UNCHECKED_CAST")
            val state = field("_state").get(AutomationController) as MutableStateFlow<AutomationRuntimeState>
            state.value = state.value.copy(status = RuntimeStatus.VERIFYING)
            field("discoveryTweetKey").set(AutomationController, expected.key)
            AutomationController.javaClass.getDeclaredMethod("recordSuccess", AtmacaAccessibilityService::class.java,
                String::class.java).apply { isAccessible = true }.invoke(AutomationController, AtmacaAccessibilityService(), expected.key)
            assertEquals(RuntimeStatus.COMPLETED, state.value.status)
            assertEquals(1, state.value.verifiedCount)
            assertEquals(expected.key, state.value.quotePostedKeys)
            assertNull(state.value.quotePendingKey)
        } finally { AutomationController.stop() }
    }
}
