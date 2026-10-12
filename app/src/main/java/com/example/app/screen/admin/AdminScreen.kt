package com.patrimonialima.app.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Reporte
import com.patrimonialima.app.ui.components.SeverityBadge
import com.patrimonialima.app.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    reportes: List<Reporte>,
    cargando: Boolean,
    onActualizarEstado: (idReporte: String, nuevoEstado: EstadoReporte) -> Unit,
    onRecargar: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión Institucional (Admin)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = onRecargar) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (reportes.isEmpty()) {
                Text(
                    text = "No hay reportes registrados para evaluar.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reportes Ciudadanos Recibidos (${reportes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(reportes) { reporte ->
                        ReporteAdminCard(
                            reporte = reporte,
                            onCambiarEstado = { nuevo -> onActualizarEstado(reporte.id, nuevo) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun ReporteAdminCard(
    reporte: Reporte,
    onCambiarEstado: (EstadoReporte) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(estado = reporte.estado)
                SeverityBadge(severidad = reporte.severidad)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = reporte.descripcion, style = MaterialTheme.typography.bodyMedium)

            if (reporte.direccion.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ubicación: ${reporte.direccion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selector desplegable para actualizar el estado del reporte
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cambiar Estado: ${reporte.estado.label}")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    EstadoReporte.values().forEach { estadoOpcion ->
                        DropdownMenuItem(
                            text = { Text(estadoOpcion.label) },
                            onClick = {
                                expanded = false
                                if (estadoOpcion != reporte.estado) {
                                    onCambiarEstado(estadoOpcion)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
