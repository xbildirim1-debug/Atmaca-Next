package com.atmacanext.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

data class PilotImage(val path: String, val mimeType: String = "image/jpeg")
object PilotImages {
    fun import(context: Context, uri: Uri): PilotImage {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream(); val buffer = ByteArray(16_384)
            while (true) { val read = input.read(buffer); if (read < 0) break
                require(output.size() + read <= 12 * 1024 * 1024) { "Resim en fazla 12 MB olabilir" }; output.write(buffer, 0, read) }
            output.toByteArray()
        } ?: error("Resim okunamadı")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Geçerli bir resim seç" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Resim açılamadı")
        val orientation = runCatching { ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
        val matrix = Matrix().apply { when (orientation) {
            2 -> postScale(-1f, 1f); 3 -> postRotate(180f); 4 -> postScale(1f, -1f)
            5 -> { postRotate(90f); postScale(-1f, 1f) }; 6 -> postRotate(90f)
            7 -> { postRotate(270f); postScale(-1f, 1f) }; 8 -> postRotate(270f)
        } }
        val bitmap = if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val directory = File(context.applicationContext.filesDir, "pilot_images").apply { mkdirs() }
        val target = File(directory, UUID.randomUUID().toString() + ".jpg")
        try { target.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it)) }; require(target.length() <= 3 * 1024 * 1024) }
        catch (error: Exception) { target.delete(); throw error }
        finally { if (bitmap !== decoded) bitmap.recycle(); decoded.recycle() }
        return PilotImage(target.absolutePath)
    }
    fun bytes(context: Context, image: PilotImage): ByteArray {
        val directory = File(context.applicationContext.filesDir, "pilot_images").canonicalFile
        val file = File(image.path).canonicalFile
        require(file.parentFile == directory && file.length() in 1..(3L * 1024 * 1024)) { "Pilot resmi bulunamadı" }
        return file.readBytes()
    }
}
