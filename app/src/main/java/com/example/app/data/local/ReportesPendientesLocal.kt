
package com.patrimonialima.app.data.local

import android.content.Context

object ReportesPendientesLocal {

    private const val PREFS = "sincronizacion_reportes"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    private fun clave(uid: String) = "pendientes_$uid"
    private fun claveErrores(uid: String) = "errores_$uid"

    fun registrar(
        context: Context,
        uid: String,
        idReporte: String
    ): Boolean {
        val actuales = obtener(context, uid).toMutableSet()
        actuales.add(idReporte)

        // commit() permite comprobar que el registro local
        // se escribió antes de continuar.
        return prefs(context).edit()
            .putStringSet(clave(uid), actuales)
            .commit()
    }

    fun obtener(
        context: Context,
        uid: String
    ): Set<String> {
        return prefs(context)
            .getStringSet(clave(uid), emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun quitar(
        context: Context,
        uid: String,
        idReporte: String
    ): Boolean {
        val actuales = obtener(context, uid).toMutableSet()
        actuales.remove(idReporte)

        return prefs(context).edit()
            .putStringSet(clave(uid), actuales)
            .commit()
    }

    fun registrarError(
        context: Context,
        uid: String,
        idReporte: String
    ) {
        val errores = obtenerErrores(context, uid).toMutableSet()
        errores.add(idReporte)

        prefs(context).edit()
            .putStringSet(claveErrores(uid), errores)
            .commit()
    }

    fun obtenerErrores(
        context: Context,
        uid: String
    ): Set<String> {
        return prefs(context)
            .getStringSet(claveErrores(uid), emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun quitarError(
        context: Context,
        uid: String,
        idReporte: String
    ) {
        val errores = obtenerErrores(context, uid).toMutableSet()
        errores.remove(idReporte)

        prefs(context).edit()
            .putStringSet(claveErrores(uid), errores)
            .commit()
    }

    fun preferencias(context: Context) = prefs(context)
}
