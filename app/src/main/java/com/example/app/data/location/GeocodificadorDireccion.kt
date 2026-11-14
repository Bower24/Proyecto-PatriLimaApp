package com.patrimonialima.app.data.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong
import java.util.Locale
import kotlin.coroutines.resume

object GeocodificadorDireccion {

    suspend fun buscar(
        context: Context,
        direccion: String
    ): LatLong? {

        if (direccion.isBlank()) return null

        if (!Geocoder.isPresent()) return null

        val consulta = "$direccion, Lima, Perú"

        return try {
            val geocoder = Geocoder(
                context.applicationContext,
                Locale("es", "PE")
            )

            if (Build.VERSION.SDK_INT >= 33) {

                suspendCancellableCoroutine { continuation ->

                    geocoder.getFromLocationName(
                        consulta,
                        1,
                        object : Geocoder.GeocodeListener {

                            override fun onGeocode(
                                addresses: MutableList<android.location.Address>
                            ) {
                                val resultado = addresses.firstOrNull()

                                if (continuation.isActive) {
                                    continuation.resume(
                                        resultado?.let {
                                            LatLong(
                                                it.latitude,
                                                it.longitude
                                            )
                                        }
                                    )
                                }
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                    )
                }

            } else {

                @Suppress("DEPRECATION")
                withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(
                        consulta,
                        1
                    )?.firstOrNull()?.let {
                        LatLong(
                            it.latitude,
                            it.longitude
                        )
                    }
                }
            }

        } catch (e: Exception) {
            android.util.Log.e(
                "Geocodificador",
                "No se pudo buscar la dirección",
                e
            )
            null
        }
    }
}
