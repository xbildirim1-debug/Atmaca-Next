package com.atmacanext.app.automation

import android.annotation.TargetApi
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/** Android 13+ accessibility input; the user's keyboard stays selected. */
internal object ReplyInputConnection {
    fun bind(service: AtmacaAccessibilityService, node: AccessibilityNodeInfo): ReplyTextTransfer.Connection? {
        if (Build.VERSION.SDK_INT < 33 || !node.isFocused || !node.isVisibleToUser || !node.isEnabled) return null
        return runCatching { bind33(service, node) }.getOrNull()
    }

    @TargetApi(33)
    private fun bind33(service: AtmacaAccessibilityService, node: AccessibilityNodeInfo): ReplyTextTransfer.Connection? {
        val method = service.inputMethod ?: return null
        val info = method.currentInputEditorInfo ?: return null
        if (!method.currentInputStarted || !ReplyTextTransfer.permitsConnection(info.packageName, info.inputType, node.isFocused)) return null
        val connection = method.currentInputConnection ?: return null
        fun stillCurrent(): Boolean {
            val live = method.currentInputEditorInfo ?: return false
            return node.refresh() && node.isVisibleToUser && node.isEnabled && node.isFocused &&
                live.packageName == info.packageName && live.fieldId == info.fieldId &&
                live.fieldName == info.fieldName && live.inputType == info.inputType && method.currentInputStarted
        }
        return object : ReplyTextTransfer.Connection {
            override fun readFullText(): String? = runCatching {
                if (!stillCurrent()) return@runCatching null
                val text = connection.getSurroundingText(4_096, 4_096, 0) ?: return@runCatching null
                // A truncated slice cannot authorize replacing all text or submission.
                text.text?.toString()?.takeIf { text.offset == 0 && it.length < 4_096 && stillCurrent() }
            }.getOrNull()

            override fun replaceAll(previous: String, value: String): Boolean = runCatching {
                if (!stillCurrent()) return@runCatching false
                connection.setSelection(0, previous.length)
                connection.commitText(value, 1, null)
                true // Dispatch only; a new frame must read the exact result.
            }.getOrDefault(false)
        }
    }
}
