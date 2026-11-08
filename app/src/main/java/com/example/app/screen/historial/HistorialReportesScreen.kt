package com.patrimonialima.app.ui.screens.historial

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Reporte
import com.patrimonialima.app.ui.components.SeverityBadge
import com.patrimonialima.app.ui.components.StatusBadge
import com.patrimonialima.app.ui.theme.BackgroundLight
import com.patrimonialima.app.ui.theme.BluePrimary
import com.patrimonialima.app.ui.theme.RedText
import com.patrimonialima.app.ui.theme.TextDark
import com.patrimonialima.app.ui.theme.TextGray

/**
 * Historial de reportes del usuario conectado al NavHost y al ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialReportesScreen(
    reportes: List<Reporte>,
    onVolver: () -> Unit = {},
    onAbrirReporte: (Reporte) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Reportes", fontWeight = FontWeight.Bold)
                        val enRevision = reportes.count { it.estado == EstadoReporte.EN_REVISION }
                        Text("${reportes.size} Total  •  $enRevision En Revisión", fontSize = 11.sp)
                    }
                },
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
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (reportes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no has registrado ningún reporte.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reportes, key = { it.id }) { reporte ->
                    ReportCard(reporte = reporte, onClick = { onAbrirReporte(reporte) })
                }
            }
        }
    }
}

/** Tarjeta de reporte protegida contra desbordes */
@Composable
fun ReportCard(reporte: Reporte, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(reporte.estado)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Reacciones",
                    tint = RedText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("${reporte.likes}", fontSize = 12.sp, color = TextDark)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(reporte.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = reporte.direccion,
                fontSize = 11.sp,
                color = TextGray,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(reporte.fecha, fontSize = 11.sp, color = TextGray)
            }
            SeverityBadge(reporte.severidad)
        }
    }
}