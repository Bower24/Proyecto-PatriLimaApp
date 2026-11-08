package com.patrimonialima.app.ui.screens.home

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.patrimonialima.app.ui.theme.AmberText
import com.patrimonialima.app.ui.theme.BackgroundLight
import com.patrimonialima.app.ui.theme.BluePrimary
import com.patrimonialima.app.ui.theme.GreenText
import com.patrimonialima.app.ui.theme.RedText
import com.patrimonialima.app.ui.theme.TextDark
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificacionAlerta(
    val id: String,
    val mensaje: String,
    val hora: String
)
private fun cargarNotificacionesGuardadas(
    prefs: android.content.SharedPreferences,
    uid: String
): List<NotificacionAlerta> {
    return try {
        val json = prefs.getString("notificaciones_$uid", null)
            ?: return emptyList()

        val array = JSONArray(json)
        val resultado = mutableListOf<NotificacionAlerta>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)

            resultado.add(
                NotificacionAlerta(
                    id = item.getString("id"),
                    mensaje = item.getString("mensaje"),
                    hora = item.getString("hora")
                )
            )
        }

        resultado
    } catch (e: Exception) {
        Log.e("HomeScreen", "Error cargando notificaciones", e)
        emptyList()
    }
}

private fun guardarNotificaciones(
    prefs: android.content.SharedPreferences,
    uid: String,
    notificaciones: List<NotificacionAlerta>
) {
    try {
        val array = JSONArray()

        notificaciones.forEach { notificacion ->
            val item = JSONObject().apply {
                put("id", notificacion.id)
                put("mensaje", notificacion.mensaje)
                put("hora", notificacion.hora)
            }

            array.put(item)
        }

        prefs.edit()
            .putString("notificaciones_$uid", array.toString())
            .apply()

    } catch (e: Exception) {
        Log.e("HomeScreen", "Error guardando notificaciones", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nombreUsuario: String = "Usuario",
    contribucion: ContribucionUsuario = ContribucionUsuario(0, 0, 0, 0),
    onNuevoReporte: () -> Unit = {},
    onVerHistorial: () -> Unit = {},
    onVerMapa: () -> Unit = {},
    onVerComunidad: () -> Unit = {},
    onIrAAdmin: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val usuario = auth.currentUser
    val uid = usuario?.uid.orEmpty()

    var esAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            try {
                val adminDoc = FirebaseFirestore.getInstance()
                    .collection("administradores")
                    .document(uid)
                    .get()
                    .await()
                esAdmin = adminDoc.exists()
            } catch (_: Exception) {
                esAdmin = false
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var mostrarBuzon by remember { mutableStateOf(false) }

// Preferencias locales
    val prefs = remember {
        context.getSharedPreferences(
            "estados_reportes_cache",
            Context.MODE_PRIVATE
        )
    }

// Recuperar el historial guardado del usuario
    val listaNotificaciones = remember(uid) {
        mutableStateListOf<NotificacionAlerta>().apply {
            if (uid.isNotBlank()) {
                addAll(cargarNotificacionesGuardadas(prefs, uid))
            }
        }
    }

    // Estados reactivos sincronizados
    var totalEnviados by remember(uid) { mutableIntStateOf(contribucion.enviados) }
    var totalEnRevision by remember(uid) { mutableIntStateOf(contribucion.enRevision) }
    var totalRevisados by remember(uid) { mutableIntStateOf(contribucion.revisados) }
    var totalLikes by remember(uid) { mutableIntStateOf(contribucion.likes) }


    DisposableEffect(uid) {
        if (uid.isBlank()) return@DisposableEffect onDispose {}

        val db = FirebaseFirestore.getInstance()

        // Escucha en tiempo real sobre la colección de reportes
// Escucha en tiempo real sobre la colección de reportes
        val listener = db.collection("reportes")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e("HomeScreen", "Error escuchando reportes", error)
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                // Reconocer los diferentes nombres de campo utilizados
                // en documentos antiguos y actuales.
                val misDocumentos = snapshot.documents.filter { doc ->
                    val userEnDoc =
                        doc.getString("idUsuario")
                            ?: doc.getString("id_usuario")
                            ?: doc.getString("userId")
                            ?: ""

                    userEnDoc == uid
                }

                // 1. Total de reportes enviados
                totalEnviados = misDocumentos.size

                // 2. Reportes en revisión
                totalEnRevision = misDocumentos.count { doc ->
                    val estado = doc.getString("estado")
                        ?.uppercase()
                        .orEmpty()

                    estado.contains("REVISION")
                }

                // 3. Reportes revisados o atendidos
                totalRevisados = misDocumentos.count { doc ->
                    val estado = doc.getString("estado")
                        ?.uppercase()
                        .orEmpty()

                    estado.contains("REVISADO") ||
                            estado.contains("ATENDIDO")
                }

                // 4. Likes acumulados
                totalLikes = misDocumentos.sumOf { doc ->
                    val likes = doc.get("likes_count") ?: doc.get("likes")

                    when (likes) {
                        is Number -> likes.toInt()
                        is String -> likes.toIntOrNull() ?: 0
                        else -> 0
                    }
                }

                // 5. Detectar cambios de estado y generar notificaciones
                for (doc in misDocumentos) {

                    val repId = doc.id

                    val estadoActual = doc.getString("estado")
                        ?.uppercase()
                        ?: "ENVIADO"

                    val direccion = doc.getString("direccion")
                        ?.ifBlank { "su reporte" }
                        ?: "su reporte"

                    // La clave incluye el UID para separar las cuentas.
                    val claveEstado = "estado_${uid}_$repId"

                    // Compatibilidad con el formato anterior de preferencias.
                    val estadoGuardado = prefs.getString(claveEstado, null)
                        ?: prefs.getString("estado_$repId", null)

                    if (estadoGuardado == null) {

                        // Primera vez que se observa este reporte:
                        // guardar el estado inicial sin generar notificación.
                        prefs.edit()
                            .putString(claveEstado, estadoActual)
                            .apply()

                    } else if (estadoGuardado != estadoActual) {

                        // Guardar el nuevo estado para evitar notificaciones repetidas.
                        prefs.edit()
                            .putString(claveEstado, estadoActual)
                            .apply()

                        // Solo notificar los estados que interesan al ciudadano.
                        val estadoLegible = when (estadoActual) {
                            "EN_REVISION" ->
                                "pasó a En Revisión por PROLIMA"

                            "REVISADO", "ATENDIDO" ->
                                "ha sido evaluado y atendido"

                            else -> null
                        }

                        if (estadoLegible != null) {

                            val mensaje =
                                "El reporte en \"$direccion\" $estadoLegible."

                            val fechaHora = SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                            ).format(Date())

                            val nuevaNotificacion = NotificacionAlerta(
                                id = "${repId}_${estadoActual}_${System.currentTimeMillis()}",
                                mensaje = mensaje,
                                hora = fechaHora
                            )

                            // Agregar al inicio del historial.
                            listaNotificaciones.add(0, nuevaNotificacion)

                            // Persistir inmediatamente el historial actualizado.
                            guardarNotificaciones(
                                prefs = prefs,
                                uid = uid,
                                notificaciones = listaNotificaciones
                            )

                            // Mostrar aviso mientras el usuario está en Home.
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = mensaje,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }
            }

        onDispose {
            listener.remove()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Patrimonio Lima",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (esAdmin) "Perfil: Técnico PROLIMA" else "Conservación participativa",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { mostrarBuzon = !mostrarBuzon }) {
                            BadgedBox(
                                badge = {
                                    if (listaNotificaciones.isNotEmpty()) {
                                        Badge(containerColor = Color(0xFFEF4444)) {
                                            Text(
                                                text = listaNotificaciones.size.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (listaNotificaciones.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Alertas",
                                    tint = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = mostrarBuzon,
                            onDismissRequest = { mostrarBuzon = false },
                            modifier = Modifier
                                .width(280.dp)
                                .background(Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Actualizaciones de reportes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextDark
                                )
                                if (listaNotificaciones.isNotEmpty()) {
                                    Text(
                                        text = "Limpiar",
                                        fontSize = 11.sp,
                                        color = BluePrimary,
                                        modifier = Modifier.clickable {
                                            listaNotificaciones.clear()

                                            guardarNotificaciones(
                                                prefs = prefs,
                                                uid = uid,
                                                notificaciones = listaNotificaciones
                                            )
                                        }
                                    )
                                }
                            }
                            HorizontalDivider()

                            if (listaNotificaciones.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Sin novedades recientes",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    },
                                    onClick = { mostrarBuzon = false }
                                )
                            } else {
                                listaNotificaciones.forEach { alerta ->
                                    DropdownMenuItem(
                                        text = {
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text(
                                                    text = alerta.mensaje,
                                                    fontSize = 12.sp,
                                                    color = TextDark,
                                                    lineHeight = 15.sp
                                                )
                                                Text(
                                                    text = alerta.hora,
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = { mostrarBuzon = false }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onCerrarSesion) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Bienvenido, ${usuario?.displayName?.ifBlank { usuario.email?.substringBefore("@") } ?: nombreUsuario}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Centro Histórico de Lima Metropolitana",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(16.dp))

            // Panel superior con las 4 tarjetas métricas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TarjetaMetrica(
                    titulo = "Enviados",
                    cantidad = totalEnviados,
                    color = BluePrimary,
                    modifier = Modifier.weight(1f)
                )
                TarjetaMetrica(
                    titulo = "En revisión",
                    cantidad = totalEnRevision,
                    color = AmberText,
                    modifier = Modifier.weight(1f)
                )
                TarjetaMetrica(
                    titulo = "Revisados",
                    cantidad = totalRevisados,
                    color = GreenText,
                    modifier = Modifier.weight(1f)
                )
                TarjetaMetrica(
                    titulo = "Likes",
                    cantidad = totalLikes,
                    color = RedText,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Acciones del ciudadano",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextDark
            )
            Spacer(Modifier.height(8.dp))

            OpcionMenuCard(
                titulo = "Crear nuevo reporte de daño",
                subtitulo = "Fotografía, georreferenciación y severidad",
                icono = Icons.Default.Add,
                colorIcono = BluePrimary,
                onClick = onNuevoReporte
            )

            Spacer(Modifier.height(10.dp))

            OpcionMenuCard(
                titulo = "Mis reportes registrados",
                subtitulo = "Historial y seguimiento de estados",
                icono = Icons.Default.History,
                colorIcono = Color(0xFF6366F1),
                onClick = onVerHistorial
            )

            Spacer(Modifier.height(10.dp))

            OpcionMenuCard(
                titulo = "Mapa patrimonial y zonas críticas",
                subtitulo = "Cartografía offline y Algoritmo A1",
                icono = Icons.Default.Map,
                colorIcono = Color(0xFF0284C7),
                onClick = onVerMapa
            )

            Spacer(Modifier.height(10.dp))

            OpcionMenuCard(
                titulo = "Explorar comunidad y respaldo",
                subtitulo = "Validación ciudadana e interacción",
                icono = Icons.Default.Explore,
                colorIcono = Color(0xFF059669),
                onClick = onVerComunidad
            )

            if (esAdmin) {
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Administración institucional",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextDark
                )
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onIrAAdmin),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF2563EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "PROLIMA",
                                tint = Color.White
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Panel de Supervisión PROLIMA",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Fiscalización técnica y cambio de estados",
                                color = Color(0xFFBFDBFE),
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ir al panel",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TarjetaMetrica(
    titulo: String,
    cantidad: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = cantidad.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = titulo,
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OpcionMenuCard(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    colorIcono: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colorIcono.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextDark
                )
                Text(
                    text = subtitulo,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}