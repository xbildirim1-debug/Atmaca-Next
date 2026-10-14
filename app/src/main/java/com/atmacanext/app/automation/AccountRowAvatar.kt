package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Locks each photo to the list row that contains only that @handle.
 * A crop never crosses into the next account.
 */
internal object AccountRowAvatar {
    data class Item(
        val label: String,
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int,
        val className: String = "",
        val children: List<Item> = emptyList(),
    )

    data class Row(val handle: String, val left: Int, val top: Int, val width: Int, val height: Int)

    fun read(root: AccessibilityNodeInfo?): Item? {
        if (root == null) return null
        return readNode(root, 0)
    }

    fun lockedRows(root: Item?, minHeight: Int, maxHeight: Int, screenWidth: Int): List<Row> {
        if (root == null) return emptyList()
        val rows = mutableMapOf<String, Item>()
        walk(root) { item ->
            if (item.height !in minHeight..maxHeight) return@walk
            if (item.width < screenWidth * 0.55f) return@walk
            val handles = handlesUnder(item)
            if (handles.size != 1) return@walk
            val handle = handles.first()
            val current = rows[handle]
            if (current == null || item.height < current.height) rows[handle] = item
        }
        return rows.map { (handle, row) ->
            val photo = imageInside(row)
            if (photo != null) Row(handle, photo.left, photo.top, photo.width, photo.height)
            else squareBeside(handle, row.left, row.top, row.height, avatarFallback(row.height))
        }
    }

    /** Used when X does not expose a real row. The square stays in this handle's vertical band. */
    fun beside(nodes: List<Item>, avatarPx: Int): List<Row> {
        val size = avatarPx.coerceIn(72, 220)
        val handles = nodes.mapNotNull { node ->
            val handle = node.label.split(Regex("[\\s,·]")).firstNotNullOfOrNull { part ->
                AccountSwitcherInspector.dedicatedHandle(part.trim())
            } ?: AccountSwitcherInspector.dedicatedHandle(node.label)
            handle?.let { it to node }
        }.distinctBy { it.first }.sortedBy { it.second.top }
        return handles.mapIndexed { index, (handle, text) ->
            val center = text.top + text.height / 2
            val prev = handles.getOrNull(index - 1)?.let { it.second.top + it.second.height / 2 }
            val next = handles.getOrNull(index + 1)?.let { it.second.top + it.second.height / 2 }
            val bandTop = prev?.let { (it + center) / 2 } ?: (center - size)
            val bandBottom = next?.let { (center + it) / 2 } ?: (center + size)
            val edge = size.coerceAtMost(((bandBottom - bandTop) * 0.72f).toInt().coerceAtLeast(48))
            val gap = (edge / 6).coerceAtLeast(8)
            val left = (text.left - edge - gap).coerceAtLeast(0)
            var top = center - edge / 2
            if (top < bandTop) top = bandTop
            if (top + edge > bandBottom) top = (bandBottom - edge).coerceAtLeast(bandTop)
            Row(handle, left, top, edge, edge)
        }
    }

    private fun avatarFallback(rowHeight: Int) = (rowHeight * 0.62f).toInt().coerceIn(36, rowHeight - 8)

    private fun squareBeside(handle: String, rowLeft: Int, rowTop: Int, rowHeight: Int, edge: Int): Row {
        val top = rowTop + (rowHeight - edge) / 2
        val left = rowLeft + ((rowHeight - edge) / 2).coerceAtLeast(4)
        return Row(handle, left, top, edge, edge)
    }

    private fun readNode(node: AccessibilityNodeInfo, depth: Int): Item {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        val children = if (depth >= 8) emptyList() else buildList {
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { add(readNode(it, depth + 1)) }
            }
        }
        return Item(
            label = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" "),
            left = rect.left,
            top = rect.top,
            width = (rect.right - rect.left).coerceAtLeast(0),
            height = (rect.bottom - rect.top).coerceAtLeast(0),
            className = node.className?.toString().orEmpty(),
            children = children,
        )
    }

    private fun walk(item: Item, visit: (Item) -> Unit) {
        visit(item)
        item.children.forEach { walk(it, visit) }
    }

    private fun handlesUnder(item: Item): Set<String> {
        val found = linkedSetOf<String>()
        fun scan(node: Item) {
            if (found.size > 1) return
            AccountSwitcherInspector.dedicatedHandle(node.label)?.let(found::add)
            node.label.split(Regex("[\\s,·]")).forEach { part ->
                AccountSwitcherInspector.dedicatedHandle(part.trim())?.let(found::add)
            }
            if (found.size > 1) return
            node.children.forEach(::scan)
        }
        scan(item)
        return found
    }

    private fun imageInside(row: Item): Item? {
        var best: Item? = null
        fun scan(node: Item) {
            val square = node.width in 32..row.height && node.height in 32..row.height &&
                kotlin.math.abs(node.width - node.height) <= node.width / 3 &&
                node.left >= row.left && node.top >= row.top &&
                node.left + node.width <= row.left + row.width &&
                node.top + node.height <= row.top + row.height &&
                node.left + node.width <= row.left + row.width / 2 &&
                (node.className.contains("Image", true) || node.label.isBlank() ||
                    node.label.contains("profil", true) || node.label.contains("profile", true) ||
                    node.label.contains("avatar", true))
            if (square && node !== row && (best == null || node.width > best!!.width)) best = node
            node.children.forEach(::scan)
        }
        scan(row)
        return best
    }
}
