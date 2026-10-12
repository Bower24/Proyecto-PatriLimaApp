package com.patrimonialima.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.patrimonialima.app.ui.screens.auth.AuthUiState
import com.patrimonialima.app.ui.screens.auth.AuthViewModel
import com.patrimonialima.app.ui.screens.auth.LoginScreen
import com.patrimonialima.app.ui.screens.historial.HistorialReportesScreen
import com.patrimonialima.app.ui.screens.historial.HistorialViewModel
import com.patrimonialima.app.ui.screens.home.HomeScreen
import com.patrimonialima.app.ui.screens.home.HomeViewModel
import com.patrimonialima.app.ui.screens.mapa.MapaInteractivoScreen
import com.patrimonialima.app.ui.screens.mapa.MapaViewModel
import com.patrimonialima.app.ui.screens.reporte.CrearReporteScreen
import com.patrimonialima.app.ui.screens.reporte.CrearReporteViewModel
import androidx.compose.ui.platform.LocalContext

/**
 * Grafo de navegación de la aplicación. "login" es el punto de partida;
 * al autenticarse exitosamente se navega a "home" limpiando el backstack
 * para que el botón "atrás" no regrese a la pantalla de login.
 */
@Composable
fun PatrimonioLimaNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.Login.route) {

        composable(Routes.Login.route) {
            val viewModel: AuthViewModel = viewModel()
            val estado by viewModel.uiState.collectAsState()

            LoginScreen(
                cargando = estado is AuthUiState.Cargando,
                mensajeError = (estado as? AuthUiState.Error)?.mensaje,
                onLoginSuccess = { correo, contrasena ->
                    viewModel.iniciarSesion(correo, contrasena) {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    }
                },
                onRegisterSuccess = { nombre, correo, contrasena ->
                    viewModel.registrar(nombre, correo, contrasena) {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.Home.route) {
            val viewModel: HomeViewModel = viewModel()
            val estado by viewModel.uiState.collectAsState()

            HomeScreen(
                nombreUsuario = estado.nombreUsuario,
                contribucion = estado.contribucion,
                onNuevoReporte = { navController.navigate(Routes.CrearReporte.route) },
                onVerHistorial = { navController.navigate(Routes.Historial.route) },
                onVerMapa = { navController.navigate(Routes.Mapa.route) },
                onCerrarSesion = {
                    viewModel.cerrarSesion()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Historial.route) {
            val viewModel: HistorialViewModel = viewModel()
            val reportes by viewModel.reportes.collectAsState()

            HistorialReportesScreen(
                reportes = reportes,
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Routes.CrearReporte.route) {
            val context = LocalContext.current
            val crearReporteViewModel: CrearReporteViewModel = viewModel()

            CrearReporteScreen(
                onVolver = { navController.popBackStack() },
                onEnviarReporte = { descripcion, severidad, direccion ->
                    crearReporteViewModel.enviarReporte(
                        context = context,
                        descripcion = descripcion,
                        severidad = severidad,
                        direccion = direccion,
                        fotoUri = null
                    )
                }
            )
        }

        composable(Routes.Mapa.route) {
            val mapaViewModel: MapaViewModel = viewModel()

            val bienes by mapaViewModel.bienes.collectAsState()
            val reportes by mapaViewModel.reportesMapa.collectAsState()
            val zonasCriticas by mapaViewModel.zonasCriticas.collectAsState()

            MapaInteractivoScreen(
                bienes = bienes,
                reportes = reportes,
                zonasCriticas = zonasCriticas,
                onVolver = { navController.popBackStack() }
            )

            //MapaMapsforgeScreen()
        }
    }
}
