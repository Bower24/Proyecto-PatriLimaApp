package com.patrimonialima.app.ui.screens.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.patrimonialima.app.data.repository.FirestoreRepository
import com.patrimonialima.app.domain.model.BienPatrimonial
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

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            _bienes.value = firestoreRepository.obtenerBienesPatrimoniales()

            val reportes = firestoreRepository.obtenerTodosLosReportes()
            _reportesMapa.value = reportes
                .filter { it.latitud != 0.0 && it.longitud != 0.0 }
                .map { reporte ->
                    ReporteMapa(
                        id = reporte.id,
                        posicion = LatLng(reporte.latitud, reporte.longitud),
                        severidad = reporte.severidad
                    )
                }
        }
    }
}
