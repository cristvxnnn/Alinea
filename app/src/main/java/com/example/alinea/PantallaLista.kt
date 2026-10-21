package com.example.alinea

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alinea.core.Reto

@Composable
fun PantallaLista(
    retos: List<Reto>,
    error: String?,
    onElegir: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Alinea", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Elige un ejercicio", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Los ejercicios son sugerencias: detente si sientes dolor. Esta app no es una herramienta médica.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text(
                "No se pudo cargar ejercicios.json: $error",
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            val grupos = retos.groupBy { it.categoria }
            LazyColumn(
                modifier = Modifier.navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                grupos.forEach { (categoria, lista) ->
                    item(key = "cat_$categoria") {
                        Text(
                            categoria,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(lista, key = { it.id }) { reto ->
                        TarjetaReto(reto, onElegir)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaReto(reto: Reto, onElegir: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onElegir(reto.id) }) {
        Column(Modifier.padding(16.dp)) {
            Text(reto.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (reto.instrucciones.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(reto.instrucciones, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Sostener ${reto.tiempoObjetivo.toInt()} s · ${reto.criterios.size} criterio(s)",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}