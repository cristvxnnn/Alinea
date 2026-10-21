package com.example.alinea

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alinea.data.CatalogoRetos

@Composable
fun AppNavegacion() {
    val contexto = LocalContext.current
    val nav = rememberNavController()
    val catalogo = remember { runCatching { CatalogoRetos.cargar(contexto) } }

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = "lista") {
            composable("lista") {
                PantallaLista(
                    retos = catalogo.getOrDefault(emptyList()),
                    error = catalogo.exceptionOrNull()?.message,
                    onElegir = { id -> nav.navigate("reto/$id") },
                )
            }
            composable(
                route = "reto/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entrada ->
                val id = entrada.arguments?.getString("id")
                val reto = catalogo.getOrNull()?.firstOrNull { it.id == id }
                PantallaCamara(reto = reto, onVolver = { nav.popBackStack() })
            }
        }
    }
}