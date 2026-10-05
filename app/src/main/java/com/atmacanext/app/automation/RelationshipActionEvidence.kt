package com.atmacanext.app.automation

/** A relationship status can be disabled/nonclickable; a new Follow action cannot. */
internal object RelationshipActionEvidence {
    fun matches(node: NodeSnapshot, parentClickable: Boolean = false, observation: Boolean = false): Boolean {
        if (!node.visible || node.editable) return false
        val labels = listOfNotNull(node.text, node.contentDescription)
        val id = node.viewId.orEmpty().lowercase()
        val clazz = node.className.orEmpty().lowercase()
        if (id.contains("tab") || clazz.contains("tab") || labels.any {
                val label = XUiVocabulary.normalize(it)
                label.contains("sekme") || label.contains(" tab")
            }) return false
        val status = labels.any { VerifiedFollowPolicy.matchesAction(it,
            XUiVocabulary.followingActions + XUiVocabulary.requestedActions) }
        if (!node.enabled && !(observation && status)) return false
        val relationship = labels.any { VerifiedFollowPolicy.matchesAction(it,
            XUiVocabulary.followActions + XUiVocabulary.followingActions + XUiVocabulary.requestedActions) }
        return relationship && (node.clickable || clazz.contains("button") || id.contains("follow") ||
            parentClickable || (observation && status))
    }
}
