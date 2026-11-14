
package com.patrimonialima.app.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong
import java.util.Locale
import kotlin.coroutines.resume

object GeocodificadorDireccion {

    private const val LAT_MIN = -12.0650
    private const val LAT_MAX = -12.0350
    private const val LON_MIN = -77.0550
    private const val LON_MAX = -77.0150

    suspend fun buscar(
        context: Context,
        direccion: String
    ): LatLong? {

        if (direccion.isBlank()) return null
        if (!Geocoder.isPresent()) return null

        val consulta =
            "${direccion.trim()}, Cercado de Lima, Lima, Perú"

        return try {

            val geocoder = Geocoder(
                context.applicationContext,
                Locale("es", "PE")
            )

            val direcciones: List<Address> =
                if (Build.VERSION.SDK_INT >= 33) {

                    suspendCancellableCoroutine { continuation ->

                        geocoder.getFromLocationName(
                            consulta,
                            5,
                            LAT_MIN,
                            LON_MIN,
                            LAT_MAX,
                            LON_MAX,
                            object : Geocoder.GeocodeListener {

                                override fun onGeocode(
                                    addresses: MutableList<Address>
                                ) {
                                    if (continuation.isActive) {
                                        continuation.resume(addresses)
                                    }
                                }

                                override fun onError(
                                    errorMessage: String?
                                ) {
                                    if (continuation.isActive) {
                                        continuation.resume(emptyList())
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
                            5,
                            LAT_MIN,
                            LON_MIN,
                            LAT_MAX,
                            LON_MAX
                        ) ?: emptyList()
                    }
                }

            // No confiar solamente en el filtro
            // del proveedor de geocodificación.
            val resultado = direcciones.firstOrNull {
                ZonaPermitidaLima.contiene(
                    it.latitude,
                    it.longitude
                )
            }

            resultado?.let {
                LatLong(it.latitude, it.longitude)
            }

        } catch (e: Exception) {
            android.util.Log.e(
                "Geocodificador",
                "Error buscando dirección",
                e
            )
            null
        }
    }

    suspend fun buscarDireccion(
        context: Context,
        latitud: Double,
        longitud: Double
    ): String? {

        if (!Geocoder.isPresent()) return null

        return try {

            val geocoder = Geocoder(
                context.applicationContext,
                Locale("es", "PE")
            )

            val direcciones =
                if (Build.VERSION.SDK_INT >= 33) {

                    suspendCancellableCoroutine<List<android.location.Address>> {
                            continuation ->

                        geocoder.getFromLocation(
                            latitud,
                            longitud,
                            1,
                            object : Geocoder.GeocodeListener {

                                override fun onGeocode(
                                    addresses: MutableList<android.location.Address>
                                ) {
                                    if (continuation.isActive) {
                                        continuation.resume(addresses)
                                    }
                                }

                                override fun onError(errorMessage: String?) {
                                    if (continuation.isActive) {
                                        continuation.resume(emptyList())
                                    }
                                }
                            }
                        )
                    }

                } else {

                    @Suppress("DEPRECATION")
                    withContext(Dispatchers.IO) {
                        geocoder.getFromLocation(
                            latitud,
                            longitud,
                            1
                        ) ?: emptyList()
                    }
                }

            val resultado = direcciones.firstOrNull()
                ?: return null

            val calle = resultado.thoroughfare
            val numero = resultado.subThoroughfare

            when {
                !calle.isNullOrBlank() &&
                        !numero.isNullOrBlank() ->
                    "$calle $numero, Cercado de Lima"

                !calle.isNullOrBlank() ->
                    "$calle, Cercado de Lima"

                else ->
                    resultado.getAddressLine(0)
            }

        } catch (e: Exception) {
            android.util.Log.e(
                "Geocodificador",
                "Error obteniendo dirección",
                e
            )
            null
        }
    }

}
