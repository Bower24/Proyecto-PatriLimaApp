package com.patrimonialima.app.ui.screens.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.patrimonialima.app.domain.model.BienPatrimonial
import com.patrimonialima.app.domain.model.Severidad
import com.patrimonialima.app.ui.theme.AmberText
import com.patrimonialima.app.ui.theme.BackgroundLight
import com.patrimonialima.app.ui.theme.BluePrimary
import com.patrimonialima.app.ui.theme.GreenText
import com.patrimonialima.app.ui.theme.PatrimonioLimaTheme
import com.patrimonialima.app.ui.theme.RedText
import com.patrimonialima.app.ui.theme.TextDark

/**
 * NOTA IMPORTANTE: esta pantalla usa la librería Google Maps Compose
 * (com.google.maps.android:maps-compose), que requiere:
 *   1. Agregar la dependencia en build.gradle (ver README adjunto).
 *   2. Configurar una API key de Google Maps válida en AndroidManifest.xml.
 * Sin esos dos requisitos, el mapa no se renderizará en un dispositivo real,
 * aunque el código compila correctamente.
 */

/** Reporte simplificado para mostrar como marcador en el mapa. */
data class ReporteMapa(val id: String, val posicion: LatLng, val severidad: Severidad)

/** Resumen de reportes concentrados en una dirección (sección "Zonas críticas"). */
data class ZonaCritica(val direccion: String, val cantidadReportes: Int, val severidad: Severidad)

private val bienesEjemplo = listOf(
    BienPatrimonial("PRLM001", "Embajada de Argentina", "Colonial-Republicano", "Lima", -12.0947830, -77.0473788),
    BienPatrimonial("PRLM019", "Casona Jr. Cailloma 149", "Colonial-Republicano", "Lima", -12.0444431, -77.0329342)
)

private val reportesEjemplo = listOf(
    ReporteMapa("R1", LatLng(-12.0500, -77.0350), Severidad.SEVERO),
    ReporteMapa("R2", LatLng(-12.0520, -77.0300), Severidad.MODERADO),
    ReporteMapa("R3", LatLng(-12.0470, -77.0400), Severidad.LEVE)
)

private val zonasCriticasEjemplo = listOf(
    ZonaCritica("Jr. Junín, Cercado de Lima", 4, Severidad.SEVERO),
    ZonaCritica("Av. Abancay, Cercado de Lima", 2, Severidad.MODERADO),
    ZonaCritica("Jr. Áncash, Cercado de Lima", 1, Severidad.LEVE)
)

/**
 * Mapa interactivo con bienes patrimoniales y reportes (sección 5.2.5, RF03/RF08).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaInteractivoScreen(
    bienes: List<BienPatrimonial> = bienesEjemplo,
    reportes: List<ReporteMapa> = reportesEjemplo,
    zonasCriticas: List<ZonaCritica> = zonasCriticasEjemplo,
    onVolver: () -> Unit = {}
) {
    var filtroSeleccionado by remember { mutableStateOf("Todos") }
    val filtros = listOf("Todos", "Bienes patrimoniales", "Reportes activos", "Zonas críticas")

    val centroLima = LatLng(-12.0555, -77.0350)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centroLima, 15f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mapa del Patrimonio", fontWeight = FontWeight.Bold)
                        Text("Bienes y reportes en Cercado de Lima", fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Chips de filtro (RF08)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtros) { filtro ->
                    FilterChip(
                        selected = filtro == filtroSeleccionado,
                        onClick = { filtroSeleccionado = filtro },
                        label = { Text(filtro, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Mapa (RF03): bienes patrimoniales en azul, reportes coloreados por severidad
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(horizontal = 16.dp),
                cameraPositionState = cameraPositionState
            ) {
                bienes.forEach { bien ->
                    Marker(
                        state = MarkerState(position = LatLng(bien.latitud, bien.longitud)),
                        title = bien.nombre,
                        snippet = bien.tipo,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
                reportes.forEach { reporte ->
                    val hue = when (reporte.severidad) {
                        Severidad.LEVE -> BitmapDescriptorFactory.HUE_GREEN
                        Severidad.MODERADO -> BitmapDescriptorFactory.HUE_ORANGE
                        Severidad.SEVERO -> BitmapDescriptorFactory.HUE_RED
                    }
                    Marker(
                        state = MarkerState(position = reporte.posicion),
                        title = "Reporte - ${reporte.severidad.label}",
                        icon = BitmapDescriptorFactory.defaultMarker(hue)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Resumen "Zonas críticas"
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    "Zonas críticas (últimos 30 días)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextDark
                )
                Spacer(Modifier.height(8.dp))
                zonasCriticas.forEach { zona ->
                    val color = when (zona.severidad) {
                        Severidad.LEVE -> GreenText
                        Severidad.MODERADO -> AmberText
                        Severidad.SEVERO -> RedText
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(zona.direccion, fontSize = 12.sp, color = TextDark)
                        val etiquetaCantidad = if (zona.cantidadReportes == 1) "reporte" else "reportes"
                        Text(
                            "${zona.cantidadReportes} $etiquetaCantidad",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapaInteractivoScreenPreview() {
    PatrimonioLimaTheme {
        // El preview de GoogleMap requiere una API key valida configurada
        // en el proyecto para renderizar el mapa real dentro de Android Studio.
        MapaInteractivoScreen()
    }
}
