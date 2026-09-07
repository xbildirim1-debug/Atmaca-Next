package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo

/** Reads the actually selected relationship tab from X's raw accessibility nodes. */
object RelationshipTabInspector {
    const val NONE = 0
    const val FOLLOWING = 1
    const val FOLLOWERS = 2
    const val OTHER = 3
    const val VERIFIED = 4

    private val following = setOf("following", "takip edilen", "takip ediliyor")
    private val followers = setOf("followers", "takipçiler", "takipçi")
    private val other = setOf(
        "subscribers", "subscriptions", "aboneler", "abonelikler", "you know", "tanıyor olabileceğin",
    )
    private val selectedState = setOf("selected", "seçili", "active", "aktif")

    internal fun classifySelectedLabels(raw: List<String>): Int {
        val labels = raw.map(XUiVocabulary::normalize)
        // Selected account rows and a selected pager containing several tabs are not tab evidence.
        if (labels.any { XIdentityDetector.extractHandle(it) != null }) return NONE
        fun matches(tokens: Set<String>) = labels.any { label -> tokens.any {
            label == it || label.startsWith("$it,") || label.startsWith("$it ·") ||
                label.startsWith("$it sekme") || label.startsWith("$it tab")
        } }
        val types = listOf(VERIFIED to XUiVocabulary.verifiedFollowersHeaders, FOLLOWING to following, FOLLOWERS to followers, OTHER to other)
            .filter { matches(it.second) }.map { it.first }
        return types.singleOrNull() ?: NONE
    }

    fun selectedTab(root: AccessibilityNodeInfo?): Int {
        if (root == null) return NONE
        for (node in AccessibilityTree.nodes(root, maxNodes = 900)) {
            if (!node.isVisibleToUser) continue
            val ownDescription = XUiVocabulary.normalize(node.contentDescription?.toString())
            val selected = node.isSelected || node.isChecked || selectedState.any(ownDescription::contains)
            if (!selected) continue
            val labels = AccessibilityTree.nodes(node, maxNodes = 16).flatMap {
                listOfNotNull(it.text?.toString(), it.contentDescription?.toString())
            }
            val tab = classifySelectedLabels(labels)
            if (tab != NONE) return tab
        }
        return NONE
    }
}
