package com.patrimonialima.app.ui.screens.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrimonialima.app.data.repository.AuthRepository
import com.patrimonialima.app.data.repository.FirestoreRepository
import com.patrimonialima.app.domain.model.Reporte
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistorialViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _reportes = MutableStateFlow<List<Reporte>>(emptyList())
    val reportes: StateFlow<List<Reporte>> = _reportes.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    init {
        cargarReportes()
    }

    fun cargarReportes() {
        val uid = authRepository.uidActual ?: run {
            _cargando.value = false
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            _reportes.value = firestoreRepository.obtenerReportesDeUsuario(uid)
                .sortedByDescending { it.fechaCreacionMillis }
            _cargando.value = false
        }
    }
}
