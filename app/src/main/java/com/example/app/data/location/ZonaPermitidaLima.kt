
package com.patrimonialima.app.data.location

import org.mapsforge.core.model.LatLong

object ZonaPermitidaLima {


    private const val LAT_MIN = -12.0650
    private const val LAT_MAX = -12.0350

    private const val LON_MIN = -77.0550
    private const val LON_MAX = -77.0150

    val centro = LatLong(
        -12.0465,
        -77.0305
    )

    fun contiene(
        latitud: Double,
        longitud: Double
    ): Boolean {
        return latitud.isFinite() &&
                longitud.isFinite() &&
                latitud in LAT_MIN..LAT_MAX &&
                longitud in LON_MIN..LON_MAX
    }

    fun contiene(posicion: LatLong): Boolean {
        return contiene(
            posicion.latitude,
            posicion.longitude
        )
    }
}
