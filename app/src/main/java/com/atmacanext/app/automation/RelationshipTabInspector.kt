package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo

/** Reads the actually selected relationship tab from X's raw accessibility nodes. */
object RelationshipTabInspector {
    const val NONE = 0
    const val FOLLOWING = 1
    const val FOLLOWERS = 2
    const val OTHER = 3

    private val following = setOf("following", "takip edilen", "takip ediliyor")
    private val followers = setOf("followers", "takipçiler", "takipçi")
    private val other = setOf(
        "subscribers", "subscriptions", "aboneler", "abonelikler", "you know", "tanıyor olabileceğin",
    )
    private val selectedState = setOf("selected", "seçili", "active", "aktif")

    fun selectedTab(root: AccessibilityNodeInfo?): Int {
        if (root == null) return NONE
        for (node in AccessibilityTree.nodes(root, maxNodes = 900)) {
            val ownDescription = XUiVocabulary.normalize(node.contentDescription?.toString())
            val selected = node.isSelected || node.isChecked || selectedState.any(ownDescription::contains)
            if (!selected) continue
            for (candidate in AccessibilityTree.nodes(node, maxNodes = 16)) {
                val labels = listOfNotNull(candidate.text?.toString(), candidate.contentDescription?.toString())
                    .map(XUiVocabulary::normalize)
                if (labels.any { label -> following.any(label::contains) }) return FOLLOWING
                if (labels.any { label -> followers.any(label::contains) }) return FOLLOWERS
                if (labels.any { label -> other.any(label::contains) }) return OTHER
            }
        }
        return NONE
    }
}
