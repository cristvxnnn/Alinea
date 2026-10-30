package com.example.alinea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alinea.core.Reto

@Composable
fun PantallaPreparacion(reto: Reto?, onComenzar: () -> Unit, onVolver: () -> Unit) {
    if (reto == null) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No se encontró el ejercicio.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onVolver) { Text("Volver a ejercicios") }
        }
    } else {
        ContenidoPreparacion(reto, onComenzar, onVolver)
    }
}

@Composable
private fun ContenidoPreparacion(reto: Reto, onComenzar: () -> Unit, onVolver: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onVolver) { Text("←  Ejercicios", fontSize = 15.sp) }

            ImagenPose(
                nombre = reto.imagen,
                descripcion = "Postura de ${reto.nombre}",
                modifier = Modifier.fillMaxWidth().aspectRatio(1.15f),
            )

            Column {
                Text(
                    reto.categoria.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(reto.nombre, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            }

            if (reto.instrucciones.isNotBlank()) {
                Text(reto.instrucciones, style = MaterialTheme.typography.bodyLarge)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("Sostener ${reto.tiempoObjetivo.toInt()} s")
                Etiqueta("${reto.criterios.size} criterio(s)")
            }

            if (reto.pasos.isNotEmpty()) {
                Text("Cómo hacerlo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                reto.pasos.forEachIndexed { i, paso -> Paso(i + 1, paso) }
            }

            Text(
                "Detente si sientes dolor. Alinea no es una herramienta médica.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4A607A),
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xF2F3F8FD))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onComenzar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text("Comenzar ejercicio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Paso(numero: Int, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(numero.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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