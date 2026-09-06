package com.atmacanext.app.automation

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File

/**
 * Captures a small sanitized accessibility probe whenever the meaningful X tree changes.
 * It is designed for Checkpoint 10 device hardening: labels not recognized as structural X UI
 * are redacted, handles are hashed, and the log is size-capped/rotated.
 */
object XUiDiagnostics {
    private const val MIN_CAPTURE_INTERVAL_MS = 700L
    private const val MAX_FILE_BYTES = 600_000L
    private const val MAX_NODES_PER_CAPTURE = 260

    private var lastCaptureAt = 0L
    private var lastSignature: String? = null

    @Synchronized
    fun record(
        context: Context,
        root: AccessibilityNodeInfo?,
        screen: XScreen,
        popup: PopupType,
        now: Long = System.currentTimeMillis(),
    ) = record(context, AccessibilityTree.snapshots(root, maxNodes = MAX_NODES_PER_CAPTURE), screen, popup, now)

    @Synchronized
    fun record(
        context: Context,
        snapshots: List<NodeSnapshot>,
        screen: XScreen,
        popup: PopupType,
        now: Long = System.currentTimeMillis(),
    ) {
        if (now - lastCaptureAt < MIN_CAPTURE_INTERVAL_MS) return
        if (snapshots.isEmpty()) return

        val signature = buildSignature(screen, popup, snapshots)
        if (signature == lastSignature) return
        lastSignature = signature
        lastCaptureAt = now

        val file = probeFile(context)
        rotateIfNeeded(file)
        file.parentFile?.mkdirs()
        file.appendText(buildBlock(now, screen, popup, signature, snapshots))
    }

    fun probeFile(context: Context): File = File(context.filesDir, "x_diagnostics/accessibility_probe.txt")

    fun rotatedProbeFile(context: Context): File = File(context.filesDir, "x_diagnostics/accessibility_probe.previous.txt")

    internal fun buildSignature(screen: XScreen, popup: PopupType, nodes: List<NodeSnapshot>): String {
        val material = nodes.asSequence()
            .map { node ->
                listOf(
                    XDiagnosticSanitizer.sanitizeLabel(node.text).orEmpty(),
                    XDiagnosticSanitizer.sanitizeLabel(node.contentDescription).orEmpty(),
                    XDiagnosticSanitizer.sanitizeViewId(node.viewId).orEmpty(),
                    node.className.orEmpty().substringAfterLast('.'),
                    node.clickable.toString(),
                    node.scrollable.toString(),
                    node.bounds.top.toString(),
                    node.bounds.bottom.toString(),
                ).joinToString("~")
            }
            .take(180)
            .joinToString("|")
        return "$screen/$popup/${material.hashCode().toString(16)}"
    }

    private fun buildBlock(
        now: Long,
        screen: XScreen,
        popup: PopupType,
        signature: String,
        nodes: List<NodeSnapshot>,
    ): String = buildString {
        appendLine("--- capture=$now screen=$screen popup=$popup signature=$signature nodes=${nodes.size} ---")
        nodes.forEachIndexed { index, node ->
            val text = XDiagnosticSanitizer.sanitizeLabel(node.text) ?: ""
            val desc = XDiagnosticSanitizer.sanitizeLabel(node.contentDescription) ?: ""
            val id = XDiagnosticSanitizer.sanitizeViewId(node.viewId) ?: ""
            val clazz = node.className.orEmpty().substringAfterLast('.').take(80)
            append(index)
            append("|id=").append(id)
            append("|class=").append(clazz)
            append("|text=").append(text)
            append("|desc=").append(desc)
            append("|click=").append(node.clickable)
            append("|scroll=").append(node.scrollable)
            append("|enabled=").append(node.enabled)
            append("|bounds=").append(node.bounds.left).append(',').append(node.bounds.top)
                .append(',').append(node.bounds.right).append(',').append(node.bounds.bottom)
            appendLine()
        }
    }

    private fun rotateIfNeeded(file: File) {
        if (!file.exists() || file.length() < MAX_FILE_BYTES) return
        val previous = File(file.parentFile, "accessibility_probe.previous.txt")
        if (previous.exists()) previous.delete()
        file.renameTo(previous)
    }
}
