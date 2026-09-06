package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Resource id > content description > exact text > class heuristics.
 * Coordinates are intentionally not part of selection.
 */
object NodeSelector {
    data class Query(
        val viewIdContains: List<String> = emptyList(),
        val contentDescriptions: Set<String> = emptySet(),
        val exactTexts: Set<String> = emptySet(),
        val classNameContains: String? = null,
        val requireClickable: Boolean = true,
    )

    data class Match(val node: AccessibilityNodeInfo, val score: Int)

    fun best(root: AccessibilityNodeInfo?, query: Query): Match? {
        return AccessibilityTree.nodes(root)
            .asSequence()
            .mapNotNull { node -> score(node, query)?.let { Match(node, it) } }
            .maxByOrNull { it.score }
    }

    private fun score(node: AccessibilityNodeInfo, query: Query): Int? {
        if (query.requireClickable && !TargetVerifier.isSafeClickable(node)) return null

        val text = node.text?.toString()?.lowercase(Locale.ROOT)?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase(Locale.ROOT)?.trim().orEmpty()
        val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
        val className = node.className?.toString()?.lowercase(Locale.ROOT).orEmpty()

        var score = 0
        query.viewIdContains.forEach { token ->
            if (viewId.contains(token.lowercase(Locale.ROOT))) score += 100
        }
        if (query.contentDescriptions.any { desc == it.lowercase(Locale.ROOT).trim() }) score += 60
        if (query.exactTexts.any { text == it.lowercase(Locale.ROOT).trim() }) score += 50
        query.classNameContains?.let { if (className.contains(it.lowercase(Locale.ROOT))) score += 10 }

        return score.takeIf { it > 0 }
    }
}
