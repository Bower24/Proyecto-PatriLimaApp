package com.patrimonialima.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.patrimonialima.app.domain.model.BienPatrimonial
import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Reporte
import com.patrimonialima.app.domain.model.Severidad
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Acceso a las colecciones "bienes_patrimoniales" y "reportes" de Cloud
 * Firestore, con el esquema definido en la sección 4.4 de la tesis.
 *
 * NOTA: la geolocalización real del reporte (a partir del GPS del
 * dispositivo o de geocodificar el texto de "dirección") no está
 * implementada todavía — por ahora se guarda un punto por defecto en el
 * centro del Cercado de Lima. Ver README para el detalle de este pendiente.
 */
class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val coleccionBienes = db.collection("bienes_patrimoniales")
    private val coleccionReportes = db.collection("reportes")

    private val formatoFecha = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "PE"))

    suspend fun obtenerBienesPatrimoniales(): List<BienPatrimonial> {
        val snapshot = coleccionBienes.get().await()
        return snapshot.documents.mapNotNull { it.toBien() }
    }

    suspend fun crearReporte(
        idUsuario: String,
        descripcion: String,
        direccion: String,
        severidad: Severidad,
        fotoUrl: String?,
        idBien: String? = null,
        latitud: Double = -12.0555,
        longitud: Double = -77.0350
    ): Result<String> {
        return try {
            val datos = hashMapOf(
                "id_usuario" to idUsuario,
                "id_bien" to idBien,
                "descripcion" to descripcion,
                "direccion" to direccion,
                "ubicacion" to GeoPoint(latitud, longitud),
                "foto_url" to fotoUrl,
                "severidad" to severidad.name.lowercase(),
                "estado" to EstadoReporte.ENVIADO.name.lowercase(),
                "likes_count" to 0,
                "fecha_creacion" to Timestamp.now()
            )
            val referencia = coleccionReportes.add(datos).await()
            Result.success(referencia.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerReportesDeUsuario(idUsuario: String): List<Reporte> {
        val snapshot = coleccionReportes
            .whereEqualTo("id_usuario", idUsuario)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toReporte() }
    }

    /** Usado por el mapa (RF03) y, más adelante, por el panel administrativo (RF05). */
    suspend fun obtenerTodosLosReportes(): List<Reporte> {
        val snapshot = coleccionReportes.get().await()
        return snapshot.documents.mapNotNull { it.toReporte() }
    }

    suspend fun actualizarEstadoReporte(idReporte: String, nuevoEstado: EstadoReporte): Result<Unit> {
        return try {
            coleccionReportes.document(idReporte)
                .update("estado", nuevoEstado.name.lowercase())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toBien(): BienPatrimonial? {
        val nombre = getString("nombre") ?: return null
        val geo = getGeoPoint("ubicacion")
        return BienPatrimonial(
            id = id,
            nombre = nombre,
            tipo = getString("tipo") ?: "",
            distrito = getString("distrito") ?: "",
            latitud = geo?.latitude ?: 0.0,
            longitud = geo?.longitude ?: 0.0,
            descripcion = getString("descripcion") ?: "",
            anio = getString("anio") ?: "",
            normaLegal = getString("norma_legal") ?: ""
        )
    }

    private fun DocumentSnapshot.toReporte(): Reporte? {
        val descripcion = getString("descripcion") ?: return null
        val geo = getGeoPoint("ubicacion")
        val severidadTexto = getString("severidad") ?: "leve"
        val estadoTexto = getString("estado") ?: "enviado"
        val timestamp = getTimestamp("fecha_creacion")

        val severidad = Severidad.values()
            .find { it.name.equals(severidadTexto, ignoreCase = true) } ?: Severidad.LEVE
        val estado = EstadoReporte.values()
            .find { it.name.equals(estadoTexto, ignoreCase = true) } ?: EstadoReporte.ENVIADO

        return Reporte(
            id = id,
            idUsuario = getString("id_usuario") ?: "",
            idBien = getString("id_bien"),
            titulo = descripcion.take(60),
            direccion = getString("direccion") ?: "",
            fecha = timestamp?.toDate()?.let { formatoFecha.format(it) } ?: "",
            severidad = severidad,
            estado = estado,
            likes = (getLong("likes_count") ?: 0L).toInt(),
            latitud = geo?.latitude ?: 0.0,
            longitud = geo?.longitude ?: 0.0,
            fotoUrl = getString("foto_url"),
            fechaCreacionMillis = timestamp?.toDate()?.time ?: 0L
        )
    }
}
