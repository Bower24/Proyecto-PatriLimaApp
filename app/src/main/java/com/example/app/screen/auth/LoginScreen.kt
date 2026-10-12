package com.patrimonialima.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrimonialima.app.ui.theme.BackgroundLight
import com.patrimonialima.app.ui.theme.BluePrimary
import com.patrimonialima.app.ui.theme.PatrimonioGradient
import com.patrimonialima.app.ui.theme.PatrimonioLimaTheme
import com.patrimonialima.app.ui.theme.TextDark
import com.patrimonialima.app.ui.theme.TextGray
import com.patrimonialima.app.ui.theme.YellowAccent

/**
 * Pantalla de autenticación (RF01), con alternancia entre inicio de sesión
 * y registro mediante un selector de pestañas.
 */
@Composable
fun LoginScreen(
    cargando: Boolean = false,
    mensajeError: String? = null,
    onLoginSuccess: (correo: String, contrasena: String) -> Unit = { _, _ -> },
    onRegisterSuccess: (nombre: String, correo: String, contrasena: String) -> Unit = { _, _, _ -> }
) {
    var modoRegistro by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(PatrimonioGradient))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Logo circular
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(YellowAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = TextDark)
            }

            Spacer(Modifier.height(12.dp))
            Text("Patrimonio Lima", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Conservación participativa del patrimonio cultural",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Selector "Iniciar Sesión" / "Registrarse"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundLight, RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        SegmentButton(
                            texto = "Iniciar Sesión",
                            seleccionado = !modoRegistro,
                            modifier = Modifier.weight(1f)
                        ) { modoRegistro = false }
                        SegmentButton(
                            texto = "Registrarse",
                            seleccionado = modoRegistro,
                            modifier = Modifier.weight(1f)
                        ) { modoRegistro = true }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (modoRegistro) {
                        CampoTexto(
                            valor = nombre,
                            onValorChange = { nombre = it },
                            etiqueta = "Nombre completo",
                            placeholder = "Juan Pérez",
                            icono = Icons.Default.Person
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    CampoTexto(
                        valor = correo,
                        onValorChange = { correo = it },
                        etiqueta = "Correo electrónico",
                        placeholder = "correo@ejemplo.com",
                        icono = Icons.Default.Email
                    )

                    Spacer(Modifier.height(14.dp))

                    CampoTexto(
                        valor = contrasena,
                        onValorChange = { contrasena = it },
                        etiqueta = "Contraseña",
                        placeholder = "••••••••",
                        icono = Icons.Default.Lock,
                        esPassword = true
                    )

                    // Despliegue de mensaje de error si existe
                    if (!mensajeError.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = mensajeError,
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Botón con indicador de carga
                    Button(
                        onClick = {
                            if (modoRegistro) {
                                onRegisterSuccess(nombre, correo, contrasena)
                            } else {
                                onLoginSuccess(correo, contrasena)
                            }
                        },
                        enabled = !cargando && correo.isNotBlank() && contrasena.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (modoRegistro) "Crear cuenta" else "Ingresar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Unidos por la conservación de nuestro patrimonio",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SegmentButton(
    texto: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val fondo = if (seleccionado) YellowAccent else Color.Transparent
    val colorTexto = if (seleccionado) TextDark else TextGray
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(fondo)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    placeholder: String,
    icono: ImageVector,
    esPassword: Boolean = false
) {
    Column {
        Text(etiqueta, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = valor,
            onValueChange = onValorChange,
            placeholder = { Text(placeholder, color = TextGray, fontSize = 13.sp) },
            leadingIcon = { Icon(icono, contentDescription = null, tint = TextGray) },
            visualTransformation = if (esPassword) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BluePrimary,
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    PatrimonioLimaTheme {
        LoginScreen()
    }
}