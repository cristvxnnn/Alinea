package com.example.alinea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alinea.core.ActividadDia
import com.example.alinea.core.DiaSemana
import com.example.alinea.core.ResultadoEjercicio
import com.example.alinea.core.calcularTotales
import com.example.alinea.core.historialPorDia
import com.example.alinea.core.semanaActual
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AZUL = Color(0xFF1A73E8)
private val PISTA = Color(0xFFE1EDFA)
private val TEXTO = Color(0xFF0B2A4A)
private val TEXTO_SEC = Color(0xFF4A607A)
private val LETRAS = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
fun PantallaProgreso(
    resultados: List<ResultadoEjercicio>,
    nombres: Map<String, String>,
    hoy: Long,
) {
    val totales = remember(resultados, hoy) { calcularTotales(resultados, hoy) }
    val semana = remember(resultados, hoy) { semanaActual(resultados, hoy) }
    val historial = remember(resultados) { historialPorDia(resultados, 14) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Progreso", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TEXTO)

        Tarjeta {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Dato(totales.ejerciciosCompletados.toString(), "ejercicios")
                Dato(valorMinutos(totales.minutos), "minutos")
                Dato(totales.diasEntrenados.toString(), "días entrenados")
            }
            Text(
                if (totales.rachaActual == 1) "Racha actual: 1 día" else "Racha actual: ${totales.rachaActual} días",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TEXTO,
            )
        }

        Tarjeta {
            Text("Esta semana", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TEXTO)
            GraficaSemana(semana, hoy)
            Text("Ejercicios completados por día", fontSize = 12.sp, color = TEXTO_SEC)
        }

        Text("Actividad reciente", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TEXTO)

        if (historial.isEmpty()) {
            Tarjeta {
                Text(
                    "Aún no hay actividad. Completa tu primer ejercicio y aparecerá aquí.",
                    fontSize = 14.sp,
                    color = TEXTO_SEC,
                )
            }
        } else {
            historial.forEach { actividad ->
                Tarjeta { FilaActividad(actividad, nombres, hoy) }
            }
        }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = contenido)
    }
}

@Composable
private fun Dato(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AZUL)
        Text(etiqueta, fontSize = 12.sp, color = TEXTO_SEC)
    }
}

@Composable
private fun GraficaSemana(semana: List<DiaSemana>, hoy: Long) {
    val maximo = maxOf(1, semana.maxOf { it.completados })
    Row(
        modifier = Modifier.fillMaxWidth().height(130.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        semana.forEachIndexed { i, d ->
            val futuro = d.dia > hoy
            val alto = if (d.completados == 0) 4.dp else (12f + 70f * d.completados / maximo).dp
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    if (d.completados > 0) d.completados.toString() else "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TEXTO,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(22.dp)
                        .height(alto)
                        .background(
                            when {
                                d.completados > 0 -> AZUL
                                futuro -> Color(0xFFF0F5FA)
                                else -> PISTA
                            },
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    LETRAS[i],
                    fontSize = 12.sp,
                    fontWeight = if (d.dia == hoy) FontWeight.Bold else FontWeight.Normal,
                    color = if (d.dia == hoy) AZUL else TEXTO_SEC,
                )
            }
        }
    }
}

@Composable
private fun FilaActividad(a: ActividadDia, nombres: Map<String, String>, hoy: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiquetaDia(a.dia, hoy), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TEXTO)
        Text(
            if (a.completados == 1) "1 completado" else "${a.completados} completados",
            fontSize = 14.sp,
            color = AZUL,
            fontWeight = FontWeight.SemiBold,
        )
    }
    val detalle = buildList {
        add(textoMinutos(a.minutos))
        if (a.saltados > 0) add("${a.saltados} saltado(s)")
        if (a.interrumpidos > 0) add("${a.interrumpidos} interrumpido(s)")
    }.joinToString(" · ")
    Text(detalle, fontSize = 13.sp, color = TEXTO_SEC)

    val lista = a.ejercicios.map { nombres[it] ?: it }
    val texto = if (lista.size > 3) lista.take(3).joinToString(", ") + "…" else lista.joinToString(", ")
    Text(texto, fontSize = 13.sp, color = TEXTO)
}

private fun etiquetaDia(dia: Long, hoy: Long): String = when (dia) {
    hoy -> "Hoy"
    hoy - 1 -> "Ayer"
    else -> LocalDate.ofEpochDay(dia)
        .format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es")))
        .replaceFirstChar { it.uppercase() }
}

private fun valorMinutos(min: Double): String = if (min > 0 && min < 1) "<1" else min.toInt().toString()

private fun textoMinutos(min: Double): String = "${valorMinutos(min)} min"