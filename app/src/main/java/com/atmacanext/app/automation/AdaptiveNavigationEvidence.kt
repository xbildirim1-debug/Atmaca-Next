package com.atmacanext.app.automation

import android.graphics.Rect
import kotlin.math.abs

/** Navigation geometry comes from distinct live navigation labels, not device type. */
internal object AdaptiveNavigationEvidence {
    enum class Edge { LEFT, RIGHT, BOTTOM }
    data class Rail(val edge: Edge, val indices: List<Int>, val homeIndex: Int)
    private val roles = mapOf(
        "home" to "home", "anasayfa" to "home", "ana sayfa" to "home",
        "search" to "search", "ara" to "search", "explore" to "search", "keşfet" to "search",
        "search and explore" to "search", "ara ve keşfet" to "search",
        "grok" to "grok", "notifications" to "notifications", "bildirimler" to "notifications",
        "messages" to "messages", "mesajlar" to "messages", "direct messages" to "messages",
    )
    fun role(n: NodeSnapshot): String? = listOfNotNull(n.text, n.contentDescription).firstNotNullOfOrNull { value ->
        val label = XUiVocabulary.normalize(value)
        roles.entries.firstOrNull { label == it.key || label.startsWith("${it.key},") ||
            label.startsWith("${it.key} sekme") || label.startsWith("${it.key} tab") }?.value
    }
    private fun usable(n: NodeSnapshot) = n.visible && n.enabled && !n.editable &&
        n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top
    private fun cx(n: NodeSnapshot) = (n.bounds.left + n.bounds.right) / 2f
    private fun cy(n: NodeSnapshot) = (n.bounds.top + n.bounds.bottom) / 2f

    fun rail(nodes: List<NodeSnapshot>, viewport: Rect): Rail? {
        val width = viewport.right - viewport.left
        val height = viewport.bottom - viewport.top
        if (width <= 0 || height <= 0) return null
        val labeled = nodes.indices.filter { usable(nodes[it]) && role(nodes[it]) != null &&
            cx(nodes[it]) in viewport.left.toFloat()..viewport.right.toFloat() &&
            cy(nodes[it]) in viewport.top.toFloat()..viewport.bottom.toFloat() &&
            nodes[it].bounds.right - nodes[it].bounds.left <= width * .35f &&
            nodes[it].bounds.bottom - nodes[it].bounds.top <= height * .15f }
        val rails = labeled.filter { role(nodes[it]) == "home" }.flatMap { home ->
            Edge.entries.mapNotNull { edge ->
                val h = nodes[home]
                val aligned = labeled.filter { i ->
                    val n = nodes[i]
                    when (edge) {
                        Edge.LEFT -> cx(h) <= viewport.left + width * .22f && cx(n) <= viewport.left + width * .22f && abs(cx(n) - cx(h)) <= width * .045f
                        Edge.RIGHT -> cx(h) >= viewport.right - width * .22f && cx(n) >= viewport.right - width * .22f && abs(cx(n) - cx(h)) <= width * .045f
                        Edge.BOTTOM -> cy(h) >= viewport.top + height * .65f && cy(n) >= viewport.top + height * .65f && abs(cy(n) - cy(h)) <= height * .055f
                    }
                }
                val distinct = aligned.mapNotNull { role(nodes[it]) }.toSet()
                val spread = if (edge == Edge.BOTTOM) aligned.map { cx(nodes[it]) } else aligned.map { cy(nodes[it]) }
                if (distinct.size < 3 || spread.maxOrNull()!! - spread.minOrNull()!! <
                    (if (edge == Edge.BOTTOM) width * .15f else height * .12f)) null
                else Rail(edge, aligned, home)
            }
        }
        return rails.distinctBy { it.edge to it.indices.toSet() }.singleOrNull()
    }

    fun viewport(nodes: List<NodeSnapshot>): Rect? {
        val visible = nodes.filter { it.visible && it.bounds.right > it.bounds.left && it.bounds.bottom > it.bounds.top }
        if (visible.isEmpty()) return null
        return Rect().apply {
            left = visible.minOf { it.bounds.left }.coerceAtLeast(0)
            top = visible.minOf { it.bounds.top }.coerceAtLeast(0)
            right = visible.maxOf { it.bounds.right }
            bottom = visible.maxOf { it.bounds.bottom }
        }
    }

    /** Keep fallback gestures inside content rather than a tablet navigation rail. */
    fun contentViewport(nodes: List<NodeSnapshot>, viewport: Rect): Rect {
        val rail = rail(nodes, viewport) ?: return viewport
        val home = nodes[rail.homeIndex]
        val result = Rect().apply { left = viewport.left; top = viewport.top; right = viewport.right; bottom = viewport.bottom }
        when (rail.edge) {
            Edge.LEFT -> result.left = maxOf(rail.indices.maxOf { nodes[it].bounds.right }, (2 * cx(home) - viewport.left).toInt())
            Edge.RIGHT -> result.right = minOf(rail.indices.minOf { nodes[it].bounds.left }, (2 * cx(home) - viewport.right).toInt())
            Edge.BOTTOM -> result.bottom = rail.indices.minOf { nodes[it].bounds.top }
        }
        return if (result.right - result.left >= (viewport.right - viewport.left) * .4f &&
            result.bottom - result.top >= (viewport.bottom - viewport.top) * .4f) result else viewport
    }

    fun searchIndex(nodes: List<NodeSnapshot>): Int? {
        val viewport = viewport(nodes) ?: return null
        val rail = rail(nodes, viewport) ?: return null
        rail.indices.filter { role(nodes[it]) == "search" }.singleOrNull()?.let { return it }
        if (rail.edge == Edge.BOTTOM) return null // Existing bottom-nav fallback handles it.
        val home = nodes[rail.homeIndex]
        val width = viewport.right - viewport.left
        val height = viewport.bottom - viewport.top
        val nextAnchor = rail.indices.filter { cy(nodes[it]) > cy(home) }.minOfOrNull { cy(nodes[it]) } ?: return null
        // Only the first slot below Home in a proven vertical navigation rail.
        return nodes.indices.filter { i ->
            val n = nodes[i]
            val clazz = n.className.orEmpty().lowercase()
            usable(n) && role(n) == null && n.text.isNullOrBlank() && n.contentDescription.isNullOrBlank() &&
                (n.clickable || clazz.contains("button") || clazz.contains("image") || clazz.contains("view")) &&
                n.bounds.right - n.bounds.left <= width * .22f && n.bounds.bottom - n.bounds.top <= height * .1f &&
                abs(cx(n) - cx(home)) <= width * .045f && cy(n) > cy(home) + height * .025f && cy(n) < nextAnchor
        }.distinctBy { nodes[it].bounds.let { b -> listOf(b.left, b.top, b.right, b.bottom) } }.singleOrNull()
    }
}
