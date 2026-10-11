package com.patrimonialima.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrimonialima.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Cargando : AuthUiState()
    data class Error(val mensaje: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun iniciarSesion(correo: String, contrasena: String, alExito: () -> Unit) {
        if (correo.isBlank() || contrasena.isBlank()) {
            _uiState.value = AuthUiState.Error("Ingresa tu correo y contraseña")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Cargando
            authRepository.iniciarSesion(correo, contrasena)
                .onSuccess {
                    _uiState.value = AuthUiState.Idle
                    alExito()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Error al iniciar sesión")
                }
        }
    }

    fun registrar(nombre: String, correo: String, contrasena: String, alExito: () -> Unit) {
        if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank()) {
            _uiState.value = AuthUiState.Error("Completa todos los campos")
            return
        }
        if (contrasena.length < 6) {
            _uiState.value = AuthUiState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Cargando
            authRepository.registrar(nombre, correo, contrasena)
                .onSuccess {
                    _uiState.value = AuthUiState.Idle
                    alExito()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Error al crear la cuenta")
                }
        }
    }
}
