package com.atmacanext.app.automation

/** A successful Back dispatch is not proof that the profile was left. */
internal object AccountProfileReturnPolicy {
    enum class Action { READY, POP_PROFILE, WAIT, LEAVE_DETAIL }

    fun decide(screen: XScreen, profilePresent: Boolean, homePresent: Boolean): Action = when {
        screen == XScreen.ACCOUNT_DRAWER || screen == XScreen.ACCOUNT_SWITCHER -> Action.READY
        profilePresent || screen == XScreen.PROFILE -> Action.POP_PROFILE
        screen == XScreen.HOME && homePresent -> Action.READY
        screen == XScreen.TWEET_DETAIL -> Action.LEAVE_DETAIL
        else -> Action.WAIT
    }

    fun ownProfile(nodes: List<NodeSnapshot>, expectedHandle: String?): Boolean {
        val profile = ProfileSurfaceEvidence.read(nodes) ?: return false
        if (!expectedHandle.isNullOrBlank() && profile.handle != XIdentityDetector.normalizeUsername(expectedHandle)) return false
        return nodes.any { n -> n.visible && n.enabled && labels(n).any { label ->
            XUiVocabulary.ownProfileSignals.any { label == it || label == "$it, düğme" || label == "$it, button" }
        } }
    }

    fun profilePresent(nodes: List<NodeSnapshot>): Boolean = ProfileSurfaceEvidence.read(nodes) != null ||
        nodes.any { n -> n.visible && labels(n).any { it in XUiVocabulary.ownProfileSignals } }

    fun backIndex(nodes: List<NodeSnapshot>, expectedHandle: String?): Int? {
        if (!ownProfile(nodes, expectedHandle)) return null
        val profile = ProfileSurfaceEvidence.read(nodes) ?: return null
        val headerTop = nodes.filter { n -> n.visible && labels(n).any { AccountSwitcherInspector.dedicatedHandle(it) == profile.handle } }
            .minOfOrNull { it.bounds.top } ?: return null
        val viewport = AdaptiveNavigationEvidence.viewport(nodes) ?: return null
        val width = viewport.right - viewport.left
        // Only an identified toolbar arrow above the profile header. A tweet
        // containing "geri" or an unlabelled corner control is never a Back.
        return nodes.indices.filter { i ->
            val n = nodes[i]
            val id = n.viewId.orEmpty().substringAfterLast('/').lowercase()
            n.visible && n.enabled && !n.editable && n.bounds.top >= viewport.top &&
                n.bounds.bottom <= headerTop && n.bounds.right - n.bounds.left in 1..(width / 3) &&
                n.bounds.bottom - n.bounds.top in 1..(width / 3) &&
                (labels(n).any { label -> XUiVocabulary.backSignals.any { label == it || label == "$it, düğme" || label == "$it, button" } } ||
                    id in setOf("back", "toolbar_back", "navigate_up"))
        }.minByOrNull { (nodes[it].bounds.right - nodes[it].bounds.left) * (nodes[it].bounds.bottom - nodes[it].bounds.top) }
    }

    private fun labels(n: NodeSnapshot) = listOfNotNull(n.text, n.contentDescription).map(XUiVocabulary::normalize)
}
