package com.patrimonialima.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun uriABase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmapOriginal = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Redimensionar a un tamaño máximo manejable (640 px)
            val maxDimension = 640
            val ratio = bitmapOriginal.width.toFloat() / bitmapOriginal.height.toFloat()
            val targetWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
            val targetHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension

            val scaledBitmap = Bitmap.createScaledBitmap(bitmapOriginal, targetWidth, targetHeight, true)

            val outputStream = ByteArrayOutputStream()
            // Compresión al 65% para mantener el peso entre 40 KB y 80 KB
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
            val bytes = outputStream.toByteArray()

            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}