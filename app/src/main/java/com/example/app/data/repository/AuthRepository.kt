package com.patrimonialima.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Maneja el inicio de sesión, registro y cierre de sesión (RF01),
 * y crea el documento de perfil correspondiente en la colección
 * "usuarios" al registrarse.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** UID del usuario autenticado actualmente, o null si no hay sesión activa. */
    val uidActual: String? get() = auth.currentUser?.uid

    suspend fun iniciarSesion(correo: String, contrasena: String): Result<String> {
        return try {
            val resultado = auth.signInWithEmailAndPassword(correo.trim(), contrasena).await()
            val uid = resultado.user?.uid
                ?: return Result.failure(Exception("No se pudo iniciar sesión"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registrar(nombre: String, correo: String, contrasena: String): Result<String> {
        val correoSanitizado = correo.trim().lowercase()

        // NUEVO : Restricción de seguridad: Los correos institucionales de PROLIMA no pueden auto-registrarse
        if (correoSanitizado.endsWith("@prolima.gob.pe") || correoSanitizado.endsWith("@patrimonio.pe")) {
            return Result.failure(
                IllegalArgumentException("Los correos institucionales de PROLIMA son asignados internamente y no admiten registro público.")
            )
        }

        return try {
            val resultado = auth.createUserWithEmailAndPassword(correoSanitizado, contrasena).await()
            val uid = resultado.user?.uid
                ?: return Result.failure(Exception("No se pudo crear la cuenta"))

            val datosUsuario = hashMapOf(
                "nombre" to nombre.trim(),
                "correo" to correoSanitizado,
                "rol" to "ciudadano",
                "fecha_registro" to Timestamp.now()
            )
            db.collection("usuarios").document(uid).set(datosUsuario).await()

            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}