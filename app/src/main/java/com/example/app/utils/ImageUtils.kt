
package com.patrimonialima.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageUtils {

    fun uriABase64(
        context: Context,
        uri: Uri
    ): String? {

        return try {

            val opciones = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            // Obtener dimensiones sin cargar toda la imagen.
            context.contentResolver
                .openInputStream(uri)
                ?.use { stream ->
                    BitmapFactory.decodeStream(
                        stream,
                        null,
                        opciones
                    )
                } ?: if (
                opciones.outWidth <= 0 ||
                opciones.outHeight <= 0
            ) {
                return null
            } else {
                Unit
            }

            if (
                opciones.outWidth <= 0 ||
                opciones.outHeight <= 0
            ) {
                return null
            }

            // Reducir el consumo de memoria al decodificar.
            var sampleSize = 1

            val dimensionMayor = max(
                opciones.outWidth,
                opciones.outHeight
            )

            while (
                dimensionMayor / sampleSize >
                PoliticaImagen.MAX_DIMENSION * 2
            ) {
                sampleSize *= 2
            }

            val opcionesDecodificacion =
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }

            val bitmap = context.contentResolver
                .openInputStream(uri)
                ?.use { stream ->
                    BitmapFactory.decodeStream(
                        stream,
                        null,
                        opcionesDecodificacion
                    )
                } ?: return null

            try {
                bitmapABase64(bitmap)
            } finally {
                bitmap.recycle()
            }

        } catch (e: Exception) {

            android.util.Log.e(
                "ImageUtils",
                "Error procesando fotografía",
                e
            )

            null
        }
    }

    fun bitmapABase64(
        bitmapOriginal: Bitmap
    ): String? {

        if (bitmapOriginal.isRecycled) return null

        var bitmapActual = redimensionar(
            bitmapOriginal,
            PoliticaImagen.MAX_DIMENSION
        )

        try {

            // Máximo de seis ciclos de redimensionamiento.
            repeat(6) {

                var calidad =
                    PoliticaImagen.CALIDAD_INICIAL

                while (
                    calidad >=
                    PoliticaImagen.CALIDAD_MINIMA
                ) {

                    val bytes =
                        ByteArrayOutputStream().use { output ->

                            val comprimido =
                                bitmapActual.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    calidad,
                                    output
                                )

                            if (!comprimido) return null

                            output.toByteArray()
                        }

                    if (
                        PoliticaImagen.jpegDentroDelLimite(
                            bytes.size
                        )
                    ) {

                        val base64 =
                            PoliticaImagen.PREFIJO +
                                    Base64.encodeToString(
                                        bytes,
                                        Base64.NO_WRAP
                                    )

                        if (
                            PoliticaImagen.base64DentroDelLimite(
                                base64
                            )
                        ) {

                            android.util.Log.d(
                                "ImageUtils",
                                "JPEG: ${bytes.size} bytes | " +
                                        "Base64: ${base64.length} bytes | " +
                                        "Calidad: $calidad | " +
                                        "Dimensiones: " +
                                        "${bitmapActual.width}x" +
                                        "${bitmapActual.height}"
                            )

                            return base64
                        }
                    }

                    calidad -= 5
                }

                // Si reducir la calidad no fue suficiente,
                // reducir también las dimensiones.
                val dimensionActual = max(
                    bitmapActual.width,
                    bitmapActual.height
                )

                val nuevaDimension =
                    (dimensionActual * 0.8)
                        .roundToInt()
                        .coerceAtLeast(1)

                if (nuevaDimension >= dimensionActual) {
                    return null
                }

                val nuevoBitmap = redimensionar(
                    bitmapActual,
                    nuevaDimension
                )

                if (
                    bitmapActual !== bitmapOriginal &&
                    bitmapActual !== nuevoBitmap
                ) {
                    bitmapActual.recycle()
                }

                bitmapActual = nuevoBitmap
            }

            return null

        } finally {

            if (bitmapActual !== bitmapOriginal) {
                bitmapActual.recycle()
            }
        }
    }

    private fun redimensionar(
        bitmap: Bitmap,
        maxDimension: Int
    ): Bitmap {

        val dimensionMayor = max(
            bitmap.width,
            bitmap.height
        )

        // No ampliar imágenes pequeñas.
        if (dimensionMayor <= maxDimension) {
            return bitmap
        }

        val escala =
            maxDimension.toDouble() / dimensionMayor

        val ancho = (bitmap.width * escala)
            .roundToInt()
            .coerceAtLeast(1)

        val alto = (bitmap.height * escala)
            .roundToInt()
            .coerceAtLeast(1)

        return Bitmap.createScaledBitmap(
            bitmap,
            ancho,
            alto,
            true
        )
    }
}
