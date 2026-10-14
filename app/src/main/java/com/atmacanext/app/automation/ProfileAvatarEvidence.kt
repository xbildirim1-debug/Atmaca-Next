package com.atmacanext.app.automation

import android.graphics.Rect

/** The large circle on an X profile, above the follower line. Not the @handle text. */
internal object ProfileAvatarEvidence {
    fun bounds(nodes: List<NodeSnapshot>, screenWidth: Int, minPx: Int, maxPx: Int): Rect? {
        val statsTop = nodes.filter { it.visible }.mapNotNull { node ->
            val label = listOfNotNull(node.text, node.contentDescription).joinToString(" ").lowercase()
            if (label.contains("takipçi") || label.contains("takipçi") || label.contains("follower")) node.bounds.top else null
        }.minOrNull() ?: Int.MAX_VALUE
        val squares = nodes.filter { node ->
            val width = node.bounds.width()
            val height = node.bounds.height()
            node.visible && width in minPx..maxPx && height in minPx..maxPx &&
                kotlin.math.abs(width - height) <= width / 3 &&
                node.bounds.left < screenWidth / 2 &&
                node.bounds.top > minPx / 4 &&
                node.bounds.bottom <= statsTop + minPx / 5
        }
        val labeled = squares.firstOrNull { node ->
            val label = listOfNotNull(node.text, node.contentDescription).joinToString(" ").lowercase()
            label.contains("profil foto") || label.contains("profile photo") || label.contains("profile picture") || label.contains("profile image")
        }
        val chosen = labeled ?: squares.maxByOrNull { it.bounds.width() } ?: return null
        return Rect(chosen.bounds)
    }
}
