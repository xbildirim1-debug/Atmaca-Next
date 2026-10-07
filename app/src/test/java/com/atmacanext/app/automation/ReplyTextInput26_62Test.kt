package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class ReplyTextInput26_62Test {
    private val comment = "Bu gerçek yorumdur.\nTürkçe: ıİğş 👋"
    private class Connection(var value: String = "", var readable: Boolean = true) : ReplyTextTransfer.Connection {
        var replacements = 0
        var accepted = true
        override fun readFullText() = value.takeIf { readable }
        override fun replaceAll(previous: String, value: String): Boolean {
            if (!accepted || this.value != previous) return false
            this.value = value
            replacements++
            return true
        }
    }
    private class Editor(override var focused: Boolean = true, override var text: String = "",
        override var connection: ReplyTextTransfer.Connection? = null) : ReplyTextTransfer.Editor {
        var focusCalls = 0
        var setCalls = 0
        var pasteCalls = 0
        var setAcknowledged = false
        var setEffective = false
        var pasteAllowed = true
        override fun focus(): Boolean { focusCalls++; return true }
        override fun setText(value: String): Boolean {
            setCalls++
            if (setEffective) text = value
            return setAcknowledged
        }
        override fun pasteReplacing(value: String): Boolean {
            pasteCalls++
            if (!pasteAllowed) return false
            text = value
            return true
        }
    }
    @Test fun aPhysicalFocusRequestDoesNotWriteIntoAnUnfocusedField() {
        val editor = Editor(focused = false)
        assertEquals(ReplyTextTransfer.Result.FOCUS_REQUESTED, ReplyTextTransfer.write(editor, comment, 1))
        assertEquals(1, editor.focusCalls)
        assertEquals(0, editor.setCalls + editor.pasteCalls)
    }
    @Test fun focusThenNativeInputTransfersTheWholeTurkishComment() {
        val input = Connection()
        val editor = Editor(focused = false, connection = input)
        assertEquals(ReplyTextTransfer.Result.FOCUS_REQUESTED, ReplyTextTransfer.write(editor, comment, 1))
        editor.focused = true
        assertEquals(ReplyTextTransfer.Result.INPUT_CONNECTION, ReplyTextTransfer.write(editor, comment, 1))
        assertEquals(comment, input.value)
        assertEquals(0, editor.setCalls + editor.pasteCalls)
    }
    @Test fun nativeInputReplacesADraftRatherThanAppendingTheComment() {
        val input = Connection("Eski taslak 👋")
        assertEquals(ReplyTextTransfer.Result.INPUT_CONNECTION, ReplyTextTransfer.write(Editor(connection = input), comment, 1))
        assertEquals(comment, input.value)
    }
    @Test fun aSecondFreshFrameCannotAppendTheSameNativeTextTwice() {
        val input = Connection()
        val editor = Editor(connection = input)
        ReplyTextTransfer.write(editor, comment, 1)
        assertEquals(ReplyTextTransfer.Result.ALREADY_PRESENT, ReplyTextTransfer.write(editor, comment, 2))
        assertEquals(1, input.replacements)
    }
    @Test fun acceptedButIneffectiveSetTextExercisesPasteOnTheNextAttempt() {
        val editor = Editor().apply { setAcknowledged = true }
        assertEquals(ReplyTextTransfer.Result.SET_TEXT, ReplyTextTransfer.write(editor, comment, 1))
        assertEquals("", editor.text)
        assertEquals(ReplyTextTransfer.Result.PASTE, ReplyTextTransfer.write(editor, comment, 2))
        assertEquals(comment, editor.text)
        assertEquals(1, editor.setCalls)
    }
    @Test fun unsupportedSetTextCanPasteWithoutAnAdvertisedPasteAction() {
        val editor = Editor()
        assertEquals(ReplyTextTransfer.Result.PASTE, ReplyTextTransfer.write(editor, comment, 1))
        assertEquals(comment, editor.text)
    }
    @Test fun anIncompleteNativeReadDoesNotAuthorizeNativeReplacement() {
        val input = Connection("Draft", readable = false)
        val editor = Editor(connection = input).apply { pasteAllowed = false }
        assertEquals(ReplyTextTransfer.Result.WAIT, ReplyTextTransfer.write(editor, comment, 1))
        assertEquals(0, input.replacements)
    }
    @Test fun aRejectedNativeWriteCanUseTheFocusedAccessibilityFallback() {
        val input = Connection().apply { accepted = false }
        assertEquals(ReplyTextTransfer.Result.PASTE, ReplyTextTransfer.write(Editor(connection = input), comment, 1))
    }
    @Test fun blankContentCannotFocusOrModifyAnEditor() {
        val editor = Editor(focused = false)
        assertEquals(ReplyTextTransfer.Result.WAIT, ReplyTextTransfer.write(editor, " ", 1))
        assertEquals(0, editor.focusCalls + editor.setCalls + editor.pasteCalls)
    }
    @Test fun anExistingExactCommentDoesNotDispatchAnotherWrite() {
        val editor = Editor(text = comment)
        assertEquals(ReplyTextTransfer.Result.ALREADY_PRESENT, ReplyTextTransfer.write(editor, comment, 1))
        assertEquals(0, editor.focusCalls + editor.setCalls + editor.pasteCalls)
    }
    @Test fun inputConnectionRequiresTheFocusedXTextEditor() {
        assertTrue(ReplyTextTransfer.permitsConnection("com.twitter.android", 0x20001, true))
        assertFalse(ReplyTextTransfer.permitsConnection("com.twitter.android", 1, false))
        assertFalse(ReplyTextTransfer.permitsConnection("com.atmacanext.v258", 1, true))
        assertFalse(ReplyTextTransfer.permitsConnection("com.twitter.android", 2, true))
    }
    @Test fun passwordEditorsNeverReceiveTheCommentThroughAnInputConnection() {
        for (type in listOf(0x81, 0x91, 0xE1))
            assertFalse(ReplyTextTransfer.permitsConnection("com.twitter.android", type, true))
    }
    @Test fun anAcknowledgedWriteIsNotTextVerification() {
        assertFalse(ReplyTextTransfer.matches("", comment))
        assertFalse(ReplyTextTransfer.matches("Bu gerçek", comment))
        assertFalse(ReplyTextTransfer.matches(null, comment))
        assertTrue(ReplyTextTransfer.matches(comment, comment))
    }
    private fun n(text: String?, top: Int, bottom: Int = top + 42, left: Int = 10, right: Int = 680,
        id: String? = null, editable: Boolean = false, visible: Boolean = true, desc: String? = null) = NodeSnapshot(
        text, desc, id?.let { "com.twitter.android:id/$it" }, null, false, true,
        Rect().apply { this.left = left; this.top = top; this.right = right; this.bottom = bottom }, editable = editable, visible = visible)
    private val body = "Finansal Kurumlar Birliği Başkanı ve Türkiye Basketbol Federasyonu Yedek Üyesi Ali Emre Ballı, İstanbul'da boş arazide park halindeki aracının içinde ölü bulundu."
    // Representative semantic layout reconstructed from the user's photo; not a device tree dump.
    private fun photo() = listOf(n("Gönderi", 60, 114), n("Pushholder @pushholder", 166, 1_340),
        n("@pushholder", 212, 246, right = 265), n(body, 258, 406, id = "tweet_text"),
        n("- Ballı'nın daha önce fon soruşturmasında adının geçtiği belirtildi. (A Haber)", 441, 511, id = "tweet_text"),
        n(null, 542, 1_222, id = "tweet_image"), n("19:18 · 06 Eki 26 · 61,3K Görüntüleme", 1_250),
        n("18 Yanıt", 1_316, left = 12, right = 75, id = "toolbar_reply"),
        n("120 Beğeni", 1_316, left = 341, right = 415, id = "toolbar_like"),
        n("Yanıtını gönder", 1_385, 1_432, left = 85, right = 410),
        n("GIF", 1_385, left = 540, right = 580), n("Görsel ekle", 1_385, left = 433, right = 480))
    @Test fun thePhotoLayoutOpensTheBottomReplyFieldBeforeTheReplyCountToolbar() {
        val nodes = photo()
        assertEquals(9, ReplyComposerEvidence.openIndex(nodes))
        assertEquals(7, ReplyComposerEvidence.openIndex(nodes, retry = true))
        assertEquals(9, ReplyComposerEvidence.focusIndex(nodes))
    }
    @Test fun aLargeMergedAuthorParentCannotHideTheActualPostText() {
        val nodes = photo()
        assertEquals(2, ReplyComposerEvidence.postHeader(nodes)?.index)
        val expected = DiscoveryTweetOpenRecovery.Attempt("pushholder-key", "pushholder", body, 1, 0)
        assertTrue(QuoteReplyFlowPolicy.detailReady(nodes, ScreenDetector.detect(nodes), expected))
    }
    @Test fun aLowerSameAuthorReplyCannotBecomeTheCompactOpenedHeader() {
        val nodes = photo().filterIndexed { i, _ -> i != 2 } + n("@pushholder", 600, 640, right = 200)
        assertEquals(1, ReplyComposerEvidence.postHeader(nodes)?.index)
    }
    @Test fun thePhotoLayoutWithAReplyFieldRoleStillWinsOverBackgroundProfileTabs() {
        val nodes = photo().map { if (it.text == "Yanıtını gönder") it.copy(text = "Yanıtını gönder, Metin alanı") else it } +
            listOf(n("Gönderiler", 1_020), n("Medya", 1_020), n("78 Takip ediliyor", 787))
        assertEquals(XScreen.TWEET_DETAIL, ScreenDetector.detect(nodes))
        assertEquals(9, ReplyComposerEvidence.openIndex(nodes))
    }
    @Test fun roleSuffixesCannotTurnAnArbitrarySentenceIntoTheReplyField() {
        assertTrue(ReplyComposerEvidence.isEntry("Post your reply, Edit box"))
        assertTrue(ReplyComposerEvidence.isEntry("Yanıtını gönder, Düğme"))
        assertFalse(ReplyComposerEvidence.isEntry("Yanıtını gönder ve hesabımı takip et"))
    }
    @Test fun aHiddenPlaceholderAndAnImageButtonCannotStealFocus() {
        val nodes = listOf(n("Yanıtını gönder", 1_410, visible = false), n("Görsel ekle", 1_385), n("GIF", 1_385))
        assertNull(ReplyComposerEvidence.focusIndex(nodes))
    }
    @Test fun duplicateSemanticsOfTheSameEditorDoNotBlockItsTextRead() {
        val node = n(comment, 800, id = "tweet_box", editable = true)
        assertTrue(ReplyComposerEvidence.contains(listOf(node, node.copy(editable = false)), comment))
        assertNull(ReplyComposerEvidence.editorIndex(listOf(node, node.copy(text = "Başka taslak"))))
    }
    @Test fun freshInputTextProofAllowsSubmissionWhileNoTextProofStillWaits() {
        assertEquals(QuoteReplyFlowPolicy.Decision.PREPARE_SUBMIT, QuoteReplyFlowPolicy.decide(
            XFlowStage.FILL_COMPOSER, true, ReplyTextTransfer.matches(comment, comment), false, true, 1, 500))
        assertEquals(QuoteReplyFlowPolicy.Decision.WAIT, QuoteReplyFlowPolicy.decide(
            XFlowStage.SUBMIT_COMPOSER, true, ReplyTextTransfer.matches("", comment), false, true, 1, 500))
    }
}
