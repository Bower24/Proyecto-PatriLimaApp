package com.patrimonialima.app.domain.model

import com.google.android.gms.maps.model.LatLng

/**
 * Estados del ciclo de vida de un reporte (campo "estado" de la colección
 * reportes, sección 4.4 del Capítulo IV).
 */
enum class EstadoReporte(val label: String) {
    ENVIADO("Enviado"),
    EN_REVISION("En Revisión"),
    REVISADO("Revisado")
}

/**
 * Nivel de severidad de un daño reportado (campo "severidad" de la
 * colección reportes, sección 4.4 del Capítulo IV).
 */
enum class Severidad(val label: String) {
    LEVE("Leve"),
    MODERADO("Moderado"),
    SEVERO("Severo")
}

/**
 * Bien patrimonial (colección bienes_patrimoniales).
 */
data class BienPatrimonial(
    val id: String,
    val nombre: String,
    val tipo: String,
    val distrito: String,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String = "",
    val anio: String = "",
    val normaLegal: String = ""
)

/**
 * Reporte ciudadano (colección reportes).
 */
data class Reporte(
    val id: String,
    val idUsuario: String = "",
    val idBien: String? = null,
    val titulo: String = "",
    val descripcion: String = "",
    val direccion: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val fecha: String = "",
    val fechaCreacionMillis: Long = 0L,
    val severidad: Severidad = Severidad.LEVE,
    val estado: EstadoReporte = EstadoReporte.ENVIADO,
    val likes: Int = 0,
    val fotoUrl: String? = null
)

data class ReporteMapa(
    val id: String,
    val posicion: LatLng,
    val severidad: Severidad,
    val descripcion: String = "" // Con valor por defecto para no romper usos previos
)