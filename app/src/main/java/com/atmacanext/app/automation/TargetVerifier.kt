package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/** Central guard that rejects nodes known to be unsafe or unrelated to the requested action. */
object TargetVerifier {
    fun isSafeClickable(node: AccessibilityNodeInfo?): Boolean {
        if (node == null || !node.isEnabled || !node.isClickable) return false
        val snapshot = node.toSnapshot()
        val combined = listOfNotNull(snapshot.text, snapshot.contentDescription, snapshot.viewId)
            .joinToString(" ")
            .lowercase(Locale.ROOT)
        return XUiVocabulary.forbiddenProfilePhrases.none { combined.contains(it) }
    }

    fun matchesExactLabel(node: AccessibilityNodeInfo?, acceptedLabels: Set<String>): Boolean {
        if (!isSafeClickable(node)) return false
        val labels = acceptedLabels.map(XUiVocabulary::normalize).toSet()
        val candidates = listOfNotNull(node?.text?.toString(), node?.contentDescription?.toString())
            .map(XUiVocabulary::normalize)
        return candidates.any { it in labels }
    }

    fun containsForbiddenProfilePhrase(node: AccessibilityNodeInfo): Boolean {
        val labels = AccessibilityTree.nodes(node, maxNodes = 24).flatMap { n ->
            listOfNotNull(n.text?.toString(), n.contentDescription?.toString())
        }.map(XUiVocabulary::normalize)
        return labels.any { label -> XUiVocabulary.forbiddenProfilePhrases.any(label::contains) }
    }
}
