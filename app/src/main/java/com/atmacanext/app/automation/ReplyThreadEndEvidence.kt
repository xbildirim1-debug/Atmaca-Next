package com.atmacanext.app.automation

/** Semantic end-of-thread markers shown by X after the last useful replies. */
internal object ReplyThreadEndEvidence {
    private val labels = setOf(
        "daha fazla keşfet",
        "daha fazlasını keşfet",
        "daha fazlasını keşfedin",
        "discover more",
        "explore more",
    )

    fun isEndLabel(raw: String?): Boolean {
        val value = XUiVocabulary.normalize(raw)
            .substringBefore(", heading")
            .substringBefore(", başlık")
            .trim()
        return value in labels
    }

    fun visible(nodes: List<NodeSnapshot>): Boolean = nodes.any { node ->
        node.visible && !node.editable &&
            listOfNotNull(node.text, node.contentDescription).any(::isEndLabel)
    }

    fun visible(root: android.view.accessibility.AccessibilityNodeInfo?): Boolean =
        visible(AccessibilityTree.snapshots(root))
}
