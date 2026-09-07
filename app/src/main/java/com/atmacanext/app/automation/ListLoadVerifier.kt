package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo

object ListLoadVerifier {
    fun isLoaded(root: AccessibilityNodeInfo?, screen: XScreen): Boolean =
        isLoaded(AccessibilityTree.snapshots(root), screen)

    private fun hasHeader(label: String, headers: Set<String>): Boolean = headers.any {
        label == it || label.startsWith("$it,") || label.startsWith("$it ·") || label.startsWith("$it sekme") || label.startsWith("$it tab")
    }

    internal fun isLoaded(nodes: List<NodeSnapshot>, screen: XScreen): Boolean {
        if (nodes.isEmpty()) return false
        val labels = nodes.flatMap { listOfNotNull(it.text, it.contentDescription) }
            .map(XUiVocabulary::normalize)
            .filter(String::isNotBlank)
        val corpus = labels.joinToString(" ")
        val handles = labels.mapNotNull(XIdentityDetector::extractHandle).distinct()
        val hasAction = labels.any { it in XUiVocabulary.followActions || it in XUiVocabulary.followingActions }
        val empty = XUiVocabulary.emptyListSignals.any(corpus::contains)

        val correctHeader = when (screen) {
            XScreen.FOLLOWERS_LIST -> labels.any { hasHeader(it, XUiVocabulary.followersHeaders) }
            XScreen.FOLLOWING_LIST -> labels.any { hasHeader(it, XUiVocabulary.followingHeaders) }
            XScreen.VERIFIED_FOLLOWERS_LIST -> labels.any { hasHeader(it, XUiVocabulary.verifiedFollowersHeaders) }
            else -> false
        }
        return correctHeader && ((handles.isNotEmpty() && hasAction) || handles.size >= 2 || empty)
    }
}
