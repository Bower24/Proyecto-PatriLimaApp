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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearReporteScreen(
    onVolver: () -> Unit,
    onEnviarReporte: suspend (direccion: String, descripcion: String, severidad: Severidad, imagenBase64: String?) -> Boolean
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
    var enviando by remember { mutableStateOf(false) }

    // Función auxiliar para comprimir a Base64 sin sobrecargar Firestore (< 1MB)
    fun procesarBitmap(bitmapOriginal: Bitmap) {
        val maxDim = 800
        val ratio = minOf(maxDim.toFloat() / bitmapOriginal.width, maxDim.toFloat() / bitmapOriginal.height, 1.0f)
        val nuevoAncho = (bitmapOriginal.width * ratio).toInt()
        val nuevoAlto = (bitmapOriginal.height * ratio).toInt()

        val escalado = Bitmap.createScaledBitmap(bitmapOriginal, nuevoAncho, nuevoAlto, true)
        val stream = ByteArrayOutputStream()
        escalado.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val bytes = stream.toByteArray()

        bitmapActual = escalado
        imagenBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // 1. Selector de Galería
    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(it)?.use { input ->
                        val bmp = BitmapFactory.decodeStream(input)
                        if (bmp != null) {
                            withContext(Dispatchers.Main) {
                                procesarBitmap(bmp)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // 2. Launcher de Cámara nativa (Toma directa de foto)
    val launcherCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        bmp?.let {
            procesarBitmap(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Daño Patrimonial", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = androidx.compose.ui.graphics.Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
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
                onValueChange = { direccion = it },
                label = { Text("Ubicación o Jr. de referencia") },
                placeholder = { Text("Ej. Jr. Áncash 338, Cercado de Lima") },
                modifier = Modifier.fillMaxWidth()
            )

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
                            containerColor = if (seleccionado) BluePrimary else androidx.compose.ui.graphics.Color.LightGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(s.label, fontSize = 11.sp, color = if (seleccionado) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.DarkGray)
                    }
                }
            }

            Text("Fotografía de evidencia", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

            // Contenedor de la foto / Botón de captura
            if (bitmapActual != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, androidx.compose.ui.graphics.Color.Gray, RoundedCornerShape(12.dp))
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
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Eliminar foto", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFFF1F1F1))
                        .border(1.dp, androidx.compose.ui.graphics.Color(0xFFCCCCCC), RoundedCornerShape(12.dp))
                        .clickable { mostrarDialogoOrigen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Adjuntar foto", tint = BluePrimary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Adjuntar foto (Cámara o Galería)", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.DarkGray)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (direccion.isNotBlank() && descripcion.isNotBlank() && !enviando) {
                        enviando = true
                        scope.launch {
                            val res = onEnviarReporte(direccion, descripcion, severidadSeleccionada, imagenBase64)
                            enviando = false
                            if (res) onVolver()
                        }
                    }
                },
                enabled = !enviando && direccion.isNotBlank() && descripcion.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                if (enviando) {
                    CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Enviar Reporte Ciudadano", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Diálogo modal para elegir Cámara o Galería
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