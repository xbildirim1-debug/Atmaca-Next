package com.atmacanext.app.automation

/** Search controls only; never choose a tweet mention or the query field as a result. */
internal object DiscoverySearchSelector {
    private val searchLabels = setOf("search", "ara", "search and explore", "ara ve keşfet", "keşfet", "explore")
    private val fieldLabels = setOf("search x", "x'te ara", "x’te ara", "search twitter", "twitter'da ara", "search", "ara")
    private val homeLabels = setOf("home", "anasayfa")
    private val searchIdTokens = setOf(
        "navigation_search", "bottom_search", "search_tab", "tab_search", "nav_search",
        "navigation_explore", "bottom_explore", "explore_tab", "tab_explore", "nav_explore",
    )
    private fun labels(n: NodeSnapshot) = listOfNotNull(n.text, n.contentDescription).map(XUiVocabulary::normalize)
    private fun usable(n: NodeSnapshot) = n.visible && n.enabled && n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top
    private fun labeled(n: NodeSnapshot, values: Set<String>) = labels(n).any { label ->
        values.any { label == it || label.startsWith("$it,") || label.startsWith("$it sekme") || label.startsWith("$it tab") }
    }

    /**
     * X can publish a different accessibility tree after the third/fourth account
     * switch: the bottom Search icon is still visible, but its text/contentDescription
     * can temporarily be blank. Discovery must not wait on HOME until the user taps it.
     *
     * Resolution order is deliberately conservative:
     *  1) explicit Search/Explore label,
     *  2) a search/explore resource id in the bottom navigation band,
     *  3) only when a labeled HOME tab proves the bottom-nav geometry, the first
     *     aligned navigation item immediately to its right.
     *
     * The final fallback uses live bounds, never a fixed screen coordinate.
     */
    fun searchTab(nodes: List<NodeSnapshot>): Int? {
        val visible = nodes.filter { it.visible && it.bounds.right > it.bounds.left && it.bounds.bottom > it.bounds.top }
        if (visible.isEmpty()) return null
        val screenTop = visible.minOf { it.bounds.top }
        val screenBottom = visible.maxOf { it.bounds.bottom }
        val screenLeft = visible.minOf { it.bounds.left }
        val screenRight = visible.maxOf { it.bounds.right }
        val screenHeight = (screenBottom - screenTop).coerceAtLeast(1)
        val screenWidth = (screenRight - screenLeft).coerceAtLeast(1)
        val lowerBandTop = screenTop + screenHeight * 0.65f
        val bottomCandidates = nodes.indices.filter { i ->
            val n = nodes[i]
            usable(n) && !n.editable && n.bounds.top > lowerBandTop
        }

        bottomCandidates.firstOrNull { labeled(nodes[it], searchLabels) }?.let { return it }

        bottomCandidates.firstOrNull { i ->
            val id = nodes[i].viewId.orEmpty().substringAfterLast('/').lowercase()
            searchIdTokens.any(id::contains) &&
                listOf("query", "edit", "input", "field", "src_text").none(id::contains)
        }?.let { return it }

        val homeIndex = bottomCandidates.firstOrNull { labeled(nodes[it], homeLabels) } ?: return null
        val home = nodes[homeIndex]
        val homeCenterX = (home.bounds.left + home.bounds.right) / 2f
        val homeCenterY = (home.bounds.top + home.bounds.bottom) / 2f
        val minHorizontalGap = maxOf((home.bounds.right - home.bounds.left) * 0.45f, screenWidth * 0.06f)
        val maxHorizontalGap = screenWidth * 0.36f
        val maxVerticalDrift = screenHeight * 0.055f

        return bottomCandidates.asSequence()
            .filter { it != homeIndex }
            .filter { i ->
                val n = nodes[i]
                val centerX = (n.bounds.left + n.bounds.right) / 2f
                val centerY = (n.bounds.top + n.bounds.bottom) / 2f
                val dx = centerX - homeCenterX
                val clazz = n.className.orEmpty().lowercase()
                val navLike = n.clickable || clazz.contains("button") || clazz.contains("image") || clazz.contains("view")
                dx >= minHorizontalGap && dx <= maxHorizontalGap &&
                    kotlin.math.abs(centerY - homeCenterY) <= maxVerticalDrift &&
                    navLike && labels(n).none { it in homeLabels }
            }
            .minByOrNull { i -> (nodes[i].bounds.left + nodes[i].bounds.right) / 2f }
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
