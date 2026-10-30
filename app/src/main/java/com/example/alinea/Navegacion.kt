package com.example.alinea

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alinea.core.MetasDiarias
import com.example.alinea.core.calcularProgreso
import com.example.alinea.core.diaLocal
import com.example.alinea.data.AppDatabase
import com.example.alinea.data.CatalogoRetos
import com.example.alinea.data.ProgresoRepositorio
import android.util.Log
import com.example.alinea.data.ProgresoAlcance
import kotlinx.coroutines.launch
import java.util.TimeZone

private data class Destino(val ruta: String, val titulo: String, val icono: ImageVector)

private val DESTINOS = listOf(
    Destino("inicio", "Inicio", Icons.Default.Home),
    Destino("lista", "Ejercicios", Icons.Default.PlayArrow),
    Destino("progreso", "Progreso", Icons.Default.DateRange),
    Destino("ajustes", "Ajustes", Icons.Default.Settings),
)

private val RUTAS_CON_BARRA = setOf("inicio", "lista", "progreso", "ajustes", "rutinas")

private fun diaDeHoy(): Long {
    val ahora = System.currentTimeMillis()
    return diaLocal(ahora, TimeZone.getDefault().getOffset(ahora))
}

@Composable
fun AppNavegacion() {
    val contexto = LocalContext.current
    val nav = rememberNavController()
    val catalogo = remember { runCatching { CatalogoRetos.cargar(contexto) } }
    val nombres = remember { catalogo.getOrDefault(emptyList()).associate { it.id to it.nombre } }
    val repo = remember { ProgresoRepositorio(AppDatabase.obtener(contexto).progresoDao()) }
    val resultados by remember { repo.observarResultados() }.collectAsState(initial = emptyList())
    val metas = remember { MetasDiarias() }
    val resumen = remember(resultados) { calcularProgreso(resultados, diaDeHoy(), metas) }

    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route

    fun irA(ruta: String) {
        nav.navigate(ruta) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // Cada pantalla gestiona sus propios márgenes del sistema
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            bottomBar = {
                if (rutaActual in RUTAS_CON_BARRA) {
                    NavigationBar {
                        DESTINOS.forEach { d ->
                            NavigationBarItem(
                                selected = rutaActual == d.ruta,
                                onClick = { irA(d.ruta) },
                                icon = { Icon(d.icono, contentDescription = null) },
                                label = { Text(d.titulo) },
                            )
                        }
                    }
                }
            },
        ) { relleno ->
            NavHost(
                navController = nav,
                startDestination = "inicio",
                modifier = Modifier.padding(relleno),
            ) {
                composable("inicio") {
                    PantallaInicio(
                        resumen = resumen,
                        metas = metas,
                        onEntrenar = { irA("lista") },
                        onEjercicios = { irA("lista") },
                        onRutinas = { irA("rutinas") },
                        onProgreso = { irA("progreso") },
                        onAjustes = { irA("ajustes") },
                    )
                }
                composable("lista") {
                    PantallaLista(
                        retos = catalogo.getOrDefault(emptyList()),
                        error = catalogo.exceptionOrNull()?.message,
                        onElegir = { id -> nav.navigate("preparacion/$id") },
                    )
                }
                composable("rutinas") { PantallaProximamente("Rutinas") }
                composable("progreso") { PantallaProgreso(resultados, nombres, diaDeHoy()) }
                composable("ajustes") {
                    PantallaAjustes(onBorrarHistorial = {
                        ProgresoAlcance.alcance.launch {
                            try {
                                repo.borrarHistorial()
                            } catch (e: Exception) {
                                Log.e("Ajustes", "No se pudo borrar el historial: ${e.message}")
                            }
                        }
                    })
                }
                composable(
                    route = "preparacion/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { e ->
                    val id = e.arguments?.getString("id")
                    val reto = catalogo.getOrNull()?.firstOrNull { it.id == id }
                    PantallaPreparacion(
                        reto = reto,
                        onComenzar = {
                            // Reemplaza la preparación por la cámara: atrás desde la cámara vuelve a la lista
                            nav.navigate("reto/$id") {
                                popUpTo("preparacion/{id}") { inclusive = true }
                            }
                        },
                        onVolver = { nav.popBackStack() },
                    )
                }
                composable(
                    route = "reto/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { e ->
                    val id = e.arguments?.getString("id")
                    val reto = catalogo.getOrNull()?.firstOrNull { it.id == id }
                    PantallaCamara(reto = reto, onVolver = { nav.popBackStack() })
                }

            }
        }
    }
}