package com.atmacanext.app.automation

/** Text-entry ports let us test a successful accessibility action that changes no text. */
internal object ReplyTextTransfer {
    enum class Result { ALREADY_PRESENT, INPUT_CONNECTION, SET_TEXT, PASTE, FOCUS_REQUESTED, WAIT }
    interface Connection {
        fun readFullText(): String?
        fun replaceAll(previous: String, value: String): Boolean
    }
    interface Editor {
        val focused: Boolean
        val text: String
        val connection: Connection?
        fun focus(): Boolean
        fun setText(value: String): Boolean
        fun pasteReplacing(value: String): Boolean
    }

    fun permitsConnection(packageName: String?, inputType: Int, focused: Boolean): Boolean =
        focused && packageName == "com.twitter.android" && (inputType and 0xF) == 1 &&
            (inputType and 0xFF0) !in setOf(0x80, 0x90, 0xE0)

    fun matches(actual: String?, expected: String): Boolean = expected.isNotBlank() &&
        actual?.replace(Regex("\\s+"), " ")?.trim() == expected.replace(Regex("\\s+"), " ").trim()

    fun write(editor: Editor, value: String, attempt: Int): Result {
        if (value.isBlank()) return Result.WAIT
        if (matches(editor.text, value)) return Result.ALREADY_PRESENT
        if (!editor.focused) return if (editor.focus()) Result.FOCUS_REQUESTED else Result.WAIT
        // Native input avoids Compose's accepted-but-ineffective ACTION_SET_TEXT.
        val connection = editor.connection
        val previous = connection?.readFullText()
        if (matches(previous, value)) return Result.ALREADY_PRESENT
        if (previous != null && connection != null && connection.replaceAll(previous, value)) return Result.INPUT_CONNECTION
        // A later frame still empty after an accepted SET_TEXT must exercise paste.
        if (attempt <= 1 && editor.setText(value)) return Result.SET_TEXT
        if (editor.pasteReplacing(value)) return Result.PASTE
        if (attempt > 1 && editor.setText(value)) return Result.SET_TEXT
        return Result.WAIT
    }
}
