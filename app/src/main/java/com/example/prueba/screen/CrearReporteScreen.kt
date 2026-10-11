package com.patrimonialima.app.ui.screens.reporte

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrimonialima.app.domain.model.Severidad
import com.patrimonialima.app.ui.theme.AmberBg
import com.patrimonialima.app.ui.theme.AmberText
import com.patrimonialima.app.ui.theme.BackgroundLight
import com.patrimonialima.app.ui.theme.BluePrimary
import com.patrimonialima.app.ui.theme.BorderGray
import com.patrimonialima.app.ui.theme.GreenBg
import com.patrimonialima.app.ui.theme.GreenText
import com.patrimonialima.app.ui.theme.PatrimonioLimaTheme
import com.patrimonialima.app.ui.theme.RedBg
import com.patrimonialima.app.ui.theme.RedText
import com.patrimonialima.app.ui.theme.TextDark
import com.patrimonialima.app.ui.theme.TextGray
import com.patrimonialima.app.ui.theme.YellowAccent

/**
 * Pantalla de creación de un nuevo reporte ciudadano (sección 5.2.4, RF02).
 * Flujo: seleccionar foto -> completar formulario -> enviar -> confirmación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearReporteScreen(
    onVolver: () -> Unit = {},
    onEnviarReporte: (descripcion: String, severidad: Severidad, direccion: String) -> Unit = { _, _, _ -> }
) {
    var fotoSeleccionada by remember { mutableStateOf(false) }
    var descripcion by remember { mutableStateOf("") }
    var severidad by remember { mutableStateOf(Severidad.MODERADO) }
    var direccion by remember { mutableStateOf("") }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Reporte", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Fotografía del daño", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
            Spacer(Modifier.height(10.dp))

            if (!fotoSeleccionada) {
                Button(
                    onClick = { fotoSeleccionada = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tomar foto")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { fotoSeleccionada = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Seleccionar de galería")
                }
            } else {
                FotoPreview(onCambiarFoto = { fotoSeleccionada = false })

                Spacer(Modifier.height(16.dp))
                Text("Descripción del daño", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    placeholder = {
                        Text("Describe el daño observado (grietas, desprendimiento, humedad, etc.)", fontSize = 12.sp)
                    },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(16.dp))
                Text("Nivel de daño", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Severidad.values().forEach { opcion ->
                        SeveridadOption(
                            severidad = opcion,
                            seleccionada = severidad == opcion,
                            modifier = Modifier.weight(1f)
                        ) { severidad = opcion }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Dirección del edificio", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    placeholder = { Text("Av. Abancay 123, Cercado de Lima", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberBg, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Asegúrate de que la foto muestre claramente el daño y que la dirección sea " +
                            "precisa para facilitar la evaluación.",
                        fontSize = 11.sp,
                        color = AmberText
                    )
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        onEnviarReporte(descripcion, severidad, direccion)
                        mostrarConfirmacion = true
                    },
                    enabled = descripcion.isNotBlank() && direccion.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YellowAccent, contentColor = TextDark)
                ) {
                    Text("Enviar Reporte", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    onVolver()
                }) {
                    Text("Aceptar")
                }
            },
            icon = { Icon(Icons.Default.Send, contentDescription = null, tint = GreenText) },
            title = { Text("¡Reporte Enviado!") },
            text = { Text("Gracias por contribuir a la conservación del patrimonio") }
        )
    }
}

@Composable
private fun FotoPreview(onCambiarFoto: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF6EA8FF), Color(0xFF3A6DF0)))),
        contentAlignment = Alignment.Center
    ) {
        Text("Foto simulada de edificio patrimonial", color = Color.White, fontSize = 13.sp)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RedText)
                .clickable { onCambiarFoto() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("Cambiar foto", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SeveridadOption(
    severidad: Severidad,
    seleccionada: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (fondo, texto) = when {
        !seleccionada -> Color.White to TextGray
        severidad == Severidad.LEVE -> GreenBg to GreenText
        severidad == Severidad.MODERADO -> AmberBg to AmberText
        else -> RedBg to RedText
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (seleccionada) Color.Transparent else BorderGray, RoundedCornerShape(10.dp))
            .background(fondo)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(severidad.label, color = texto, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Preview(showBackground = true)
@Composable
private fun CrearReporteScreenPreview() {
    PatrimonioLimaTheme {
        CrearReporteScreen()
    }
}
