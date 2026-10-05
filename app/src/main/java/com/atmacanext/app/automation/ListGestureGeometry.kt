package com.atmacanext.app.automation

import android.graphics.Rect

/** Clip Compose's virtual/off-screen rows before building an Android gesture. */
internal object ListGestureGeometry {
    data class Stroke(val startX: Float, val startY: Float, val endX: Float, val endY: Float) {
        fun valid(): Boolean = listOf(startX, startY, endX, endY).all { it.isFinite() && it >= 0f }
    }

    fun viewport(bounds: Rect, displayWidth: Int = 0, displayHeight: Int = 0): Rect? {
        val left = maxOf(0, bounds.left)
        val top = maxOf(0, bounds.top)
        val right = if (displayWidth > 0) minOf(bounds.right, displayWidth) else bounds.right
        val bottom = if (displayHeight > 0) minOf(bounds.bottom, displayHeight) else bounds.bottom
        if (right - left < 2 || bottom - top < 2) return null
        return Rect().apply { this.left = left; this.top = top; this.right = right; this.bottom = bottom }
    }

    fun swipe(bounds: Rect, rows: List<Rect>, forward: Boolean, horizontal: Boolean = false): Stroke? {
        val screen = viewport(bounds) ?: return null
        val width = screen.right - screen.left
        val height = screen.bottom - screen.top
        if (horizontal) {
            val y = screen.top + height * 0.55f
            val a = screen.left + width * 0.82f
            val b = screen.left + width * 0.18f
            return if (forward) Stroke(a, y, b, y) else Stroke(b, y, a, y)
        }
        val safeTop = screen.top + height * 0.15f
        val safeBottom = screen.top + height * 0.88f
        val centers = rows.mapNotNull { row ->
            val top = maxOf(row.top, screen.top)
            val bottom = minOf(row.bottom, screen.bottom)
            if (bottom <= top || row.right <= screen.left || row.left >= screen.right) null
            else ((top.toFloat() + bottom) / 2f).coerceIn(safeTop, safeBottom)
        }
        var top = centers.minOrNull() ?: screen.top + height * 0.30f
        var bottom = centers.maxOrNull() ?: screen.top + height * 0.78f
        if (bottom - top < height * 0.24f) {
            top = screen.top + height * 0.30f
            bottom = screen.top + height * 0.78f
        }
        if (bottom <= top) return null
        val x = screen.left + width * 0.5f
        return if (forward) Stroke(x, bottom, x, top) else Stroke(x, top, x, bottom)
    }
}
