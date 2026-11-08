package com.patrimonialima.app.domain

import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Reporte
import com.patrimonialima.app.domain.model.Severidad
import kotlin.math.*

data class ZonaCritica(
    val centroideLat: Double,
    val centroideLon: Double,
    val radioMetros: Double = 100.0,
    val reportes: List<Reporte>,
    val criticidadAcumulada: Int,
    val direccionRepresentativa: String = ""
)

object CalculadorZonasCriticas {

    private fun obtenerPeso(reporte: Reporte): Int {
        val texto = reporte.severidad.name.uppercase()
        return when {
            texto.contains("SEVERO") -> 3
            texto.contains("MODERADO") -> 2
            texto.contains("LEVE") -> 1
            else -> 1
        }
    }

    fun calcular(
        reportes: List<Reporte>,
        radioMetros: Double = 100.0,
        topK: Int = 5
    ): List<ZonaCritica> {
        // FILTRADO ESTRICTO:
        // 1. Coordenadas reales distintas de 0.0
        // 2. Exclusión de reportes ya revisados (solo entran ENVIADO y EN_REVISION)
        val reportesValidos = reportes.filter { reporte ->
            val tieneCoordenadas = reporte.latitud != 0.0 && reporte.longitud != 0.0
            val estaPendiente = reporte.estado == EstadoReporte.ENVIADO || reporte.estado == EstadoReporte.EN_REVISION
            tieneCoordenadas && estaPendiente
        }

        if (reportesValidos.isEmpty()) return emptyList()

        // Ordenamiento determinístico: primero mayor severidad y desempate por id
        val ordenados = reportesValidos.sortedWith(
            compareByDescending<Reporte> { obtenerPeso(it) }
                .thenBy { it.id }
        )

        val visitados = mutableSetOf<String>()
        val zonasCriticas = mutableListOf<ZonaCritica>()

        for (reporteBase in ordenados) {
            if (reporteBase.id in visitados) continue

            val clusterActual = mutableListOf<Reporte>()
            clusterActual.add(reporteBase)
            visitados.add(reporteBase.id)

            for (candidato in ordenados) {
                if (candidato.id in visitados) continue

                val d = calcularDistanciaHaversine(
                    reporteBase.latitud, reporteBase.longitud,
                    candidato.latitud, candidato.longitud
                )

                if (d <= radioMetros) {
                    clusterActual.add(candidato)
                    visitados.add(candidato.id)
                }
            }

            // Regla A1: Requiere un clúster mínimo de 2 reportes para proyectar una Zona Crítica
            if (clusterActual.size >= 2) {
                val latC = clusterActual.map { it.latitud }.average()
                val lonC = clusterActual.map { it.longitud }.average()
                val criticidadTotal = clusterActual.sumOf { obtenerPeso(it) }
                val dir = clusterActual.firstOrNull { it.direccion.isNotBlank() }?.direccion
                    ?: "Sector patrimonial (${clusterActual.size} incidentes)"

                zonasCriticas.add(
                    ZonaCritica(
                        centroideLat = latC,
                        centroideLon = lonC,
                        radioMetros = radioMetros,
                        reportes = clusterActual,
                        criticidadAcumulada = criticidadTotal,
                        direccionRepresentativa = dir
                    )
                )
            }
        }

        return zonasCriticas.sortedByDescending { it.criticidadAcumulada }.take(topK)
    }

    fun calcularDistanciaHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}