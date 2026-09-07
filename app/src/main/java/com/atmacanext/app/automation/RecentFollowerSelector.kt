package com.atmacanext.app.automation

/** Reads the current list each time. A follow button is never source-profile evidence. */
internal object RecentFollowerSelector {
    fun orderedHandles(nodes: List<NodeSnapshot>, excluded: Set<String>): List<String> = nodes
        .filter { it.visible && it.bounds.bottom > it.bounds.top && it.bounds.right > it.bounds.left }
        .sortedWith(compareBy<NodeSnapshot> { it.bounds.top }.thenBy { it.bounds.left })
        .mapNotNull { node -> listOfNotNull(node.text, node.contentDescription)
            .firstNotNullOfOrNull(AccountSwitcherInspector::dedicatedHandle) }
        .filter { it !in excluded }.distinct()
}
