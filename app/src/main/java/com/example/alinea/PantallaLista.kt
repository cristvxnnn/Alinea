package com.example.alinea

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Text("Elige tu ejercicio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                grupos.forEach { (categoria, lista) ->
                    item(key = "cat_$categoria") {
                        Text(
                            categoria.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(lista, key = { it.id }) { reto -> TarjetaReto(reto, onElegir) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaReto(reto: Reto, onElegir: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onElegir(reto.id) },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ImagenPose(
                nombre = reto.imagen,
                descripcion = "Postura de ${reto.nombre}",
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
            Text(reto.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (reto.instrucciones.isNotBlank()) {
                Text(
                    reto.instrucciones,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Etiqueta("Sostener ${reto.tiempoObjetivo.toInt()} s")
                Etiqueta("${reto.criterios.size} criterio(s)")
            }
            Text(
                "Ver ejercicio  ›",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(
        texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1D4E89),
        modifier = Modifier
            .background(Color(0xFFE1EDFA), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}