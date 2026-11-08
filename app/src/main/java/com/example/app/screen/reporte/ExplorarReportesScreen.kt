package com.patrimonialima.app.ui.screens.reportes

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ReporteComunidad(
    val id: String = "",
    val direccion: String = "",
    val descripcion: String = "",
    val severidad: String = "",
    val estado: String = "",
    val fotoBase64: String? = null,
    val likesCount: Long = 0,
    val dioLike: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorarReportesScreen(onVolver: () -> Unit) {
    val uidActual = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val db = remember { FirebaseFirestore.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    var reportes by remember { mutableStateOf<List<ReporteComunidad>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // Sincronización en tiempo real con verificación de likes persistentes
    DisposableEffect(uidActual) {
        val listener = db.collection("reportes")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    cargando = false
                    return@addSnapshotListener
                }

                coroutineScope.launch {
                    val lista = mutableListOf<ReporteComunidad>()

                    for (doc in snapshot.documents) {
                        val repId = doc.id
                        val likes = when (val l = doc.get("likes_count") ?: doc.get("likes")) {
                            is Number -> l.toLong()
                            is String -> l.toLongOrNull() ?: 0L
                            else -> 0L
                        }

                        // Comprobar si el usuario actual dio like revisando su subcolección
                        var yaDioLike = false
                        if (uidActual.isNotBlank()) {
                            try {
                                val likeDoc = db.collection("reportes")
                                    .document(repId)
                                    .collection("likes")
                                    .document(uidActual)
                                    .get()
                                    .await()
                                yaDioLike = likeDoc.exists()
                            } catch (_: Exception) {}
                        }

                        lista.add(
                            ReporteComunidad(
                                id = repId,
                                direccion = doc.getString("direccion") ?: "Centro Histórico de Lima",
                                descripcion = doc.getString("descripcion") ?: "",
                                severidad = doc.getString("severidad") ?: "LEVE",
                                estado = doc.getString("estado") ?: "ENVIADO",
                                fotoBase64 = doc.getString("fotoBase64"),
                                likesCount = likes,
                                dioLike = yaDioLike
                            )
                        )
                    }

                    reportes = lista
                    cargando = false
                }
            }

        onDispose {
            listener.remove()
        }
    }

    fun toggleLike(reporte: ReporteComunidad) {
        if (uidActual.isBlank()) return

        val nuevoEstado = !reporte.dioLike
        val delta = if (nuevoEstado) 1L else -1L
        val nuevoConteo = (reporte.likesCount + delta).coerceAtLeast(0)

        // Actualización optimista inmediata en la UI
        reportes = reportes.map {
            if (it.id == reporte.id) it.copy(dioLike = nuevoEstado, likesCount = nuevoConteo)
            else it
        }

        val reporteRef = db.collection("reportes").document(reporte.id)
        val likeRef = reporteRef.collection("likes").document(uidActual)

        db.runTransaction { transaction ->
            val likeSnapshot = transaction.get(likeRef)
            val repSnapshot = transaction.get(reporteRef)

            val conteoActual = when (val l = repSnapshot.get("likes_count") ?: repSnapshot.get("likes")) {
                is Number -> l.toLong()
                is String -> l.toLongOrNull() ?: 0L
                else -> 0L
            }

            if (likeSnapshot.exists()) {
                transaction.delete(likeRef)
                val finalCount = (conteoActual - 1).coerceAtLeast(0)
                transaction.update(
                    reporteRef,
                    "likes_count",
                    finalCount
                )
            } else {
                transaction.set(
                    likeRef,
                    mapOf(
                        "fecha" to FieldValue.serverTimestamp(),
                        "uid" to uidActual
                    )
                )
                val finalCount = conteoActual + 1
                transaction.update(
                    reporteRef,
                    "likes_count",
                    finalCount
                )
            }
        }.addOnFailureListener { error ->
            android.util.Log.e("Likes", "Error en transacción de like", error)
            // Revertir estado si falla la red o las reglas de seguridad
            reportes = reportes.map {
                if (it.id == reporte.id) reporte else it
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes de la Comunidad", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E3A8A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1E3A8A))
            }
        } else if (reportes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay reportes registrados aún.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8FAFC)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(reportes, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            item.fotoBase64?.let { base64Str ->
                                val bitmap = remember(base64Str) { decodificarBase64Seguro(base64Str) }
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Foto reporte",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(10.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.direccion,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = item.severidad.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (item.severidad.equals("SEVERO", ignoreCase = true)) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                                        labelColor = if (item.severidad.equals("SEVERO", ignoreCase = true)) Color(0xFFDC2626) else Color(0xFFD97706)
                                    )
                                )
                            }

                            if (item.descripcion.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(item.descripcion, fontSize = 12.sp, color = Color(0xFF64748B))
                            }

                            Spacer(Modifier.height(12.dp))
                            Divider(color = Color(0xFFF1F5F9))
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estado: ${item.estado}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Button(
                                    onClick = { toggleLike(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (item.dioLike) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                                        contentColor = if (item.dioLike) Color(0xFFEF4444) else Color(0xFF64748B)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.dioLike) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Like",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (item.dioLike) Color(0xFFEF4444) else Color(0xFF64748B)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("${item.likesCount}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decodificarBase64Seguro(base64String: String): Bitmap? {
    return try {
        val pureBase64 = if (base64String.contains(",")) {
            base64String.substringAfter(",")
        } else {
            base64String
        }
        val decodedBytes = Base64.decode(pureBase64.trim(), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (_: Throwable) {
        null
    }
}