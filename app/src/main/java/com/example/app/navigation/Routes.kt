package com.patrimonialima.app.ui.navigation

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Home : Routes("home")
    object Historial : Routes("historial")
    object CrearReporte : Routes("crear_reporte")
    object Mapa : Routes("mapa")
}
