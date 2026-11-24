
package com.patrimonialima.app.utils

object PoliticaImagen {

    const val MAX_DIMENSION = 640

    const val CALIDAD_INICIAL = 65

    const val CALIDAD_MINIMA = 20

    const val MAX_JPEG_BYTES = 140_000

    const val MAX_BASE64_BYTES = 195_000

    const val PREFIJO = "data:image/jpeg;base64,"

    fun tamanoBase64Teorico(
        bytesOriginales: Int,
        incluirPrefijo: Boolean = true
    ): Long {

        require(bytesOriginales >= 0)

        val tamano =
            4L * ((bytesOriginales.toLong() + 2L) / 3L)

        return if (incluirPrefijo) {
            tamano + PREFIJO.length
        } else {
            tamano
        }
    }

    fun jpegDentroDelLimite(
        cantidadBytes: Int
    ): Boolean {

        return cantidadBytes in 1..MAX_JPEG_BYTES
    }

    fun base64DentroDelLimite(
        fotoBase64: String?
    ): Boolean {

        // La fotografía es opcional.
        if (fotoBase64 == null) return true

        if (fotoBase64.isBlank()) return false

        return fotoBase64
            .toByteArray(Charsets.UTF_8)
            .size <= MAX_BASE64_BYTES
    }
}
