
package com.patrimonialima.app.ui.screens.reporte

import androidx.lifecycle.ViewModel
import com.patrimonialima.app.data.repository.AuthRepository
import com.patrimonialima.app.data.repository.FirestoreRepository
import com.patrimonialima.app.domain.model.Severidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Context

sealed class EnvioReporteEstado {
    object Idle : EnvioReporteEstado()
    object Enviando : EnvioReporteEstado()

    data class EnCola(
        val idReporte: String
    ) : EnvioReporteEstado()

    // Se conserva para compatibilidad con la navegación actual.
    object Exito : EnvioReporteEstado()

    data class Error(
        val mensaje: String
    ) : EnvioReporteEstado()
}

class CrearReporteViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _estado =
        MutableStateFlow<EnvioReporteEstado>(
            EnvioReporteEstado.Idle
        )

    val estado: StateFlow<EnvioReporteEstado> =
        _estado.asStateFlow()


    fun enviarReporte(
        context: Context,
        descripcion: String,
        severidad: Severidad,
        direccion: String,
        fotoBase64: String? = null
    ) {
        if (_estado.value is EnvioReporteEstado.Enviando ||
            _estado.value is EnvioReporteEstado.EnCola
        ) {
            return
        }

        val idUsuario = authRepository.uidActual

        if (idUsuario == null) {
            _estado.value = EnvioReporteEstado.Error(
                "Debes iniciar sesión para enviar un reporte"
            )
            return
        }

        if (descripcion.isBlank() || direccion.isBlank()) {
            _estado.value = EnvioReporteEstado.Error(
                "Completa la dirección y la descripción"
            )
            return
        }

        _estado.value = EnvioReporteEstado.Enviando

        val resultado = firestoreRepository.crearReporte(
            context = context.applicationContext,
            idUsuario = idUsuario,
            descripcion = descripcion.trim(),
            direccion = direccion.trim(),
            severidad = severidad,
            fotoBase64 = fotoBase64
        )

        resultado
            .onSuccess { idReporte ->
                _estado.value = EnvioReporteEstado.EnCola(
                    idReporte
                )
            }
            .onFailure { error ->
                _estado.value = EnvioReporteEstado.Error(
                    error.message
                        ?: "No se pudo registrar el reporte"
                )
            }
    }


    fun reiniciarEstado() {
        _estado.value = EnvioReporteEstado.Idle
    }
}
