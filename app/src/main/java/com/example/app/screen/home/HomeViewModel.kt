package com.patrimonialima.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.patrimonialima.app.data.repository.AuthRepository
import com.patrimonialima.app.data.repository.FirestoreRepository
import com.patrimonialima.app.domain.model.EstadoReporte
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContribucionUsuario(
    val enviados: Int = 0,
    val enRevision: Int = 0,
    val revisados: Int = 0,
    val likes: Int = 0
)

data class HomeUiState(
    val nombreUsuario: String = "Usuario",
    val contribucion: ContribucionUsuario = ContribucionUsuario()
)

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        cargarDatosUsuario()
    }

    fun cargarDatosUsuario() {
        val uid = authRepository.uidActual ?: return
        viewModelScope.launch {
            val nombre = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email
                ?: "Usuario"

            val reportes = firestoreRepository.obtenerReportesDeUsuario(uid)

            // Si "Enviados" son todos los creados históricamente por el usuario:
            val totalCreados = reportes.size
            // Si prefieres estrictamente los que están en estado ENVIADO:
            // val totalEnviados = reportes.count { it.estado == EstadoReporte.ENVIADO }

            val enRevision = reportes.count { it.estado == EstadoReporte.EN_REVISION }
            val revisados = reportes.count { it.estado == EstadoReporte.REVISADO }
            val likes = reportes.sumOf { it.likes }

            _uiState.value = HomeUiState(
                nombreUsuario = nombre,
                contribucion = ContribucionUsuario(
                    enviados = totalCreados,
                    enRevision = enRevision,
                    revisados = revisados,
                    likes = likes
                )
            )
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }
}