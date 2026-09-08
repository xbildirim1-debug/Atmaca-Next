package com.atmacanext.app.automation

/** Select the username itself, never the relationship button or a whole row ancestor. */
internal object SourceProfileTarget {
    fun index(nodes: List<NodeSnapshot>, username: String): Int? {
        val wanted = XIdentityDetector.normalizeUsername(username)
        if (wanted.isBlank()) return null
        return nodes.indices.firstOrNull { i ->
            val n = nodes[i]
            val labels = listOfNotNull(n.text, n.contentDescription)
            n.visible && n.enabled && n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top &&
                labels.any { AccountSwitcherInspector.dedicatedHandle(it) == wanted } &&
                labels.none { VerifiedFollowPolicy.matchesAction(it, XUiVocabulary.followActions + XUiVocabulary.followingActions) }
        }
    }
}
