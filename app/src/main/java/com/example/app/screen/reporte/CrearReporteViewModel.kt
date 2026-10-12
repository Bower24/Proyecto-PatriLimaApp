package com.patrimonialima.app.ui.screens.reporte

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrimonialima.app.data.repository.AuthRepository
import com.patrimonialima.app.data.repository.FirestoreRepository
import com.patrimonialima.app.data.repository.StorageRepository
import com.patrimonialima.app.domain.model.Severidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EnvioReporteEstado {
    object Idle : EnvioReporteEstado()
    object Enviando : EnvioReporteEstado()
    object Exito : EnvioReporteEstado()
    data class Error(val mensaje: String) : EnvioReporteEstado()
}

class CrearReporteViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(),
    private val storageRepository: StorageRepository = StorageRepository()
) : ViewModel() {

    private val _estado = MutableStateFlow<EnvioReporteEstado>(EnvioReporteEstado.Idle)
    val estado: StateFlow<EnvioReporteEstado> = _estado.asStateFlow()

    fun enviarReporte(
        descripcion: String,
        severidad: Severidad,
        direccion: String,
        fotoBase64: String? = null
    ) {
        val idUsuario = authRepository.uidActual
        if (idUsuario == null) {
            _estado.value = EnvioReporteEstado.Error("Debes iniciar sesión para enviar un reporte")
            return
        }

        viewModelScope.launch {
            _estado.value = EnvioReporteEstado.Enviando

            val resultado = firestoreRepository.crearReporte(
                idUsuario = idUsuario,
                descripcion = descripcion,
                direccion = direccion,
                severidad = severidad,
                fotoBase64 = fotoBase64
            )

            resultado
                .onSuccess { _estado.value = EnvioReporteEstado.Exito }
                .onFailure { error ->
                    _estado.value = EnvioReporteEstado.Error(error.message ?: "No se pudo enviar el reporte")
                }
        }
    }

    fun reiniciarEstado() {
        _estado.value = EnvioReporteEstado.Idle
    }
}