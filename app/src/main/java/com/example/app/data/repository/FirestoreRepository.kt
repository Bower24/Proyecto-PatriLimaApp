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
import android.content.Context
import android.util.Base64
import com.patrimonialima.app.data.local.ReportesPendientesLocal

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



    fun crearReporte(
        context: Context,
        idUsuario: String,
        descripcion: String,
        direccion: String,
        severidad: Severidad,
        fotoBase64: String? = null,
        latitud: Double = -12.0465,
        longitud: Double = -77.0305
    ): Result<String> {

        var idRegistrado: String? = null

        return try {
            // Firestore admite como máximo 1 MiB por documento.
            // Dejamos margen para los demás campos.
            if (fotoBase64 != null &&
                fotoBase64.toByteArray(Charsets.UTF_8).size > 750_000
            ) {
                return Result.failure(
                    IllegalArgumentException(
                        "La fotografía es demasiado grande. " +
                                "Selecciona una imagen más pequeña."
                    )
                )
            }

            val fechaActual = SimpleDateFormat(
                "dd 'de' MMMM 'de' yyyy",
                Locale("es", "PE")
            ).format(Date())

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

            // Registrar primero el identificador pendiente.
            val registrado = ReportesPendientesLocal.registrar(
                context,
                idUsuario,
                nuevoId
            )

            if (!registrado) {
                return Result.failure(
                    IllegalStateException(
                        "No se pudo registrar el reporte pendiente localmente."
                    )
                )
            }

            idRegistrado = nuevoId

            // No usar await(): sin internet esperaría al servidor.
            reporteRef.set(data)
                .addOnFailureListener { error ->
                    android.util.Log.e(
                        "FirestoreRepository",
                        "Error enviando reporte $nuevoId",
                        error
                    )

                    // Se conserva el ID pendiente para poder
                    // mostrar el error y revisar el reporte.
                    ReportesPendientesLocal.registrarError(
                        context,
                        idUsuario,
                        nuevoId
                    )
                }

            Result.success(nuevoId)

        } catch (e: Exception) {
            idRegistrado?.let { id ->
                ReportesPendientesLocal.quitar(
                    context,
                    idUsuario,
                    id
                )
            }
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