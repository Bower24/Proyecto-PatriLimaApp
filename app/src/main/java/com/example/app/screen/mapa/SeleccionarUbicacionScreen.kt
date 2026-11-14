
package com.patrimonialima.app.ui.screens.mapa

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.patrimonialima.app.data.location.GeocodificadorDireccion
import com.patrimonialima.app.data.location.ZonaPermitidaLima
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.model.common.Observer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarUbicacionScreen(
    direccion: String,
    latitudInicial: Double? = null,
    longitudInicial: Double? = null,
    onConfirmar: (Double, Double, String?) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val posicionInicial = remember(
        latitudInicial,
        longitudInicial
    ) {
        if (
            latitudInicial != null &&
            longitudInicial != null &&
            ZonaPermitidaLima.contiene(
                latitudInicial,
                longitudInicial
            )
        ) {
            LatLong(latitudInicial, longitudInicial)
        } else {
            ZonaPermitidaLima.centro
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

    var confirmando by remember {
        mutableStateOf(false)
    }

    var mensaje by remember {
        mutableStateOf(
            "Mueve el mapa para seleccionar el inmueble."
        )
    }

    var esError by remember {
        mutableStateOf(false)
    }

    var errorMapa by remember {
        mutableStateOf<String?>(null)
    }

    // 1. PREPARAR CARTOGRAFÍA OFFLINE
    LaunchedEffect(Unit) {
        cargando = true

        try {
            AndroidGraphicFactory.createInstance(
                context.applicationContext as Application
            )
        } catch (_: Exception) {
            // Puede estar inicializado previamente.
        }

        try {
            archivoMapa = withContext(Dispatchers.IO) {
                val assets =
                    context.assets.list("").orEmpty()

                val nombre = when {
                    "lima.map" in assets -> "lima.map"
                    "peru.map" in assets -> "peru.map"
                    else -> error(
                        "No existe lima.map ni peru.map"
                    )
                }

                val destino = File(
                    context.filesDir,
                    nombre
                )

                if (
                    !destino.exists() ||
                    destino.length() == 0L
                ) {
                    context.assets.open(nombre).use { entrada ->
                        FileOutputStream(destino).use { salida ->
                            entrada.copyTo(salida)
                        }
                    }
                }

                destino
            }

        } catch (e: Exception) {
            errorMapa = e.message
                ?: "No se pudo cargar el mapa."
        } finally {
            cargando = false
        }
    }

    // 2. OBSERVAR MOVIMIENTOS DEL MAPA
    // No realiza consultas de geocodificación.
    DisposableEffect(mapView) {
        val mapa = mapView

        if (mapa == null) {
            onDispose { }
        } else {
            val handler = Handler(Looper.getMainLooper())

            val observador = object : Observer {
                override fun onChange() {
                    val centro =
                        mapa.model.mapViewPosition.center

                    handler.post {
                        if (mapView === mapa) {
                            posicionSeleccionada = LatLong(
                                centro.latitude,
                                centro.longitude
                            )
                        }
                    }
                }
            }

            mapa.model.mapViewPosition.addObserver(
                observador
            )

            onDispose {
                mapa.model.mapViewPosition.removeObserver(
                    observador
                )
                mapa.destroyAll()
            }
        }
    }

    // 3. BÚSQUEDA AUTOMÁTICA DE LA DIRECCIÓN
    LaunchedEffect(mapView, direccion) {
        val mapa = mapView
            ?: return@LaunchedEffect

        // Si el usuario ya confirmó una ubicación,
        // no se vuelve a buscar automáticamente.
        if (
            latitudInicial != null &&
            longitudInicial != null
        ) {
            return@LaunchedEffect
        }

        if (direccion.isBlank()) {
            mensaje = "Desplaza el mapa y selecciona " +
                    "manualmente la ubicación."
            esError = false
            return@LaunchedEffect
        }

        buscandoDireccion = true
        mensaje = "Buscando dirección..."
        esError = false

        try {
            val resultado = withTimeoutOrNull(5000L) {
                GeocodificadorDireccion.buscar(
                    context,
                    direccion
                )
            }

            if (
                resultado != null &&
                ZonaPermitidaLima.contiene(resultado)
            ) {
                mapa.setCenter(resultado)
                mapa.setZoomLevel(18.toByte())

                posicionSeleccionada = resultado

                mensaje = "Dirección encontrada. " +
                        "Ajusta el punto exacto en el mapa."

                esError = false

            } else {
                mapa.setCenter(ZonaPermitidaLima.centro)
                mapa.setZoomLevel(16.toByte())

                posicionSeleccionada =
                    ZonaPermitidaLima.centro

                mensaje = "No encontramos una ubicación " +
                        "válida para esa dirección. " +
                        "Selecciona el inmueble manualmente."

                esError = true
            }

        } finally {
            buscandoDireccion = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Seleccionar ubicación")
                },
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

        // 4. PANEL INFERIOR LEGIBLE
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (esError)
                                Color(0xFFFFF3F0)
                            else
                                Color(0xFFEAF4FF),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (esError)
                            Icons.Default.WarningAmber
                        else
                            Icons.Default.Info,
                        contentDescription = null,
                        tint = if (esError)
                            Color(0xFFC62828)
                        else
                            Color(0xFF1565C0),
                        modifier = Modifier.size(22.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (esError)
                                "Atención con la ubicación"
                            else
                                "Ubicación del inmueble",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = mensaje,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                // Coordenadas en tiempo real
                Text(
                    text = String.format(
                        Locale.US,
                        "Lat: %.6f | Lon: %.6f",
                        posicionSeleccionada.latitude,
                        posicionSeleccionada.longitude
                    ),
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                Button(
                    onClick = {
                        if (!confirmando) {
                            val centroActual =
                                mapView
                                    ?.model
                                    ?.mapViewPosition
                                    ?.center
                                    ?: posicionSeleccionada

                            if (
                                !ZonaPermitidaLima.contiene(
                                    centroActual
                                )
                            ) {
                                mensaje =
                                    "La ubicación está fuera " +
                                            "del área permitida. " +
                                            "Mueve el mapa hacia el " +
                                            "Centro Histórico de Lima."

                                esError = true

                            } else {
                                confirmando = true

                                scope.launch {
                                    try {
                                        // Solo se consulta al
                                        // confirmar la ubicación.
                                        val direccionEncontrada =
                                            withTimeoutOrNull(
                                                3000L
                                            ) {
                                                GeocodificadorDireccion
                                                    .buscarDireccion(
                                                        context,
                                                        centroActual.latitude,
                                                        centroActual.longitude
                                                    )
                                            }

                                        onConfirmar(
                                            centroActual.latitude,
                                            centroActual.longitude,
                                            direccionEncontrada
                                        )

                                    } finally {
                                        confirmando = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = archivoMapa != null &&
                            mapView != null &&
                            !buscandoDireccion &&
                            !confirmando
                ) {
                    if (confirmando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        if (confirmando)
                            "Obteniendo dirección..."
                        else
                            "Confirmar ubicación",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->

        // 5. MAPA Y CRUZ CENTRAL
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
                    Text(
                        errorMapa ?: "Error al cargar mapa",
                        color = Color.Red
                    )
                }

                archivoMapa != null -> {
                    val archivo = archivoMapa!!

                    key(archivo.absolutePath) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),

                            factory = { ctx: Context ->

                                // AQUÍ ESTÁ MapView(ctx).apply
                                MapView(ctx).apply {
                                    setClickable(true)
                                    setBuiltInZoomControls(true)

                                    mapScaleBar.isVisible = true

                                    val cache =
                                        AndroidUtil.createTileCache(
                                            ctx,
                                            "selector_mapcache",
                                            model.displayModel.tileSize,
                                            1f,
                                            model.frameBufferModel
                                                .overdrawFactor
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

                    // Cruz fija sobre el mapa
                    Text(
                        text = "✚",
                        color = Color.Red,
                        style = MaterialTheme
                            .typography.displayMedium
                    )
                }
            }

            if (buscandoDireccion) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(20.dp)
                )
            }
        }
    }
}
