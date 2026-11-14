
package com.patrimonialima.app.ui.screens.mapa

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.patrimonialima.app.data.location.GeocodificadorDireccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarUbicacionScreen(
    direccion: String,
    latitudInicial: Double? = null,
    longitudInicial: Double? = null,
    onConfirmar: (Double, Double) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current

    val centroLima = remember {
        LatLong(-12.0465, -77.0305)
    }

    val posicionInicial = remember(
        latitudInicial,
        longitudInicial
    ) {
        if (latitudInicial != null &&
            longitudInicial != null
        ) {
            LatLong(latitudInicial, longitudInicial)
        } else {
            centroLima
        }
    }

    var posicionSeleccionada by remember {
        mutableStateOf(posicionInicial)
    }

    var mapView by remember {
        mutableStateOf<MapView?>(null)
    }

    var archivoMapa by remember {
        mutableStateOf<File?>(null)
    }

    var cargando by remember {
        mutableStateOf(true)
    }

    var buscandoDireccion by remember {
        mutableStateOf(false)
    }

    var mensaje by remember {
        mutableStateOf("Mueve el mapa para seleccionar el inmueble.")
    }

    var errorMapa by remember {
        mutableStateOf<String?>(null)
    }

    // Preparar el mapa offline.
    LaunchedEffect(Unit) {
        cargando = true

        try {
            AndroidGraphicFactory.createInstance(
                context.applicationContext as Application
            )
        } catch (_: Exception) {
            // Mapsforge ya podría estar inicializado.
        }

        try {
            archivoMapa = withContext(Dispatchers.IO) {
                val assets = context.assets.list("").orEmpty()

                val nombre = when {
                    "lima.map" in assets -> "lima.map"
                    "peru.map" in assets -> "peru.map"
                    else -> error("No existe lima.map ni peru.map")
                }

                val destino = File(context.filesDir, nombre)

                if (!destino.exists() || destino.length() == 0L) {
                    context.assets.open(nombre).use { entrada ->
                        FileOutputStream(destino).use { salida ->
                            entrada.copyTo(salida)
                        }
                    }
                }

                destino
            }
        } catch (e: Exception) {
            errorMapa = e.message ?: "No se pudo cargar el mapa."
        } finally {
            cargando = false
        }
    }

    // Buscar automáticamente solo al abrir el selector.
    // Si el usuario ya había confirmado coordenadas,
    // respetarlas y no volver a geocodificar.

    LaunchedEffect(mapView, direccion) {
        if (mapView == null) return@LaunchedEffect

        buscandoDireccion = true
        mensaje = "Buscando dirección..."

        val resultado = GeocodificadorDireccion.buscar(
            context,
            direccion
        )

        buscandoDireccion = false

        if (resultado != null) {
            posicionSeleccionada = resultado
            mapView?.setCenter(resultado)
            mapView?.setZoomLevel(18.toByte())

            mensaje = "Dirección encontrada. Ajusta el punto si es necesario."
        } else {
            mensaje = "No se encontró la dirección. Selecciona el lugar manualmente."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar ubicación") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancelar"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Lat: %.6f  |  Lon: %.6f".format(
                        java.util.Locale.US,
                        posicionSeleccionada.latitude,
                        posicionSeleccionada.longitude
                    ),
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = {
                        val centroActual =
                            mapView?.model?.mapViewPosition?.center
                                ?: posicionSeleccionada

                        onConfirmar(
                            centroActual.latitude,
                            centroActual.longitude
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = archivoMapa != null &&
                            mapView != null &&
                            !buscandoDireccion
                ) {
                    Text("Confirmar ubicación")
                }
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {

            when {
                cargando -> {
                    CircularProgressIndicator()
                }

                errorMapa != null -> {
                    Text(errorMapa ?: "Error al cargar mapa")
                }

                archivoMapa != null -> {

                    val archivo = archivoMapa!!

                    key(archivo.absolutePath) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx: Context ->

                                MapView(ctx).apply {

                                    setClickable(true)
                                    setBuiltInZoomControls(true)
                                    mapScaleBar.isVisible = true

                                    val cache = AndroidUtil.createTileCache(
                                        ctx,
                                        "selector_mapcache",
                                        model.displayModel.tileSize,
                                        1f,
                                        model.frameBufferModel.overdrawFactor
                                    )

                                    val capa = TileRendererLayer(
                                        cache,
                                        MapFile(archivo),
                                        model.mapViewPosition,
                                        AndroidGraphicFactory.INSTANCE
                                    )

                                    capa.setXmlRenderTheme(
                                        MapsforgeThemes.DEFAULT
                                    )

                                    layerManager.layers.add(capa)

                                    setCenter(posicionInicial)
                                    setZoomLevel(17.toByte())

                                    mapView = this
                                }
                            }
                        )
                    }

                    // Cruz central que permanece fija mientras
                    // el usuario desplaza el mapa.
                    Text(
                        text = "✚",
                        color = Color.Red,
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            if (buscandoDireccion) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(20.dp)
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.destroyAll()
            mapView = null
        }
    }
}
