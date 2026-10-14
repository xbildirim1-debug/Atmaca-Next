package com.atmacanext.app.automation

import android.graphics.Rect

/** Picks the current account photo on the X account drawer. Ignores tiny badges. */
internal object AccountAvatarEvidence {
    data class Candidate(
        val label: String,
        val viewId: String,
        val className: String,
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int,
        val visible: Boolean,
    )

    fun bounds(nodes: List<NodeSnapshot>, avatarPx: Int = 160): Rect? {
        val mapped = nodes.map { node ->
            Candidate(
                label = listOfNotNull(node.contentDescription, node.text).joinToString(" "),
                viewId = node.viewId.orEmpty(),
                className = node.className.orEmpty(),
                left = node.bounds.left,
                top = node.bounds.top,
                width = node.bounds.right - node.bounds.left,
                height = node.bounds.bottom - node.bounds.top,
                visible = node.visible,
            )
        }
        val chosen = pick(mapped) ?: besideHandle(mapped, avatarPx) ?: return null
        return Rect(chosen.left, chosen.top, chosen.left + chosen.width, chosen.top + chosen.height)
    }

    fun shots(nodes: List<NodeSnapshot>, avatarPx: Int): List<Rect> {
        val mapped = nodes.map { node ->
            Candidate(
                label = listOfNotNull(node.contentDescription, node.text).joinToString(" "),
                viewId = node.viewId.orEmpty(),
                className = node.className.orEmpty(),
                left = node.bounds.left,
                top = node.bounds.top,
                width = node.bounds.right - node.bounds.left,
                height = node.bounds.bottom - node.bounds.top,
                visible = node.visible,
            )
        }
        return candidates(mapped, avatarPx).map { Rect(it.left, it.top, it.left + it.width, it.top + it.height) }
    }

    /** The circle is either left of @handle or above it. Never the text itself. */
    fun besideHandle(nodes: List<Candidate>, avatarPx: Int): Candidate? = candidates(nodes, avatarPx).firstOrNull()

    fun candidates(nodes: List<Candidate>, avatarPx: Int): List<Candidate> {
        val handle = nodes.firstOrNull { node ->
            node.visible && node.label.none { it == '\n' } && (
                AccountSwitcherInspector.dedicatedHandle(node.label) != null ||
                    node.label.split(Regex("[\\s,·]")).any { AccountSwitcherInspector.dedicatedHandle(it.trim()) != null }
                )
        } ?: return emptyList()
        val edge = avatarPx.coerceIn(72, 220)
        val found = ArrayList<Candidate>()
        pick(nodes)?.let { if (!overlaps(it, handle)) found += it }
        nodes.filter { node ->
            node.visible && node.label.isBlank() &&
                node.width in edge / 2..edge + 80 && node.height in edge / 2..edge + 80 &&
                kotlin.math.abs(node.width - node.height) <= node.width / 4 &&
                !overlaps(node, handle) &&
                (node.left + node.width <= handle.left - 8 || node.top + node.height <= handle.top - 8)
        }.maxByOrNull { it.width }?.let(found::add)
        val gap = 16
        if (handle.left >= edge + gap) {
            val left = handle.left - gap - edge
            val top = (handle.top + handle.height / 2 - edge / 2).coerceAtLeast(0)
            found += Candidate("left-of-handle", "", "", left, top, edge, edge, true)
        }
        val aboveTop = (handle.top - handle.height - gap - edge).coerceAtLeast(0)
        if (aboveTop + edge <= handle.top - 8) {
            found += Candidate("above-handle", "", "", handle.left.coerceAtLeast(0), aboveTop, edge, edge, true)
        }
        return found.distinctBy { "${it.left}:${it.top}:${it.width}" }
    }

    private fun overlaps(node: Candidate, handle: Candidate): Boolean {
        val right = node.left + node.width
        val bottom = node.top + node.height
        val handleRight = handle.left + handle.width
        val handleBottom = handle.top + handle.height
        return node.left < handleRight && right > handle.left && node.top < handleBottom && bottom > handle.top
    }

    fun pick(nodes: List<Candidate>): Candidate? {
        val labeled = nodes.firstOrNull { node ->
            val label = node.label.lowercase()
            node.visible && node.width >= 48 && node.height >= 48 &&
                (label.contains("profil foto") || label.contains("profile photo") ||
                    label.contains("profile picture") || label.contains("avatar"))
        }
        if (labeled != null) return labeled
        return nodes.filter { node ->
            val id = node.viewId.lowercase()
            node.visible && node.width in 72..320 && node.height in 72..320 &&
                kotlin.math.abs(node.width - node.height) <= node.width / 3 && node.top < 900 &&
                (id.contains("avatar") || id.contains("profile_image") || id.contains("profileimage"))
        }.minByOrNull { it.top }
    }
}
