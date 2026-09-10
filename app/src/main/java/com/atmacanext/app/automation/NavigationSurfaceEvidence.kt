package com.atmacanext.app.automation

internal object NavigationSurfaceEvidence {
    private fun labels(n: NodeSnapshot) = listOfNotNull(n.text, n.contentDescription).map(XUiVocabulary::normalize)

    fun backIndex(nodes: List<NodeSnapshot>): Int? = backIndex(
        nodes = nodes,
        flowStage = AutomationController.state.value.flowStage,
        screen = AutomationController.state.value.activeScreen,
    )

    internal fun backIndex(nodes: List<NodeSnapshot>, flowStage: XFlowStage, screen: XScreen): Int? {
        // Discovery flows return ENGAGEMENT_LIST -> TWEET_DETAIL -> target PROFILE.
        // Once the target profile surface is reached, never emit another Back merely
        // because its identity/header takes a moment to settle. The runtime can wait
        // and fall back to X search instead of escaping the target profile entirely.
        val returningToTargetProfile = flowStage == XFlowStage.RETURN_DISCOVERY_TARGET &&
            (screen == XScreen.PROFILE ||
                (screen == XScreen.UNKNOWN &&
                    (ProfileSurfaceEvidence.read(nodes) != null || FeedRowEvidence.profileFeed(nodes))))
        if (returningToTargetProfile) return null

        return nodes.indices.firstOrNull { i ->
            val n = nodes[i]
            n.visible && n.enabled && !n.editable &&
                n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top &&
                (labels(n).any { it in XUiVocabulary.backSignals } ||
                    n.viewId.orEmpty().let { it.endsWith("/back") || it.contains("toolbar_back") || it.contains("navigate_up") })
        }
    }

    fun profileActionEnd(nodes: List<NodeSnapshot>, statTop: Int): Int = nodes.filter { n ->
        n.visible && n.bounds.top >= statTop && labels(n).any { it == "gönderiler" || it == "posts" || it.startsWith("gönderiler,") || it.startsWith("posts,") }
    }.minOfOrNull { it.bounds.top } ?: statTop

    /** Only the sparse back/loading surface, never an arbitrary UNKNOWN page. */
    fun loading(nodes: List<NodeSnapshot>): Boolean {
        val visible = nodes.filter { it.visible }
        if (backIndex(visible) == null || visible.any { it.editable }) return false
        val allowed = XUiVocabulary.backSignals + setOf("loading", "loading…", "loading...", "yükleniyor", "yükleniyor…", "yükleniyor...", "please wait", "lütfen bekleyin")
        return visible.flatMap(::labels).filter { it.isNotBlank() }.all { it in allowed }
    }
}
