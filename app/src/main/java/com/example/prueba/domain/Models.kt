package com.patrimonialima.app.domain.model

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
    val titulo: String,
    val direccion: String,
    val fecha: String,
    val severidad: Severidad,
    val estado: EstadoReporte,
    val likes: Int,
    val fotoUrl: String? = null
)
