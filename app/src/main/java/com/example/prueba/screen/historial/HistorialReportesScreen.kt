package com.patrimonialima.app.ui.screens.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Reporte
import com.patrimonialima.app.domain.model.Severidad
import com.patrimonialima.app.ui.components.SeverityBadge
import com.patrimonialima.app.ui.components.StatusBadge
import com.patrimonialima.app.ui.theme.BackgroundLight
import com.patrimonialima.app.ui.theme.BluePrimary
import com.patrimonialima.app.ui.theme.PatrimonioLimaTheme
import com.patrimonialima.app.ui.theme.RedText
import com.patrimonialima.app.ui.theme.TextDark
import com.patrimonialima.app.ui.theme.TextGray

private val reportesEjemplo = listOf(
    Reporte(
        id = "R1",
        titulo = "Grietas en la fachada colonial, posible daño estructural",
        direccion = "Jr. Junín 234, Cercado de Lima",
        fecha = "17 de abril de 2026",
        severidad = Severidad.SEVERO,
        estado = EstadoReporte.EN_REVISION,
        likes = 23
    ),
    Reporte(
        id = "R2",
        titulo = "Desprendimiento de pintura en balcón de madera",
        direccion = "Av. Abancay 567, Cercado de Lima",
        fecha = "14 de abril de 2026",
        severidad = Severidad.MODERADO,
        estado = EstadoReporte.REVISADO,
        likes = 15
    ),
    Reporte(
        id = "R3",
        titulo = "Humedad en muro de adobe",
        direccion = "Jr. Áncash 123, Cercado de Lima",
        fecha = "11 de abril de 2026",
        severidad = Severidad.MODERADO,
        estado = EstadoReporte.REVISADO,
        likes = 8
    ),
    Reporte(
        id = "R4",
        titulo = "Pequeñas fisuras en cornisa",
        direccion = "Plaza Mayor, Cercado de Lima",
        fecha = "9 de abril de 2026",
        severidad = Severidad.LEVE,
        estado = EstadoReporte.ENVIADO,
        likes = 5
    )
)

/**
 * Historial de reportes del usuario (sección 5.2.3 de la tesis, RF07).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialReportesScreen(
    reportes: List<Reporte> = reportesEjemplo,
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
        LazyColumn(
            modifier = Modifier
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

/** Tarjeta de reporte, reutilizable también fuera del historial (ej. resultados de búsqueda). */
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
            Text(reporte.direccion, fontSize = 11.sp, color = TextGray)
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

@Preview(showBackground = true)
@Composable
private fun HistorialReportesScreenPreview() {
    PatrimonioLimaTheme {
        HistorialReportesScreen()
    }
}
