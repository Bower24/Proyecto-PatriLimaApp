package com.patrimonialima.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.patrimonialima.app.domain.model.BienPatrimonial
import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Reporte
import com.patrimonialima.app.domain.model.Severidad
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance().apply {
        // Habilitar persistencia offline estándar compatible con cualquier versión del SDK
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestoreSettings = settings
        } catch (_: Exception) {
            // Ya inicializado previamente
        }
    }

    suspend fun crearReporte(
        idUsuario: String,
        descripcion: String,
        direccion: String,
        severidad: Severidad,
        fotoBase64: String? = null,
        latitud: Double = -12.0465,
        longitud: Double = -77.0305
    ): Result<Unit> {
        return try {
            val fechaActual = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "PE")).format(Date())
            val nuevoId = UUID.randomUUID().toString()
            val reporteRef = db.collection("reportes").document(nuevoId)

            val data = hashMapOf(
                "id" to nuevoId,
                "idUsuario" to idUsuario,
                "descripcion" to descripcion,
                "direccion" to direccion,
                "severidad" to severidad.name,
                "estado" to EstadoReporte.ENVIADO.name,
                "fecha" to fechaActual,
                "fechaCreacionMillis" to System.currentTimeMillis(),
                "likes_count" to 0L,
                "latitud" to latitud,
                "longitud" to longitud,
                "fotoBase64" to fotoBase64
            )

            // Se envía a la caché local de inmediato. Sin internet, queda en cola y se sube sola al reconectar
            reporteRef.set(data)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerTodosLosReportes(): List<Reporte> {
        return try {
            val snapshot = db.collection("reportes").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val severidadStr = doc.getString("severidad")?.uppercase().orEmpty()
                    val estadoStr = doc.getString("estado")?.uppercase().orEmpty()

                    val severidadEnum = when {
                        severidadStr.contains("SEVERO") -> Severidad.SEVERO
                        severidadStr.contains("MODERADO") -> Severidad.MODERADO
                        else -> Severidad.LEVE
                    }

                    val estadoEnum = when {
                        estadoStr.contains("REVISION") -> EstadoReporte.EN_REVISION
                        estadoStr.contains("REVISADO") || estadoStr.contains("ATENDIDO") -> EstadoReporte.REVISADO
                        else -> EstadoReporte.ENVIADO
                    }

                    val likes = when (val l = doc.get("likes_count") ?: doc.get("likes")) {
                        is Number -> l.toInt()
                        is String -> l.toIntOrNull() ?: 0
                        else -> 0
                    }

                    Reporte(
                        id = doc.id,
                        idUsuario = doc.getString("idUsuario").orEmpty(),
                        idBien = doc.getString("idBien"),
                        titulo = doc.getString("titulo").orEmpty(),
                        descripcion = doc.getString("descripcion").orEmpty(),
                        direccion = doc.getString("direccion").orEmpty(),
                        latitud = doc.getDouble("latitud") ?: 0.0,
                        longitud = doc.getDouble("longitud") ?: 0.0,
                        fecha = doc.getString("fecha").orEmpty(),
                        fechaCreacionMillis = doc.getLong("fechaCreacionMillis") ?: 0L,
                        severidad = severidadEnum,
                        estado = estadoEnum,
                        likes = likes,
                        fotoUrl = doc.getString("fotoBase64")
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obtenerBienesPatrimoniales(): List<BienPatrimonial> {
        return try {
            val snapshot = db.collection("bienes_patrimoniales").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    BienPatrimonial(
                        id = doc.id,
                        nombre = doc.getString("nombre").orEmpty(),
                        tipo = doc.getString("tipo").orEmpty(),
                        distrito = doc.getString("distrito") ?: "Cercado de Lima",
                        latitud = doc.getDouble("latitud") ?: 0.0,
                        longitud = doc.getDouble("longitud") ?: 0.0,
                        descripcion = doc.getString("descripcion").orEmpty(),
                        anio = doc.getString("anio").orEmpty(),
                        normaLegal = doc.getString("normaLegal").orEmpty()
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obtenerReportesDeUsuario(uid: String): List<Reporte> {
        return try {
            val coleccion = db.collection("reportes")


            val consultas = listOf(
                coleccion.whereEqualTo("idUsuario", uid).get(),
                coleccion.whereEqualTo("id_usuario", uid).get(),
                coleccion.whereEqualTo("userId", uid).get()
            )

            val documentos = consultas.map { it.await() }
                .flatMap { it.documents }
                .distinctBy { it.id }

            documentos.mapNotNull { doc ->
                try {
                    val severidadStr = (
                            doc.getString("severidad") ?: "LEVE"
                            ).uppercase()

                    val estadoStr = (
                            doc.getString("estado") ?: "ENVIADO"
                            ).uppercase()

                    val severidadEnum = when {
                        severidadStr.contains("SEVERO") ->
                            Severidad.SEVERO

                        severidadStr.contains("MODERADO") ->
                            Severidad.MODERADO

                        else -> Severidad.LEVE
                    }

                    val estadoEnum = when {
                        estadoStr.contains("REVISION") ->
                            EstadoReporte.EN_REVISION

                        estadoStr.contains("REVISADO") ||
                                estadoStr.contains("ATENDIDO") ->
                            EstadoReporte.REVISADO

                        else -> EstadoReporte.ENVIADO
                    }

                    val likes = when (
                        val valor = doc.get("likes_count") ?: doc.get("likes")
                    ) {
                        is Number -> valor.toInt()
                        is String -> valor.toIntOrNull() ?: 0
                        else -> 0
                    }

                    Reporte(
                        id = doc.id,
                        idUsuario = doc.getString("idUsuario")
                            ?: doc.getString("id_usuario")
                            ?: doc.getString("userId")
                            ?: uid,

                        idBien = doc.getString("idBien")
                            ?: doc.getString("id_bien"),

                        titulo = doc.getString("titulo").orEmpty(),
                        descripcion = doc.getString("descripcion").orEmpty(),
                        direccion = doc.getString("direccion").orEmpty(),

                        latitud = doc.getDouble("latitud")
                            ?: (doc.getGeoPoint("ubicacion")?.latitude ?: 0.0),

                        longitud = doc.getDouble("longitud")
                            ?: (doc.getGeoPoint("ubicacion")?.longitude ?: 0.0),

                        fecha = doc.getString("fecha")
                            ?: doc.getTimestamp("fecha_creacion")?.toDate()?.toString()
                            ?: "",

                        fechaCreacionMillis = doc.getLong("fechaCreacionMillis")
                            ?: doc.getTimestamp("fecha_creacion")?.toDate()?.time
                            ?: 0L,

                        severidad = severidadEnum,
                        estado = estadoEnum,
                        likes = likes,

                        fotoUrl = doc.getString("fotoBase64")
                            ?: doc.getString("foto_url")
                    )
                } catch (e: Exception) {
                    android.util.Log.e(
                        "FirestoreRepository",
                        "Error procesando reporte ${doc.id}",
                        e
                    )
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "FirestoreRepository",
                "Error obteniendo reportes del usuario",
                e
            )
            emptyList()
        }
    }


}