package com.patrimonialima.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PatrimonioColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = YellowAccent,
    background = BackgroundLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = TextDark,
    error = RedText
)

/**
 * Tema global de la aplicación "Patrimonio Lima".
 * Envuelve cada pantalla para que use la paleta de colores del proyecto.
 */
@Composable
fun PatrimonioLimaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PatrimonioColorScheme,
        content = content
    )
}
