package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class ReplyFallback26_63Test {
    private val comment = "Türkçe yorum ıİğş 👋"
    private class Connection : ReplyTextTransfer.Connection {
        var text = ""
        var calls = 0
        override fun readFullText() = text
        override fun replaceAll(previous: String, value: String): Boolean { calls++; return true }
    }
    private class Editor(override val connection: Connection) : ReplyTextTransfer.Editor {
        override var focused = true
        override var text = ""
        var pasteEffective = true
        var setEffective = false
        var setCalls = 0
        var pasteCalls = 0
        override fun focus() = true
        override fun setText(value: String): Boolean { setCalls++; if (setEffective) text = value; return true }
        override fun pasteReplacing(value: String): Boolean { pasteCalls++; if (pasteEffective) text = value; return true }
    }
    @Test fun anAcceptedNativeWriteThatChangesNothingUsesPasteOnTheNextFreshAttempt() {
        val input = Connection()
        val editor = Editor(input)
        assertEquals(ReplyTextTransfer.Result.INPUT_CONNECTION, ReplyTextTransfer.write(editor, comment, 1))
        assertFalse(ReplyTextTransfer.matches(editor.text, comment))
        assertEquals(ReplyTextTransfer.Result.PASTE, ReplyTextTransfer.write(editor, comment, 2))
        assertEquals(1, input.calls)
        assertEquals(comment, editor.text)
    }
    @Test fun aThirdAttemptUsesSetTextAfterNativeAndPasteBothAcknowledgeWithoutChangingText() {
        val input = Connection()
        val editor = Editor(input).apply { pasteEffective = false; setEffective = true }
        ReplyTextTransfer.write(editor, comment, 1)
        ReplyTextTransfer.write(editor, comment, 2)
        assertFalse(ReplyTextTransfer.matches(editor.text, comment))
        assertEquals(ReplyTextTransfer.Result.SET_TEXT, ReplyTextTransfer.write(editor, comment, 3))
        assertEquals(comment, editor.text)
        assertEquals(1, input.calls)
        assertEquals(1, editor.pasteCalls)
        assertEquals(1, editor.setCalls)
    }
    @Test fun textThatArrivesLateFromTheFirstNativeWriteIsReadBeforeAnyFallback() {
        val input = Connection()
        val editor = Editor(input)
        ReplyTextTransfer.write(editor, comment, 1)
        input.text = comment
        assertEquals(ReplyTextTransfer.Result.ALREADY_PRESENT, ReplyTextTransfer.write(editor, comment, 2))
        assertEquals(1, input.calls)
        assertEquals(0, editor.setCalls + editor.pasteCalls)
    }
    @Test fun neitherAcknowledgementNorTheThirdAttemptCanVerifyAnEmptyComment() {
        val input = Connection()
        val editor = Editor(input).apply { pasteEffective = false }
        for (attempt in 1..3) ReplyTextTransfer.write(editor, comment, attempt)
        assertFalse(ReplyTextTransfer.matches(editor.text, comment))
        assertFalse(ReplyTextTransfer.matches(input.readFullText(), comment))
        assertEquals(1, input.calls)
    }
}
