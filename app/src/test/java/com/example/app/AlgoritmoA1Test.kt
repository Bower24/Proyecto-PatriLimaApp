package com.patrimonialima.app

import com.patrimonialima.app.domain.CalculadorZonasCriticas
import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Reporte
import com.patrimonialima.app.domain.model.Severidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgoritmoA1Test {

    @Test
    fun haversine_calculaDistanciaAproximadaCorrecta() {
        // Plaza Mayor de Lima a Convento de San Francisco (~300 - 360 metros)
        val latPlaza = -12.0453
        val lonPlaza = -77.0311
        val latSanFrancisco = -12.0459
        val lonSanFrancisco = -77.0278

        val distancia = CalculadorZonasCriticas.calcularDistanciaHaversine(
            latPlaza, lonPlaza,
            latSanFrancisco, lonSanFrancisco
        )

        assertTrue("La distancia real debe oscilar entre 300 y 400 metros", distancia in 300.0..400.0)
    }

    @Test
    fun clustering_agrupaPuntosMenoresA100Metros() {
        // Dos puntos a menos de 40 metros en Jr. Áncash
        val r1 = Reporte(
            id = "rep-001",
            idUsuario = "user-1",
            direccion = "Jr. Áncash 213",
            descripcion = "Fisura severa en cornisa",
            latitud = -12.0451,
            longitud = -77.0289,
            severidad = Severidad.SEVERO,
            estado = EstadoReporte.ENVIADO
        )
        val r2 = Reporte(
            id = "rep-002",
            idUsuario = "user-2",
            direccion = "Jr. Áncash 245",
            descripcion = "Desprendimiento de adobe",
            latitud = -12.0453,
            longitud = -77.0287,
            severidad = Severidad.MODERADO,
            estado = EstadoReporte.ENVIADO
        )

        val zonas = CalculadorZonasCriticas.calcular(
            reportes = listOf(r1, r2),
            radioMetros = 100.0
        )

        assertEquals("Debe consolidar 1 sola zona crítica", 1, zonas.size)
        // Severidad SEVERO (3) + MODERADO (2) = 5[cite: 1]
        assertEquals("La criticidad acumulada debe ser 5", 5, zonas[0].criticidadAcumulada)
        assertEquals("La zona crítica debe contener los 2 reportes", 2, zonas[0].reportes.size)
    }

    @Test
    fun clustering_noCreaZonaCriticaParaReportesAislados() {
        // Un solo reporte aislado sin vecinos cercanos
        val r1 = Reporte(
            id = "rep-001",
            idUsuario = "user-1",
            direccion = "Jr. Carabaya 420",
            descripcion = "Grafiti en fachada",
            latitud = -12.0482,
            longitud = -77.0315,
            severidad = Severidad.SEVERO,
            estado = EstadoReporte.ENVIADO
        )

        val zonas = CalculadorZonasCriticas.calcular(
            reportes = listOf(r1),
            radioMetros = 100.0
        )

        // Cumple la condición de no crear zonas críticas si N < 2[cite: 3]
        assertTrue("No debe proyectar zona crítica si N < 2", zonas.isEmpty())
    }

    @Test
    fun clustering_esDeterministicoIndependienteDelOrdenDeEntrada() {
        val r1 = Reporte(
            id = "rep-001",
            direccion = "Jr. Áncash 213",
            latitud = -12.0451,
            longitud = -77.0289,
            severidad = Severidad.SEVERO
        )
        val r2 = Reporte(
            id = "rep-002",
            direccion = "Jr. Áncash 245",
            latitud = -12.0453,
            longitud = -77.0287,
            severidad = Severidad.MODERADO
        )

        // Ejecución en orden A -> B
        val ordenDirecto = CalculadorZonasCriticas.calcular(listOf(r1, r2))
        // Ejecución en orden inverso B -> A
        val ordenInverso = CalculadorZonasCriticas.calcular(listOf(r2, r1))

        assertEquals("La cantidad de zonas debe ser igual", ordenDirecto.size, ordenInverso.size)
        assertEquals("La criticidad debe ser idéntica", ordenDirecto[0].criticidadAcumulada, ordenInverso[0].criticidadAcumulada)
        assertEquals("El centroide Latitud debe coincidir", ordenDirecto[0].centroideLat, ordenInverso[0].centroideLat, 0.0001)
        assertEquals("El centroide Longitud debe coincidir", ordenDirecto[0].centroideLon, ordenInverso[0].centroideLon, 0.0001)
    }
}