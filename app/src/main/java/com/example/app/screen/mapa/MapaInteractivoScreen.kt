package com.patrimonialima.app.ui.screens.mapa

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.patrimonialima.app.domain.ZonaCritica
import com.patrimonialima.app.domain.model.BienPatrimonial
import com.patrimonialima.app.domain.model.ReporteMapa
import com.patrimonialima.app.domain.model.Severidad
import com.patrimonialima.app.ui.theme.AmberText
import com.patrimonialima.app.ui.theme.BackgroundLight
import com.patrimonialima.app.ui.theme.BluePrimary
import com.patrimonialima.app.ui.theme.GreenText
import com.patrimonialima.app.ui.theme.RedText
import com.patrimonialima.app.ui.theme.TextDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.Circle
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaInteractivoScreen(
    bienes: List<BienPatrimonial> = emptyList(),
    reportes: List<ReporteMapa> = emptyList(),
    zonasCriticas: List<ZonaCritica> = emptyList(),
    onVolver: () -> Unit = {}
) {
    var filtroSeleccionado by remember { mutableStateOf("Todos") }
    val filtros = listOf("Todos", "Bienes patrimoniales", "Reportes activos", "Zonas críticas")
    val context = LocalContext.current

    var archivoMapa by remember { mutableStateOf<File?>(null) }
    var cargandoMapa by remember { mutableStateOf(true) }
    var errorMapa by remember { mutableStateOf<String?>(null) }

    // Copiar mapa desde assets al almacenamiento interno una sola vez
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Busca 'lima.map' (o 'peru.map' si aún usas el grande)
                val nombreArchivo = if (context.assets.list("")?.contains("lima.map") == true) "lima.map" else "peru.map"
                val destino = File(context.filesDir, nombreArchivo)

                if (!destino.exists() || destino.length() == 0L) {
                    context.assets.open(nombreArchivo).use { entrada ->
                        FileOutputStream(destino).use { salida ->
                            entrada.copyTo(salida)
                        }
                    }
                }
                archivoMapa = destino
            } catch (e: Exception) {
                errorMapa = "Error al inicializar mapa: ${e.message}"
            } finally {
                cargandoMapa = false
            }
        }
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

            // Chips de filtro
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

            // Contenedor del Mapa Mapsforge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    cargandoMapa -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BluePrimary)
                            Text("Cargando cartografía offline...", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    errorMapa != null -> {
                        Text(errorMapa ?: "Error", color = RedText, fontSize = 12.sp)
                    }
                    archivoMapa != null -> {
                        MapaMapsforgeNativo(
                            archivo = archivoMapa!!,
                            bienes = bienes,
                            reportes = reportes,
                            zonasCriticas = zonasCriticas,
                            filtro = filtroSeleccionado,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Resumen de Zonas Críticas (Algoritmo A1)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    "Zonas críticas detectadas (Algoritmo A1)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextDark
                )
                Spacer(Modifier.height(8.dp))

                if (zonasCriticas.isEmpty()) {
                    Text(
                        "No se registran concentraciones críticas de daño por el momento.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } else {
                    zonasCriticas.forEach { zona ->
                        val tieneSevero = zona.reportes.any { it.severidad == Severidad.SEVERO }
                        val tieneModerado = zona.reportes.any { it.severidad == Severidad.MODERADO }
                        val color = when {
                            tieneSevero -> RedText
                            tieneModerado -> AmberText
                            else -> GreenText
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = zona.direccionRepresentativa.ifBlank { "Sector patrimonial (${zona.reportes.size} ptos)" },
                                fontSize = 12.sp,
                                color = TextDark,
                                modifier = Modifier.weight(1f)
                            )
                            val cant = zona.reportes.size
                            val etiquetaCantidad = if (cant == 1) "reporte" else "reportes"
                            Text(
                                text = "$cant $etiquetaCantidad (Criticidad: ${zona.criticidad})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MapaMapsforgeNativo(
    archivo: File,
    bienes: List<BienPatrimonial>,
    reportes: List<ReporteMapa>,
    zonasCriticas: List<ZonaCritica>,
    filtro: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    remember {
        AndroidGraphicFactory.createInstance(
            context.applicationContext as Application
        )
        true
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Actualizar marcadores al cambiar los filtros o listas
    LaunchedEffect(filtro, bienes, reportes, zonasCriticas, mapViewRef) {
        val mapView = mapViewRef ?: return@LaunchedEffect
        val layerManager = mapView.layerManager

        // Corrección del error de size(): Mapsforge usa el método Java size() o remove()
        // Conservamos la capa 0 (el mapa base) y removemos el resto
        while (layerManager.layers.size() > 1) {
            layerManager.layers.remove(layerManager.layers.size() - 1)
        }

        // 1. Zonas Críticas: Círculos translúcidos de 100m (Algoritmo A1)
        if (filtro == "Todos" || filtro == "Zonas críticas") {
            zonasCriticas.forEach { zona ->
                val paintRelleno = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                    color = android.graphics.Color.argb(80, 244, 67, 54)
                    setStyle(Style.FILL)
                }
                val paintBorde = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                    color = android.graphics.Color.rgb(211, 47, 47)
                    strokeWidth = 3f
                    setStyle(Style.STROKE)
                }

                // Corrección de coordenadas:
                // Si tu modelo tiene latitud/longitud directas, usa: zona.latitud, zona.longitud
                // Si calcula el centroide a partir del primer reporte o promedio de sus reportes:
                val lat = if (zona.reportes.isNotEmpty()) {
                    var sumaLat = 0.0
                    for (r in zona.reportes) {
                        sumaLat += r.latitud
                    }
                    sumaLat / zona.reportes.size
                } else {
                    -12.0465
                }

                val lon = if (zona.reportes.isNotEmpty()) {
                    var sumaLon = 0.0
                    for (r in zona.reportes) {
                        sumaLon += r.longitud
                    }
                    sumaLon / zona.reportes.size
                } else {
                    -77.0305
                }
                val circle = Circle(
                    LatLong(lat, lon),
                    100f,
                    paintRelleno,
                    paintBorde
                )
                layerManager.layers.add(circle)
            }
        }

        // 2. Bienes Patrimoniales (Azul)
        if (filtro == "Todos" || filtro == "Bienes patrimoniales") {
            val paintBien = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = android.graphics.Color.rgb(33, 150, 243)
                setStyle(Style.FILL)
            }
            bienes.forEach { bien ->
                val punto = Circle(LatLong(bien.latitud, bien.longitud), 10f, paintBien, null)
                layerManager.layers.add(punto)
            }
        }

        // 3. Reportes de daño (Puntos según severidad)
        if (filtro == "Todos" || filtro == "Reportes activos") {
            reportes.forEach { rep ->
                val colorArgb = when (rep.severidad) {
                    Severidad.LEVE -> android.graphics.Color.rgb(76, 175, 80)
                    Severidad.MODERADO -> android.graphics.Color.rgb(255, 152, 0)
                    Severidad.SEVERO -> android.graphics.Color.rgb(244, 67, 54)
                }
                val paintRep = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                    color = colorArgb
                    setStyle(Style.FILL)
                }
                val punto = Circle(LatLong(rep.posicion.latitude, rep.posicion.longitude), 12f, paintRep, null)
                layerManager.layers.add(punto)
            }
        }

        mapView.repaint()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx: Context ->
            MapView(ctx).apply {
                setClickable(true)
                setBuiltInZoomControls(true)
                mapScaleBar.isVisible = true

                val cache = AndroidUtil.createTileCache(
                    ctx,
                    "mapcache",
                    model.displayModel.tileSize,
                    1f,
                    model.frameBufferModel.overdrawFactor
                )

                val datosMapa = MapFile(archivo)
                val capa = TileRendererLayer(
                    cache,
                    datosMapa,
                    model.mapViewPosition,
                    AndroidGraphicFactory.INSTANCE
                )
                capa.setXmlRenderTheme(MapsforgeThemes.DEFAULT)
                layerManager.layers.add(capa)

                // Coordenadas fijas del Centro Histórico de Lima
                setCenter(LatLong(-12.0465, -77.0305))
                setZoomLevel(15.toByte())

                mapViewRef = this
            }
        }
    )
}