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

data class HomeUiState(
    val nombreUsuario: String = "Usuario",
    val contribucion: ContribucionUsuario = ContribucionUsuario(0, 0, 0)
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

    private fun cargarDatosUsuario() {
        val uid = authRepository.uidActual ?: return
        viewModelScope.launch {
            val nombre = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email
                ?: "Usuario"

            val reportes = firestoreRepository.obtenerReportesDeUsuario(uid)
            val enviados = reportes.size
            val enRevision = reportes.count { it.estado == EstadoReporte.EN_REVISION }
            val likes = reportes.sumOf { it.likes }

            _uiState.value = HomeUiState(
                nombreUsuario = nombre,
                contribucion = ContribucionUsuario(enviados, enRevision, likes)
            )
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }
}
