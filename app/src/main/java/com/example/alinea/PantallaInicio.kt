package com.example.alinea

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alinea.core.MetasDiarias
import com.example.alinea.core.ResumenProgreso

private val AZUL = Color(0xFF1A73E8)
private val CELESTE = Color(0xFF4FC3F7)
private val PERVINCA = Color(0xFF7C9DFF)
private val TEXTO = Color(0xFF0B2A4A)
private val TEXTO_SEC = Color(0xFF4A607A)

@Composable
fun PantallaInicio(
    resumen: ResumenProgreso,
    metas: MetasDiarias,
    onEntrenar: () -> Unit,
    onEjercicios: () -> Unit,
    onRutinas: () -> Unit,
    onProgreso: () -> Unit,
    onAjustes: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Alinea", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TEXTO)
        }

        Column {
            Text("TU ESPACIO DE BIENESTAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AZUL)
            Text(
                "Muévete a tu ritmo.",
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TEXTO,
            )
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Anillos(
                    resumen.anilloEjercicios,
                    resumen.anilloMinutos,
                    resumen.anilloDias,
                    modifier = Modifier.size(104.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("HOY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AZUL)
                    Leyenda(
                        AZUL,
                        if (resumen.completadosHoy >= metas.ejercicios)
                            "Meta cumplida · ${resumen.completadosHoy} ejercicios"
                        else "${resumen.completadosHoy} de ${metas.ejercicios} ejercicios",
                    )
                    Leyenda(
                        CELESTE,
                        if (resumen.minutosHoy >= metas.minutos)
                            "Meta cumplida · ${resumen.minutosHoy.toInt()} min"
                        else "${resumen.minutosHoy.toInt()} de ${metas.minutos} min",
                    )
                    Leyenda(PERVINCA, "${resumen.diasActivosSemana} de 7 días")
                    Text(
                        if (resumen.racha == 1) "Racha: 1 día" else "Racha: ${resumen.racha} días",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TEXTO,
                    )
                }
            }
        }

        Button(
            onClick = onEntrenar,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AZUL, contentColor = Color.White),
        ) {
            Text("Comenzar entrenamiento", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Text("Explora Alinea", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TEXTO_SEC)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Ejercicios", "Explora las posturas", Color(0xFFD6E6FB), onEjercicios, Modifier.weight(1f))
            Tile("Rutinas", "Organiza tu práctica", Color(0xFFCDEFFC), onRutinas, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Progreso", "Consulta tus sesiones", Color(0xFFDCE3FF), onProgreso, Modifier.weight(1f))
            Tile("Ajustes", "Voz, sonido y permisos", Color(0xFFE4E9F0), onAjustes, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TEXTO)
    }
}

@Composable
private fun Tile(
    titulo: String,
    detalle: String,
    colorIcono: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).background(colorIcono, RoundedCornerShape(10.dp)))
            Column {
                Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TEXTO)
                Text(detalle, fontSize = 11.sp, color = TEXTO_SEC)
            }
        }
    }
}

/** Tres anillos: ejercicios (exterior), minutos y días activos (interior). */
@Composable
private fun Anillos(a: Float, b: Float, c: Float, modifier: Modifier = Modifier) {
    val pista = Color(0xFFE1EDFA)
    Canvas(modifier) {
        val grosor = 9.dp.toPx()
        val separacion = 3.dp.toPx()

        fun anillo(radio: Float, color: Color, progreso: Float) {
            val arriba = Offset(center.x - radio, center.y - radio)
            val tamano = Size(radio * 2, radio * 2)
            drawArc(pista, 0f, 360f, false, arriba, tamano, style = Stroke(grosor))
            if (progreso > 0f) {
                drawArc(
                    color, -90f, 360f * progreso, false, arriba, tamano,
                    style = Stroke(grosor, cap = StrokeCap.Round),
                )
            }
        }

        val r1 = size.minDimension / 2 - grosor / 2
        val r2 = r1 - grosor - separacion
        val r3 = r2 - grosor - separacion
        anillo(r1, AZUL, a)
        anillo(r2, CELESTE, b)
        anillo(r3, PERVINCA, c)
    }
}

@Composable
fun PantallaProximamente(titulo: String) {
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TEXTO)
            Text("Próximamente", fontSize = 15.sp, color = TEXTO_SEC)
        }
    }
}