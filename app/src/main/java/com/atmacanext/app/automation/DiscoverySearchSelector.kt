package com.atmacanext.app.automation

/** Search controls only; never choose a tweet mention or the query field as a result. */
internal object DiscoverySearchSelector {
    private val searchLabels = setOf("search", "ara", "search and explore", "ara ve keşfet", "keşfet", "explore")
    private val fieldLabels = setOf("search x", "x'te ara", "x’te ara", "search twitter", "twitter'da ara", "search", "ara")
    private fun labels(n: NodeSnapshot) = listOfNotNull(n.text, n.contentDescription).map(XUiVocabulary::normalize)
    private fun usable(n: NodeSnapshot) = n.visible && n.enabled && n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top
    private fun labeled(n: NodeSnapshot, values: Set<String>) = labels(n).any { label ->
        values.any { label == it || label.startsWith("$it,") || label.startsWith("$it sekme") || label.startsWith("$it tab") }
    }
    fun searchTab(nodes: List<NodeSnapshot>): Int? {
        val bottom = nodes.filter { it.visible }.maxOfOrNull { it.bounds.bottom } ?: return null
        return nodes.indices.firstOrNull { i ->
            val n = nodes[i]
            usable(n) && !n.editable && n.bounds.top > bottom * 0.65 && labeled(n, searchLabels)
        }
    }
    fun searchField(nodes: List<NodeSnapshot>): Int? = nodes.indices.firstOrNull { i ->
        val n = nodes[i]
        val id = n.viewId.orEmpty().substringAfterLast('/').lowercase()
        usable(n) && n.editable && (id in setOf("query", "search_src_text", "search_query", "search_edit_text") || labeled(n, fieldLabels))
    } ?: nodes.indices.filter { usable(nodes[it]) && nodes[it].editable }.singleOrNull()

    fun searchEntry(nodes: List<NodeSnapshot>): Int? = nodes.indices.firstOrNull { i ->
        val n = nodes[i]
        usable(n) && labeled(n, fieldLabels) && !n.editable
    }
    fun peopleTab(nodes: List<NodeSnapshot>): Int? = nodes.indices.firstOrNull { i ->
        usable(nodes[i]) && labeled(nodes[i], setOf("people", "kişiler"))
    }
    fun result(nodes: List<NodeSnapshot>, handle: String, fieldIndex: Int): Int? {
        val field = nodes.getOrNull(fieldIndex) ?: return null
        // Autocomplete and People results expose dedicated @handle labels.
        // Do not search arbitrary text for a mention or tap a row's Follow button.
        return nodes.indices.firstOrNull { i ->
            val n = nodes[i]
            usable(n) && !n.editable && i != fieldIndex && n.bounds.top >= field.bounds.bottom &&
                listOfNotNull(n.text, n.contentDescription).any { it.trim().equals("@$handle", ignoreCase = true) } &&
                labels(n).none { label -> (XUiVocabulary.followActions + XUiVocabulary.followingActions).any { label.contains(it) } }
        }
    }
}
