package com.atmacanext.app.automation

/** Confirms a discovery target even when X temporarily omits the profile header handle. */
internal object DiscoveryProfileEvidence {
    fun matches(nodes: List<NodeSnapshot>, detectedHandle: String?, expected: String): Boolean {
        val target = XIdentityDetector.normalizeUsername(expected)
        if (XIdentityDetector.normalizeUsername(detectedHandle.orEmpty()) == target) return true
        return FeedRowEvidence.profileFeed(nodes) && FeedRowEvidence.rows(nodes).any { it.author == target }
    }
}
