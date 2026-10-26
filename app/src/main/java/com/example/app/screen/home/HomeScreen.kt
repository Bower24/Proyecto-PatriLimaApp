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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.patrimonialima.app.ui.theme.BackgroundLight
import com.patrimonialima.app.ui.theme.BlueBg
import com.patrimonialima.app.ui.theme.BluePrimary
import com.patrimonialima.app.ui.theme.BlueText
import com.patrimonialima.app.ui.theme.PatrimonioGradient
import com.patrimonialima.app.ui.theme.TextDark
import com.patrimonialima.app.ui.theme.TextGray
import com.patrimonialima.app.ui.theme.YellowAccent

data class ContribucionUsuario(
    val reportesEnviados: Int = 0,
    val enRevision: Int = 0,
    val likesRecibidos: Int = 0
)

@Composable
fun HomeScreen(
    datoEducativo: String = "Lima fue fundada en 1535 y su centro histórico fue declarado " +
            "Patrimonio de la Humanidad por UNESCO en 1988.",
    onNuevoReporte: () -> Unit = {},
    onVerHistorial: () -> Unit = {},
    onVerMapa: () -> Unit = {},
    onVerComunidad: () -> Unit = {},
    onIrAAdmin: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val correoActual = currentUser?.email.orEmpty().lowercase()
    val uidActual = currentUser?.uid.orEmpty()
    val nombreUsuario = currentUser?.displayName ?: currentUser?.email ?: "Usuario"

    var contribucion by remember { mutableStateOf(ContribucionUsuario()) }
    var alertaCambioEstado by remember { mutableStateOf<String?>(null) }

    // Escucha en tiempo real de reportes personales y cambios de estado (RF04)
    DisposableEffect(uidActual, correoActual) {
        if (uidActual.isEmpty() && correoActual.isEmpty()) return@DisposableEffect onDispose {}

        val db = FirebaseFirestore.getInstance()
        val listenerRegistration = db.collection("reportes")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                // Filtrar reportes que pertenecen a este usuario
                val misReportes = snapshot.documents.filter { doc ->
                    val docUserId = doc.getString("idUsuario")
                        ?: doc.getString("id_usuario")
                        ?: doc.getString("userId")
                        ?: ""
                    val docEmail = doc.getString("email")
                        ?: doc.getString("correo")
                        ?: ""

                    (uidActual.isNotEmpty() && docUserId == uidActual) ||
                            (correoActual.isNotEmpty() && (docUserId.equals(correoActual, ignoreCase = true) || docEmail.equals(correoActual, ignoreCase = true)))
                }

                var enRevisionCount = 0
                var likesTotal = 0

                for (doc in misReportes) {
                    val estado = doc.getString("estado").orEmpty()
                    if (estado.equals("EN_REVISION", ignoreCase = true) ||
                        estado.equals("En revisión", ignoreCase = true) ||
                        estado.equals("EN_REVISIÓN", ignoreCase = true)
                    ) {
                        enRevisionCount++
                    }

                    val likes = doc.getLong("likes_count")
                        ?: doc.getLong("likes")
                        ?: 0L
                    likesTotal += likes.toInt()
                }

                // Detección de cambios de estado para alertar al ciudadano (RF04)
                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.MODIFIED) {
                        val doc = change.document
                        val docUserId = doc.getString("idUsuario") ?: doc.getString("userId") ?: ""
                        val docEmail = doc.getString("email") ?: doc.getString("correo") ?: ""

                        val esMio = (uidActual.isNotEmpty() && docUserId == uidActual) ||
                                (correoActual.isNotEmpty() && (docUserId.equals(correoActual, ignoreCase = true) || docEmail.equals(correoActual, ignoreCase = true)))

                        if (esMio) {
                            val dir = doc.getString("direccion") ?: "tu reporte"
                            val nuevoEstado = doc.getString("estado") ?: ""
                            alertaCambioEstado = "Actualización: El reporte en '$dir' cambió a estado '$nuevoEstado'."
                        }
                    }
                }

                contribucion = ContribucionUsuario(
                    reportesEnviados = misReportes.size,
                    enRevision = enRevisionCount,
                    likesRecibidos = likesTotal
                )
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    // Validación de rol administrativo
    val esAdministrador = correoActual.endsWith("@prolima.gob.pe")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // Cabecera institucional con gradiente
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Patrimonio Lima", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Bienvenido, $nombreUsuario", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Tarjeta de estadísticas reactiva
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

            // Notificación reactiva in-app de cambio de estado (RF04)
            alertaCambioEstado?.let { mensaje ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF0284C7))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = mensaje,
                            color = Color(0xFF0369A1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { alertaCambioEstado = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF0284C7))
                        }
                    }
                }
            }

            // Panel PROLIMA (Solo visible para rol admin)
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

            // CTA Nuevo Reporte
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
                    Text("Documenta daños en edificios patrimoniales", fontSize = 11.sp, color = TextDark.copy(alpha = 0.75f))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Accesos rápidos: Historial y Mapa
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

            // NUEVO BOTÓN LARGO: Reportes de la Comunidad y Likes
            Card(
                onClick = onVerComunidad,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFE0E7FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reportes de la Comunidad",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Explora incidentes de otros vecinos y apoya con tu voto",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Cápsula educativa
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