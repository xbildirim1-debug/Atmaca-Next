package com.atmacanext.app.automation

/** Counts are presentation data, not selectors. Never match the repost action button. */
internal object EngagementListEvidence {
    fun isReposts(raw: String): Boolean {
        val s = XUiVocabulary.normalize(raw).substringBefore(", selected").substringBefore(", seçili")
        return s in setOf("yeniden gönderenler", "retweetler", "reposts", "reposted by") ||
            Regex("[0-9., kmb]+ (?:kişi )?tarafından yeniden gönderildi").matches(s) ||
            Regex("(?:reposts [0-9.,kmb ]+|[0-9.,kmb ]+ reposts|reposted by [0-9.,kmb ]+)").matches(s)
    }
    fun selected(root: android.view.accessibility.AccessibilityNodeInfo?): Boolean = AccessibilityTree.nodes(root).any { node ->
        node.isVisibleToUser && (node.isSelected || node.isChecked || node.contentDescription.orEmpty().let {
            it.contains("selected", true) || it.contains("seçili", true) }) &&
            AccessibilityTree.snapshots(node, 12).any { child -> listOfNotNull(child.text, child.contentDescription).any(::isReposts) }
    }
    fun selected(nodes: List<NodeSnapshot>): Boolean = nodes.any { n ->
        n.visible && (n.selected || n.checked || n.contentDescription.orEmpty().let {
            it.contains("selected", true) || it.contains("seçili", true) }) &&
            listOfNotNull(n.text, n.contentDescription).any(::isReposts)
    }
    fun title(nodes: List<NodeSnapshot>): Boolean = nodes.any { n -> n.visible &&
        listOfNotNull(n.text, n.contentDescription).any { XUiVocabulary.normalize(it) in setOf("gönderi etkileşimleri", "post engagements", "tweet engagements") } }
    fun openLabel(raw: String) = XUiVocabulary.normalize(raw) in setOf("alıntıları görüntüle", "view quotes", "view post engagements", "gönderi etkileşimlerini görüntüle")
}
