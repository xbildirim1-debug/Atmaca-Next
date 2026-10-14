package com.atmacanext.app.data.settings

import android.content.Context
import android.graphics.Bitmap
import java.io.File

/** Profile photos captured while scanning X accounts. One JPEG per handle. */
object AccountAvatarStore {
    fun file(context: Context, username: String): File {
        val handle = username.removePrefix("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "account" }
        val dir = File(context.applicationContext.filesDir, "avatars").apply { mkdirs() }
        return File(dir, "$handle.jpg")
    }

    fun clear(context: Context) {
        File(context.applicationContext.filesDir, "avatars").listFiles()?.forEach { it.delete() }
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** Removes photos that were copied onto more than one account. Unique photos stay. */
    fun dropCopies(context: Context) {
        val dir = File(context.applicationContext.filesDir, "avatars")
        val files = dir.listFiles()?.filter { it.isFile && it.length() > 0 } ?: return
        val marked = files.mapNotNull { file ->
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: return@mapNotNull null
            val mark = fingerprint(bitmap)
            bitmap.recycle()
            file to mark
        }
        marked.groupBy { it.second }.values.filter { it.size > 1 }.flatten().forEach { it.first.delete() }
    }

    /** A crop of the @handle line is mostly white with almost no color. A photo is not. */
    fun looksLikeText(bitmap: Bitmap): Boolean {
        val step = (bitmap.width / 16).coerceAtLeast(1)
        var total = 0
        var white = 0
        var color = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                val max = maxOf(red, green, blue)
                val min = minOf(red, green, blue)
                if (red > 228 && green > 228 && blue > 228) white++
                else if (max - min > 28) color++
                total++
                x += step
            }
            y += step
        }
        return total > 0 && white > total * 0.5 && color < total * 0.12
    }

    fun matchesOther(context: Context, username: String, bitmap: Bitmap): Boolean {
        val fitted = fit(bitmap)
        val mark = fingerprint(fitted)
        if (fitted !== bitmap) fitted.recycle()
        val handle = key(username)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.all.any { (name, value) -> name.startsWith("hash:") && name != "hash:$handle" && value == mark }
    }

    fun write(context: Context, username: String, bitmap: Bitmap): Boolean {
        val handle = key(username)
        val fitted = fit(bitmap)
        val mark = fingerprint(fitted)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val owner = prefs.all.entries.firstOrNull { (name, value) ->
            name.startsWith("hash:") && name != "hash:$handle" && value == mark
        }?.key?.removePrefix("hash:")
        if (owner != null) {
            fitted.recycle()
            file(context, username).delete()
            return false
        }
        file(context, username).outputStream().use { fitted.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        fitted.recycle()
        prefs.edit().putLong("hash:$handle", mark).apply()
        return true
    }

    /** Drops the white margin around a round photo, then fills a square so the circle clip sits flush. */
    internal fun fit(bitmap: Bitmap): Bitmap {
        val square = centerSquare(bitmap)
        val box = contentBounds(square)
        val wide = box != null && box[2] > square.width * 0.72 && box[3] > square.height * 0.72
        val tight = if (!wide || box == null) square else Bitmap.createBitmap(square, box[0], box[1], box[2], box[3])
        val filled = if (tight.width == tight.height) tight else centerSquare(tight)
        val scaled = if (filled.width == 256) filled else Bitmap.createScaledBitmap(filled, 256, 256, true)
        if (square !== bitmap && square !== tight && square !== filled && square !== scaled) square.recycle()
        if (tight !== square && tight !== filled && tight !== scaled) tight.recycle()
        if (filled !== scaled) filled.recycle()
        return scaled
    }

    private fun centerSquare(bitmap: Bitmap): Bitmap {
        val side = bitmap.width.coerceAtMost(bitmap.height)
        if (bitmap.width == side && bitmap.height == side) return bitmap
        val left = (bitmap.width - side) / 2
        val top = (bitmap.height - side) / 2
        return Bitmap.createBitmap(bitmap, left, top, side, side)
    }

    private fun contentBounds(bitmap: Bitmap): IntArray? {
        fun blank(pixel: Int): Boolean {
            val alpha = pixel ushr 24
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            return alpha < 16 || (red > 245 && green > 245 && blue > 245)
        }
        fun rowBlank(y: Int) = (0 until bitmap.width step 2.coerceAtLeast(1)).all { blank(bitmap.getPixel(it, y)) }
        fun colBlank(x: Int) = (0 until bitmap.height step 2.coerceAtLeast(1)).all { blank(bitmap.getPixel(x, it)) }
        var top = 0
        var bottom = bitmap.height - 1
        var left = 0
        var right = bitmap.width - 1
        while (top < bottom && rowBlank(top)) top++
        while (bottom > top && rowBlank(bottom)) bottom--
        while (left < right && colBlank(left)) left++
        while (right > left && colBlank(right)) right--
        if (left == 0 && top == 0 && right == bitmap.width - 1 && bottom == bitmap.height - 1) return null
        val width = (right - left + 1).coerceAtLeast(1)
        val height = (bottom - top + 1).coerceAtLeast(1)
        if (width < bitmap.width / 3 || height < bitmap.height / 3) return null
        return intArrayOf(left, top, width, height)
    }

    internal fun fingerprint(bitmap: Bitmap): Long {
        var hash = 1125899906842597L
        val stepX = (bitmap.width / 8).coerceAtLeast(1)
        val stepY = (bitmap.height / 8).coerceAtLeast(1)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                hash = hash * 31 + bitmap.getPixel(x, y)
                x += stepX
            }
            y += stepY
        }
        return hash
    }

    private fun key(username: String) =
        username.removePrefix("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "account" }

    private const val PREFS = "atmaca_avatar_hash_v1"
}
