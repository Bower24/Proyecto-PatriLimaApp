package com.patrimonialima.app.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Sube la fotografía adjunta a un reporte (RF02) a Cloud Storage y
 * devuelve la URL de descarga, que luego se guarda en el campo
 * "foto_url" del documento en la colección "reportes".
 */
class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun subirFotoReporte(uri: Uri): Result<String> {
        return try {
            val nombreArchivo = "reportes/${UUID.randomUUID()}.jpg"
            val referencia = storage.reference.child(nombreArchivo)
            referencia.putFile(uri).await()
            val urlDescarga = referencia.downloadUrl.await()
            Result.success(urlDescarga.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
