package com.patrimonialima.app.data.repository

import android.content.Context
import android.net.Uri
import com.patrimonialima.app.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sube la fotografía adjunta a un reporte (RF02) a Cloud Storage y
 * devuelve la URL de descarga, que luego se guarda en el campo
 * "foto_url" del documento en la colección "reportes".
 */

class StorageRepository {
    suspend fun subirFoto(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        ImageUtils.uriABase64(context, uri)
            ?: throw Exception("No se pudo procesar la imagen seleccionada")
    }
}
