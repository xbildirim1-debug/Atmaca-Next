package com.atmacanext.app.automation

/**
 * Finds the profile circle in a drawer-header screenshot.
 * Letters are black-on-white. A photo is the colorful block on the left.
 */
internal object AvatarPixels {
    fun square(pixels: IntArray, width: Int, height: Int, minSize: Int, maxSize: Int): IntArray? {
        if (width < 8 || height < 8 || pixels.size < width * height) return null
        val smallest = minSize.coerceIn(8, height)
        val largest = maxSize.coerceIn(smallest, height)
        val sizes = listOf(smallest, (smallest + largest) / 2, largest).distinct()
        var bestScore = 0.12f
        var best: IntArray? = null
        for (size in sizes) {
            val step = (size / 5).coerceAtLeast(4)
            var top = 0
            while (top + size <= height) {
                var left = 0
                val maxLeft = (width * 0.62f).toInt()
                while (left + size <= width && left <= maxLeft) {
                    val score = score(pixels, width, left, top, size) * (1f - left / width.toFloat() * 0.4f)
                    if (score > bestScore) {
                        bestScore = score
                        best = intArrayOf(left, top, size)
                    }
                    left += step
                }
                top += step
            }
        }
        return best
    }

    private fun score(pixels: IntArray, width: Int, left: Int, top: Int, size: Int): Float {
        val step = (size / 8).coerceAtLeast(1)
        var total = 0
        var extreme = 0
        var satSum = 0.0
        var y = top
        while (y < top + size) {
            var x = left
            while (x < left + size) {
                val pixel = pixels[y * width + x]
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                val max = maxOf(red, green, blue)
                val min = minOf(red, green, blue)
                val luma = (red * 3 + green * 6 + blue) / 10
                if (luma > 235 || luma < 28) extreme++
                satSum += if (max == 0) 0.0 else (max - min).toDouble() / max
                total++
                x += step
            }
            y += step
        }
        if (total == 0) return 0f
        val sat = (satSum / total).toFloat()
        if (extreme > total * 0.78f && sat < 0.1f) return 0f
        if (sat < 0.06f) return 0f
        return sat
    }
}
