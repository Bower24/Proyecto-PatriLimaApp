package com.patrimonialima.app.domain

import com.google.android.gms.maps.model.LatLng
import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Reporte
import com.patrimonialima.app.domain.model.Severidad
import kotlin.math.*

data class ZonaCritica(
    val centroide: LatLng,
    val direccionRepresentativa: String,
    val criticidad: Int,
    val reportes: List<Reporte>
)

object CalculadorZonasCriticas {

    /**
     * Algoritmo A1: Identificación y priorización de zonas críticas
     * Agrupamiento greedy espacial por radio de proximidad y suma ponderada de severidad.
     */
    fun calcular(
        reportes: List<Reporte>,
        radioMetros: Double = 100.0,
        topK: Int = 5
    ): List<ZonaCritica> {
        // 1. Filtrar reportes activos (excluir los ya revisados/atendidos)
        val activos = reportes.filter { it.estado != EstadoReporte.REVISADO && it.latitud != 0.0 && it.longitud != 0.0 }
        if (activos.isEmpty()) return emptyList()

        // Estructura auxiliar para acumular reportes por cluster
        val clusters = mutableListOf<MutableList<Reporte>>()
        val centroides = mutableListOf<LatLng>()

        for (r in activos) {
            var encontrado = -1
            for (i in centroides.indices) {
                val dist = distanciaHaversineMetros(r.latitud, r.longitud, centroides[i].latitude, centroides[i].longitude)
                if (dist <= radioMetros) {
                    encontrado = i
                    break
                }
            }

            if (encontrado >= 0) {
                clusters[encontrado].add(r)
                centroides[encontrado] = recalcularCentroide(clusters[encontrado])
            } else {
                clusters.add(mutableListOf(r))
                centroides.add(LatLng(r.latitud, r.longitud))
            }
        }

        // 2. Ponderación por severidad
        val zonas = clusters.mapIndexed { index, listaReportes ->
            val puntaje = listaReportes.sumOf { pesoSeveridad(it.severidad) }
            val dir = listaReportes.firstOrNull { it.direccion.isNotBlank() }?.direccion ?: "Zona patrimonial"
            ZonaCritica(
                centroide = centroides[index],
                direccionRepresentativa = dir,
                criticidad = puntaje,
                reportes = listaReportes
            )
        }

        // 3. Ordenar descendentemente por criticidad y limitar a topK
        return zonas.sortedByDescending { it.criticidad }.take(topK)
    }

    private fun pesoSeveridad(severidad: Severidad): Int = when (severidad) {
        Severidad.LEVE -> 1
        Severidad.MODERADO -> 2
        Severidad.SEVERO -> 3
    }

    private fun recalcularCentroide(reportes: List<Reporte>): LatLng {
        val latProm = reportes.map { it.latitud }.average()
        val lngProm = reportes.map { it.longitud }.average()
        return LatLng(latProm, lngProm)
    }

    private fun distanciaHaversineMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Radio terrestre en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
