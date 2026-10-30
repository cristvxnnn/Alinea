package com.example.alinea

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaAjustes(onBorrarHistorial: () -> Unit) {
    var confirmar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajustes", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacidad", style = MaterialTheme.typography.titleMedium)
                Text(
                    "El análisis de tu cuerpo ocurre en tu teléfono. No se guardan ni se envían " +
                            "imágenes ni video. Tu historial se guarda solo en este dispositivo. " +
                            "La librería de IA de Google (MediaPipe) puede enviar métricas técnicas de " +
                            "rendimiento y uso, sin imágenes.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tus datos", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Borra todas tus sesiones y resultados de este teléfono. No se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { confirmar = true }) { Text("Borrar mi historial") }
            }
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("¿Borrar tu historial?") },
            text = { Text("Se eliminarán todas tus sesiones y resultados. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    onBorrarHistorial()
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } },
        )
    }
}