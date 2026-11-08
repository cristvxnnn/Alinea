package com.example.alinea

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alinea.core.*
import kotlin.math.roundToInt

private val AZUL = Color(0xFF1A73E8)
private val VERDE_OK = Color(0xFF0E9F6E)
private val FONDO = Color(0xFFF3F8FD)
private val TEXTO = Color(0xFF0B2A4A)
private val TEXTO_SEC = Color(0xFF4A607A)
private val PISTA = Color(0xFFE1EDFA)

@Composable
fun PantallaResultado(
    reto: Reto,
    resumen: ResumenIntento?,
    hoy: ResumenProgreso,
    metas: MetasDiarias,
    onRepetir: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var desgloseAbierto by remember { mutableStateOf(false) }
    val segundos = reto.tiempoObjetivo.toInt()

    Box(
        modifier
            .background(FONDO)
            // Bloquea los toques hacia la cámara que está debajo
            .pointerInput(Unit) {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 168.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Encabezado(reto, segundos)
            TarjetaEjercicio(reto, segundos)

            if (resumen != null) {
                Text("Cómo te fue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TEXTO)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Metrica(
                        Modifier.weight(1f), "Tiempo sostenido", "$segundos s", "Meta cumplida", 1f,
                    )
                    Metrica(
                        Modifier.weight(1f), "Postura correcta",
                        "${(resumen.precision * 100).roundToInt()} %", "del tiempo sostenido",
                        resumen.precision,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Metrica(
                        Modifier.weight(1f), "Intentos", "${resumen.intentos}",
                        if (resumen.reinicios == 0) "Lo lograste a la primera"
                        else "${resumen.reinicios} reinicio(s) antes",
                        null,
                    )
                    Metrica(
                        Modifier.weight(1f), "Racha", "${hoy.racha}",
                        if (hoy.racha == 1) "día seguido" else "días seguidos", null,
                    )
                }
                TarjetaMasExigido(resumen)
            }

            TarjetaHoy(hoy, metas)

            if (resumen != null) {
                Desglose(resumen, desgloseAbierto) { desgloseAbierto = !desgloseAbierto }
            }

            Text(
                "Tu video no sale del teléfono.",
                fontSize = 12.sp,
                color = TEXTO_SEC,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xF2F3F8FD))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onRepetir,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text("Repetir ejercicio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
                Text("Volver a la lista", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Encabezado(reto: Reto, segundos: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.background(PISTA, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(7.dp).background(VERDE_OK, CircleShape))
            Text("EJERCICIO COMPLETADO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4E89))
        }
        Box(Modifier.size(96.dp).background(VERDE_OK.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(68.dp).background(VERDE_OK, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        Text("¡Reto superado!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TEXTO, textAlign = TextAlign.Center)
        Text(
            "Sostuviste ${reto.nombre} durante $segundos s.",
            fontSize = 15.sp,
            color = TEXTO_SEC,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TarjetaEjercicio(reto: Reto, segundos: Int) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ImagenPose(
                nombre = reto.imagen,
                descripcion = "Postura de ${reto.nombre}",
                modifier = Modifier.size(width = 88.dp, height = 112.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(reto.categoria.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AZUL)
                Text(reto.nombre, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = TEXTO)
                Text(
                    "$segundos/$segundos s",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0E7C58),
                    modifier = Modifier
                        .background(VERDE_OK.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun Metrica(
    modifier: Modifier,
    titulo: String,
    valor: String,
    detalle: String,
    progreso: Float?,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TEXTO_SEC)
            Text(valor, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TEXTO)
            Text(detalle, fontSize = 12.sp, color = TEXTO_SEC)
            if (progreso != null) {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).background(PISTA, RoundedCornerShape(3.dp))) {
                    Box(
                        Modifier
                            .fillMaxWidth(progreso.coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(VERDE_OK, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaMasExigido(resumen: ResumenIntento) {
    val d = resumen.masExigido
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(if (d == null) VERDE_OK.copy(alpha = 0.15f) else Color(0xFFFFF4D6), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (d == null) "✓" else "!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TEXTO)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Lo que más te costó", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TEXTO_SEC)
                Text(
                    if (d == null) "Todas las articulaciones se mantuvieron en rango"
                    else "${d.etiqueta}: fuera de rango el ${resumen.pctFuera(d)} % del tiempo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TEXTO,
                )
            }
        }
    }
}

@Composable
private fun TarjetaHoy(hoy: ResumenProgreso, metas: MetasDiarias) {
    val segmentos = metas.ejercicios.coerceIn(1, 10)
    val llenos = hoy.completadosHoy.coerceAtMost(segmentos)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Hoy", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TEXTO)
                Text(
                    if (hoy.completadosHoy >= metas.ejercicios) "Meta cumplida"
                    else "${hoy.completadosHoy} de ${metas.ejercicios} ejercicios",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AZUL,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(segmentos) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(if (i < llenos) VERDE_OK else PISTA, RoundedCornerShape(4.dp))
                    )
                }
            }
            Text(
                "${hoy.minutosHoy.toInt()} de ${metas.minutos} min de práctica",
                fontSize = 12.sp,
                color = TEXTO_SEC,
            )
        }
    }
}

@Composable
private fun Desglose(resumen: ResumenIntento, abierto: Boolean, alternar: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = alternar),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Desglose por articulación", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TEXTO)
                Text(if (abierto) "Ocultar" else "Ver", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AZUL)
            }
            if (abierto) {
                resumen.criterios.forEach { c ->
                    val pct = resumen.pctFuera(c)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.etiqueta, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TEXTO)
                            Text(
                                if (pct == 0) "Estable todo el tiempo" else "Fuera de rango el $pct % del tiempo",
                                fontSize = 12.sp,
                                color = TEXTO_SEC,
                            )
                        }
                        Text(
                            if (c.esDistancia) "${"%.1f".format(c.valor)} anchos" else "${c.valor.toInt()}°",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TEXTO,
                        )
                    }
                }
                Text(
                    "Valores medidos en el instante en que completaste el ejercicio.",
                    fontSize = 11.sp,
                    color = TEXTO_SEC,
                )
            }
        }
    }
}