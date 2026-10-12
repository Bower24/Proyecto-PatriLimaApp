package com.patrimonialima.app.ui.screens.admin

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Severidad
import com.patrimonialima.app.ui.theme.BluePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Modelo simple para la vista administrativa
data class ReporteAdminItem(
    val id: String = "",
    val direccion: String = "",
    val descripcion: String = "",
    val severidad: Severidad = Severidad.LEVE,
    val estado: EstadoReporte = EstadoReporte.ENVIADO,
    val fotoBase64: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onVolver: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var cargando by remember { mutableStateOf(true) }
    val listaReportes = remember { mutableStateListOf<ReporteAdminItem>() }
    var filtroEstado by remember { mutableStateOf("Todos") }
    val opcionesFiltro = listOf("Todos", "Enviados", "En Revisión", "Revisados")

    // Cargar reportes de Firestore en tiempo real
    fun cargarReportes() {
        scope.launch(Dispatchers.IO) {
            cargando = true
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("reportes")
                    .get()
                    .await()

                val items = snapshot.documents.map { doc ->
                    val sevStr = doc.getString("severidad") ?: "LEVE"
                    val estStr = doc.getString("estado") ?: "ENVIADO"
                    ReporteAdminItem(
                        id = doc.id,
                        direccion = doc.getString("direccion") ?: "Sin dirección",
                        descripcion = doc.getString("descripcion") ?: "Sin descripción",
                        severidad = try { Severidad.valueOf(sevStr) } catch (_: Exception) { Severidad.LEVE },
                        estado = try { EstadoReporte.valueOf(estStr) } catch (_: Exception) { EstadoReporte.ENVIADO },
                        fotoBase64 = doc.getString("fotoBase64")
                    )
                }

                withContext(Dispatchers.Main) {
                    listaReportes.clear()
                    listaReportes.addAll(items)
                    cargando = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    cargando = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        cargarReportes()
    }

    // Filtrado local según la pestaña seleccionada
    val reportesFiltrados = remember(filtroEstado, listaReportes.toList()) {
        when (filtroEstado) {
            "Enviados" -> listaReportes.filter { it.estado == EstadoReporte.ENVIADO }
            "En Revisión" -> listaReportes.filter { it.estado == EstadoReporte.EN_REVISION }
            "Revisados" -> listaReportes.filter { it.estado == EstadoReporte.REVISADO }
            else -> listaReportes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gestión de Reportes (PROLIMA)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Panel de Administración y Control", fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { cargarReportes() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Recargar", tint = Color.White)
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
        ) {
            // Chips de filtrado por estado
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(opcionesFiltro) { opt ->
                    FilterChip(
                        selected = opt == filtroEstado,
                        onClick = { filtroEstado = opt },
                        label = { Text(opt, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else if (reportesFiltrados.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay reportes en esta categoría.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reportesFiltrados, key = { it.id }) { item ->
                        TarjetaReporteAdmin(
                            reporte = item,
                            onCambiarEstado = { nuevoEstado ->
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        FirebaseFirestore.getInstance()
                                            .collection("reportes")
                                            .document(item.id)
                                            .update("estado", nuevoEstado.name)
                                            .await()

                                        withContext(Dispatchers.Main) {
                                            val index = listaReportes.indexOfFirst { it.id == item.id }
                                            if (index != -1) {
                                                listaReportes[index] = listaReportes[index].copy(estado = nuevoEstado)
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaReporteAdmin(
    reporte: ReporteAdminItem,
    onCambiarEstado: (EstadoReporte) -> Unit
) {
    val bitmap = remember(reporte.fotoBase64) {
        reporte.fotoBase64?.let { base64Str ->
            try {
                val bytes = Base64.decode(base64Str, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        }
    }

    val colorEstado = when (reporte.estado) {
        EstadoReporte.ENVIADO -> Color(0xFF1E88E5)    // Azul
        EstadoReporte.EN_REVISION -> Color(0xFFFB8C00) // Ámbar
        EstadoReporte.REVISADO -> Color(0xFF43A047)    // Verde
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Severidad: ${reporte.severidad.label}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = when (reporte.severidad) {
                        Severidad.SEVERO -> Color(0xFFD32F2F)
                        Severidad.MODERADO -> Color(0xFFF57C00)
                        Severidad.LEVE -> Color(0xFF388E3C)
                    }
                )

                // Etiqueta del estado actual
                Box(
                    modifier = Modifier
                        .background(colorEstado.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = reporte.estado.label,
                        color = colorEstado,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = reporte.direccion,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF212121)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = reporte.descripcion,
                fontSize = 12.sp,
                color = Color(0xFF555555)
            )

            if (bitmap != null) {
                Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Evidencia del reporte",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Gestión de flujo:",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Botón "En revisión" con capacidad de alternar / retroceder a "Enviado" (momentaneo)
                val estaEnRevision = reporte.estado == EstadoReporte.EN_REVISION
                val puedeActivarRevision = reporte.estado == EstadoReporte.ENVIADO || estaEnRevision

                OutlinedButton(
                    onClick = {
                        if (estaEnRevision) {
                            onCambiarEstado(EstadoReporte.ENVIADO) // Retrocede a Enviado
                        } else {
                            onCambiarEstado(EstadoReporte.EN_REVISION) // Avanza a En revisión
                        }
                    },
                    enabled = puedeActivarRevision,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (puedeActivarRevision) Color(0xFFFB8C00) else Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (estaEnRevision) "Deshacer revisión" else "En revisión",
                        fontSize = 11.sp,
                        color = if (puedeActivarRevision) Color(0xFFFB8C00) else Color.Gray
                    )
                }

                // 2. Botón "Revisado" con capacidad de alternar / retroceder a "En revisión" (momentaneo)
                val estaRevisado = reporte.estado == EstadoReporte.REVISADO
                val puedeActivarRevisado = reporte.estado == EstadoReporte.EN_REVISION || estaRevisado

                OutlinedButton(
                    onClick = {
                        if (estaRevisado) {
                            onCambiarEstado(EstadoReporte.EN_REVISION) // Retrocede a En revisión
                        } else {
                            onCambiarEstado(EstadoReporte.REVISADO) // Avanza a Revisado
                        }
                    },
                    enabled = puedeActivarRevisado,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (puedeActivarRevisado) Color(0xFF43A047) else Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (estaRevisado) "Deshacer revisado" else "Revisado",
                        fontSize = 11.sp,
                        color = if (puedeActivarRevisado) Color(0xFF43A047) else Color.Gray
                    )
                }
            }
        }
    }
}