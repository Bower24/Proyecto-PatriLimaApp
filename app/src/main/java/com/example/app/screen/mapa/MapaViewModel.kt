package com.patrimonialima.app.ui.screens.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.patrimonialima.app.data.repository.FirestoreRepository
import com.patrimonialima.app.domain.CalculadorZonasCriticas
import com.patrimonialima.app.domain.ZonaCritica
import com.patrimonialima.app.domain.model.BienPatrimonial
import com.patrimonialima.app.domain.model.ReporteMapa
import com.patrimonialima.app.domain.model.Severidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



class MapaViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _bienes = MutableStateFlow<List<BienPatrimonial>>(emptyList())
    val bienes: StateFlow<List<BienPatrimonial>> = _bienes.asStateFlow()

    private val _reportesMapa = MutableStateFlow<List<ReporteMapa>>(emptyList())
    val reportesMapa: StateFlow<List<ReporteMapa>> = _reportesMapa.asStateFlow()

    // Exposición de las zonas críticas calculadas mediante Algoritmo A1
    private val _zonasCriticas = MutableStateFlow<List<ZonaCritica>>(emptyList())
    val zonasCriticas: StateFlow<List<ZonaCritica>> = _zonasCriticas.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                _bienes.value = firestoreRepository.obtenerBienesPatrimoniales()

                val reportes = firestoreRepository.obtenerTodosLosReportes()

                // Mapeo para los pines en el mapa
                _reportesMapa.value = reportes
                    .filter { it.latitud != 0.0 && it.longitud != 0.0 }
                    .map { reporte ->
                        ReporteMapa(
                            id = reporte.id,
                            posicion = LatLng(reporte.latitud, reporte.longitud),
                            severidad = reporte.severidad,
                            descripcion = reporte.descripcion
                        )
                    }

                // Ejecución del Algoritmo A1 de agrupamiento y ranking
                _zonasCriticas.value = CalculadorZonasCriticas.calcular(
                    reportes = reportes,
                    radioMetros = 100.0,
                    topK = 5
                )
            } finally {
                _cargando.value = false
            }
        }
    }
}