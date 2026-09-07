package com.atmacanext.app.automation

internal object AccountRowSelectionEvidence {
    fun isSelected(nodes: List<NodeSnapshot>, username: String): Boolean {
        val wanted = XIdentityDetector.normalizeUsername(username)
        val labels = nodes.flatMap { listOfNotNull(it.text, it.contentDescription) }
        val handles = labels.mapNotNull(AccountSwitcherInspector::dedicatedHandle).toSet()
        if (handles != setOf(wanted)) return false
        val markers = setOf("selected", "current account", "checked", "checkmark", "seçili",
            "aktif hesap", "işaretli", "onay işareti")
        return nodes.any { it.visible && (it.selected || it.checked ||
            listOfNotNull(it.text, it.contentDescription).any { text ->
                XUiVocabulary.normalize(text) in markers
            }) }
    }
}
