package com.patrimonialima.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrimonialima.app.domain.model.EstadoReporte
import com.patrimonialima.app.domain.model.Severidad
import com.patrimonialima.app.ui.theme.AmberBg
import com.patrimonialima.app.ui.theme.AmberText
import com.patrimonialima.app.ui.theme.BlueBg
import com.patrimonialima.app.ui.theme.BlueText
import com.patrimonialima.app.ui.theme.GreenBg
import com.patrimonialima.app.ui.theme.GreenText
import com.patrimonialima.app.ui.theme.RedBg
import com.patrimonialima.app.ui.theme.RedText

/** Etiqueta visual del nivel de severidad de un reporte (leve/moderado/severo). */
@Composable
fun SeverityBadge(severidad: Severidad, modifier: Modifier = Modifier) {
    val (fondo, texto) = when (severidad) {
        Severidad.LEVE -> GreenBg to GreenText
        Severidad.MODERADO -> AmberBg to AmberText
        Severidad.SEVERO -> RedBg to RedText
    }
    Text(
        text = severidad.label,
        color = texto,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(fondo, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Etiqueta visual del estado del reporte (enviado/en revisión/revisado). */
@Composable
fun StatusBadge(estado: EstadoReporte, modifier: Modifier = Modifier) {
    val (fondo, texto) = when (estado) {
        EstadoReporte.ENVIADO -> BlueBg to BlueText
        EstadoReporte.EN_REVISION -> AmberBg to AmberText
        EstadoReporte.REVISADO -> GreenBg to GreenText
    }
    Text(
        text = estado.label,
        color = texto,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(fondo, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
