package com.atmacanext.app.automation

/** Distinguishes the home pager's Following feed from a profile's relationship lists. */
internal object HomeTimelineEvidence {
    fun matches(nodes: List<NodeSnapshot>): Boolean {
        val visible = nodes.filter { it.visible }
        val labels = visible.flatMap { listOfNotNull(it.text, it.contentDescription) }
            .map(XUiVocabulary::normalize)
        fun tab(label: String, title: String): Boolean = label == title ||
            label.startsWith("$title,") || label.startsWith("$title ·") ||
            label.startsWith("$title sekme") || label.startsWith("$title tab")
        val forYou = labels.any { tab(it, "sana özel") || tab(it, "for you") }
        val following = labels.any { label -> XUiVocabulary.followingHeaders.any { tab(label, it) } }
        val homeId = visible.any { it.viewId.orEmpty().substringAfterLast('/').contains("home_timeline") }
        return homeId || (forYou && following)
    }
}
