package com.patrimonialima.app.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.patrimonialima.app.ui.theme.BackgroundLight
import com.patrimonialima.app.ui.theme.BlueBg
import com.patrimonialima.app.ui.theme.BluePrimary
import com.patrimonialima.app.ui.theme.BlueText
import com.patrimonialima.app.ui.theme.PatrimonioGradient
import com.patrimonialima.app.ui.theme.PatrimonioLimaTheme
import com.patrimonialima.app.ui.theme.TextDark
import com.patrimonialima.app.ui.theme.TextGray
import com.patrimonialima.app.ui.theme.YellowAccent

/** Resumen de la contribución del usuario (RF09: reacciones/likes recibidos). */
data class ContribucionUsuario(
    val reportesEnviados: Int,
    val enRevision: Int,
    val likesRecibidos: Int
)

@Composable
fun HomeScreen(
    nombreUsuario: String = "Usuario",
    contribucion: ContribucionUsuario = ContribucionUsuario(
        reportesEnviados = 12,
        enRevision = 8,
        likesRecibidos = 47
    ),
    datoEducativo: String = "Lima fue fundada en 1535 y su centro histórico fue declarado " +
            "Patrimonio de la Humanidad por UNESCO en 1988.",
    onNuevoReporte: () -> Unit = {},
    onVerHistorial: () -> Unit = {},
    onVerMapa: () -> Unit = {},
    onIrAAdmin: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    // Verificación de credenciales de administrador
    val auth = remember { FirebaseAuth.getInstance() }
    val correoActual = auth.currentUser?.email.orEmpty().lowercase()

    // Lista de correos con privilegios administrativos (puedes agregar tu correo de prueba aquí)
    val correosAdmin = listOf(
        "admin@prolima.gob.pe",
        "admin@patrimonio.pe",
        "admin@example.pe"

    )
    val esAdministrador = correoActual in correosAdmin || correoActual.startsWith("admin")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // Encabezado con degradado + tarjeta de contribución
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(PatrimonioGradient))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Patrimonio Lima", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Bienvenido, $nombreUsuario", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ícono de Admin: visible únicamente si el usuario es administrador
                    if (esAdministrador) {
                        IconButton(onClick = onIrAAdmin) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Panel PROLIMA",
                                tint = YellowAccent
                            )
                        }
                    }

                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EstadisticaItem("${contribucion.reportesEnviados}", "Reportes enviados")
                EstadisticaItem("${contribucion.enRevision}", "En revisión")
                EstadisticaItem("${contribucion.likesRecibidos}", "Likes recibidos")
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            // Botón destacado de Gestión PROLIMA (solo visible para Administradores)
            if (esAdministrador) {
                Card(
                    onClick = onIrAAdmin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BluePrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Panel de Gestión (PROLIMA)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Revisar y actualizar estado de reportes", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // CTA principal: Nuevo Reporte (RF02)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(YellowAccent)
                    .clickable { onNuevoReporte() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(TextDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Nuevo Reporte", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 15.sp)
                    Text(
                        "Documenta daños en edificios patrimoniales",
                        fontSize = 11.sp,
                        color = TextDark.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Accesos rápidos: Historial (RF07) y Mapa (RF03)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccesoRapidoCard(
                    icono = Icons.Default.History,
                    titulo = "Historial",
                    subtitulo = "Ver mis reportes",
                    modifier = Modifier.weight(1f),
                    onClick = onVerHistorial
                )
                AccesoRapidoCard(
                    icono = Icons.Default.Map,
                    titulo = "Mapa",
                    subtitulo = "Zonas críticas",
                    modifier = Modifier.weight(1f),
                    onClick = onVerMapa
                )
            }

            Spacer(Modifier.height(12.dp))

            // Contenido educativo rotativo (RF10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BlueBg, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = BlueText)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("¿Sabías que?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                    Text(datoEducativo, fontSize = 12.sp, color = TextGray)
                }
            }
        }
    }
}

@Composable
private fun EstadisticaItem(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(valor, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(etiqueta, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
    }
}

@Composable
private fun AccesoRapidoCard(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icono, contentDescription = null, tint = BlueText)
        Spacer(Modifier.height(6.dp))
        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
        Text(subtitulo, fontSize = 10.sp, color = TextGray)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PatrimonioLimaTheme {
        HomeScreen()
    }
}