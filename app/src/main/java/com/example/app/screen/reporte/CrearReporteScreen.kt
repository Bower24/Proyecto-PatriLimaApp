package com.patrimonialima.app.ui.screens.reporte

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrimonialima.app.domain.model.Severidad
import com.patrimonialima.app.ui.theme.BluePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.patrimonialima.app.utils.ImageUtils
import com.patrimonialima.app.utils.PoliticaImagen
import androidx.compose.runtime.*
import com.patrimonialima.app.ui.screens.mapa.SeleccionarUbicacionScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearReporteScreen(
    onVolver: () -> Unit,
    onEnviarReporte: (
        direccion: String,
        descripcion: String,
        severidad: Severidad,
        imagenBase64: String?,
        latitud: Double,
        longitud: Double
    ) -> Unit,
    enviando: Boolean = false,
    mensajeError: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var direccion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var severidadSeleccionada by remember { mutableStateOf(Severidad.LEVE) }


    // Estados para la imagen
    var bitmapActual by remember { mutableStateOf<Bitmap?>(null) }
    var imagenBase64 by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoOrigen by remember { mutableStateOf(false) }
    var procesandoFoto by remember { mutableStateOf(false) }
    var errorFoto by remember { mutableStateOf<String?>(null) }

    var mostrarSelectorUbicacion by remember {
        mutableStateOf(false)
    }

    var latitudSeleccionada by remember {
        mutableStateOf<Double?>(null)
    }

    var longitudSeleccionada by remember {
        mutableStateOf<Double?>(null)
    }


    // Procesamiento compartido para cámara y galería.
    fun procesarFoto(procesar: () -> String?) {
        if (procesandoFoto || enviando) return
        procesandoFoto = true
        errorFoto = null
        scope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {
                    val cadena = procesar() ?: return@withContext null
                    // Conservar el formato utilizado por las pantallas existentes.
                    val base64 = cadena.removePrefix(PoliticaImagen.PREFIJO)
                    val bytes = Base64.decode(base64, Base64.NO_WRAP)
                    val preview = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: return@withContext null
                    base64 to preview
                }
                if (resultado == null) {
                    errorFoto = "No se pudo procesar la fotografía. Intenta nuevamente."
                } else {
                    imagenBase64 = resultado.first
                    bitmapActual = resultado.second
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                errorFoto = "No se pudo procesar la fotografía. Intenta nuevamente."
            } finally {
                procesandoFoto = false
            }
        }
    }

    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { seleccion ->
            procesarFoto { ImageUtils.uriABase64(context, seleccion) }
        }
    }

    val launcherCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        bmp?.let { foto ->
            procesarFoto {
                try {
                    ImageUtils.bitmapABase64(foto)
                } finally {
                    foto.recycle()
                }
            }
        }
    }

    if (mostrarSelectorUbicacion) {

        SeleccionarUbicacionScreen(
            direccion = direccion,
            latitudInicial = latitudSeleccionada,
            longitudInicial = longitudSeleccionada,

            onConfirmar = { latitud, longitud, nuevaDireccion ->
                latitudSeleccionada = latitud
                longitudSeleccionada = longitud

                if (!nuevaDireccion.isNullOrBlank()) {
                    direccion = nuevaDireccion
                }

                mostrarSelectorUbicacion = false
            },

            onCancelar = {
                mostrarSelectorUbicacion = false
            }
        )

    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Registrar Daño Patrimonial", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BluePrimary,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { nuevaDireccion ->
                        if (direccion != nuevaDireccion) {
                            direccion = nuevaDireccion
                            latitudSeleccionada = null
                            longitudSeleccionada = null
                        }
                    },
                    label = { Text("Ubicación o Jr. de referencia") },
                    placeholder = { Text("Ej. Jr. Áncash 338, Cercado de Lima") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        mostrarSelectorUbicacion = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        if (latitudSeleccionada != null)
                            "Modificar ubicación"
                        else
                            "Ubicar inmueble en el mapa"
                    )
                }

                if (latitudSeleccionada != null &&
                    longitudSeleccionada != null
                ) {
                    Text(
                        text = "Ubicación seleccionada correctamente",
                        color = Color(0xFF15803D),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción del daño detectado") },
                    placeholder = { Text("Ej. Fisura profunda en fachada de adobe...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Nivel de Severidad", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Severidad.values().forEach { s ->
                        val seleccionado = s == severidadSeleccionada
                        Button(
                            onClick = { severidadSeleccionada = s },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (seleccionado) BluePrimary else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = s.label,
                                fontSize = 11.sp,
                                color = if (seleccionado) Color.White else Color.DarkGray
                            )
                        }
                    }
                }

                Text("Fotografía de evidencia", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                if (procesandoFoto) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Procesando fotografía...")
                    }
                } else if (bitmapActual != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            bitmap = bitmapActual!!.asImageBitmap(),
                            contentDescription = "Foto reporte",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                bitmapActual = null
                                imagenBase64 = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar foto", tint = Color.White)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F1F1))
                            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(12.dp))
                            .clickable(enabled = !enviando && !procesandoFoto) { mostrarDialogoOrigen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Adjuntar foto",
                                tint = BluePrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Adjuntar foto (Cámara o Galería)", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }

                errorFoto?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { errorFoto = null }) {
                        Text("Continuar sin una nueva fotografía")
                    }
                }
                mensajeError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))



                Button(
                    onClick = {
                        val lat = latitudSeleccionada
                        val lon = longitudSeleccionada

                        if (
                            direccion.isNotBlank() &&
                            descripcion.isNotBlank() &&
                            lat != null &&
                            lon != null &&
                            !enviando && !procesandoFoto && errorFoto == null
                        ) {
                            onEnviarReporte(
                                direccion,
                                descripcion,
                                severidadSeleccionada,
                                imagenBase64,
                                lat,
                                lon
                            )
                        }
                    },
                    enabled = !enviando && !procesandoFoto && errorFoto == null &&
                            direccion.isNotBlank() &&
                            descripcion.isNotBlank() &&
                            latitudSeleccionada != null &&
                            longitudSeleccionada != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary
                    )
                ) {
                    if (enviando) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Enviar Reporte Ciudadano",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

            }
        }

        if (mostrarDialogoOrigen) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoOrigen = false },
                title = { Text("Seleccionar origen") },
                text = { Text("¿Cómo deseas adjuntar la fotografía del monumento?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mostrarDialogoOrigen = false
                            launcherCamara.launch()
                        }
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cámara")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            mostrarDialogoOrigen = false
                            launcherGaleria.launch("image/*")
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Galería")
                    }
                }
            )
        }
    }
}