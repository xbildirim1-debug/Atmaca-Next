package com.atmacanext.app.automation

/** Confirms a discovery target even when X temporarily omits the profile header handle. */
internal object DiscoveryProfileEvidence {
    fun matches(nodes: List<NodeSnapshot>, detectedHandle: String?, expected: String): Boolean {
        val target = XIdentityDetector.normalizeUsername(expected)
        if (target.isBlank() || nodes.any { it.visible && it.editable } || EngagementListEvidence.title(nodes)) return false
        val profile = ProfileSurfaceEvidence.read(nodes)
        if (profile != null) return profile.handle == target
        if (!FeedRowEvidence.profileFeed(nodes)) return false
        // An explicit different profile header wins over reposts/mentions of target.
        val tabsTop = nodes.filter { it.visible && listOfNotNull(it.text, it.contentDescription)
            .any { s -> XUiVocabulary.normalize(s) in setOf("gönderiler", "posts") } }
            .minOfOrNull { it.bounds.top }
        val header = tabsTop?.let { top -> nodes.filter { it.visible && !it.editable && it.bounds.bottom <= top }
            .sortedBy { it.bounds.top }.firstNotNullOfOrNull { n ->
                listOfNotNull(n.text, n.contentDescription).firstNotNullOfOrNull(AccountSwitcherInspector::dedicatedHandle)
            } }
        if (header != null) return header == target
        if (!detectedHandle.isNullOrBlank() && XIdentityDetector.normalizeUsername(detectedHandle) != target) return false
        return FeedRowEvidence.profileFeed(nodes) && FeedRowEvidence.rows(nodes).any { it.author == target }
    }
}
