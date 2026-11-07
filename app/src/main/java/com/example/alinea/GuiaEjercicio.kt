package com.example.alinea

import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.alinea.core.Reto

private val AZUL = Color(0xFF1A73E8)
private val TEXTO = Color(0xFF0B2A4A)

/** Cápsula compacta,  que abre la guía durante el ejercicio. */
@Composable
fun CapsulaGuia(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(forma)
            .background(Color(0xCC0A1A33))
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF7C5CFF), Color(0xFF22D3EE))),
                forma,
            )
            .clickable(
                onClickLabel = "Abrir la guía del ejercicio",
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconoGuia(Modifier.size(18.dp))
        Text("Ver guía", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
/**representa "referencia de la postura". */
@Composable
private fun IconoGuia(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val e = Stroke(width = size.width * 0.11f, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawCircle(Color.White, radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.16f), style = e)
        drawLine(Color.White, Offset(w * 0.5f, h * 0.30f), Offset(w * 0.5f, h * 0.62f), e.width, StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.18f, h * 0.42f), Offset(w * 0.82f, h * 0.42f), e.width, StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.3f, h * 0.95f), e.width, StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.7f, h * 0.95f), e.width, StrokeCap.Round)
    }
}

/**
 * Guía ampliada con imagen e instrucciones. Es un Dialog: se muestra encima de la pantalla sin
 * recrear la cámara, y el botón atrás la cierra.
 */
@Composable
fun GuiaExpandida(reto: Reto, onCerrar: () -> Unit) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 640.dp),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("GUÍA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AZUL)
                Text(reto.nombre, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = TEXTO)
                ImagenPose(
                    nombre = reto.imagen,
                    descripcion = "Postura de ${reto.nombre}",
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
                if (reto.pasos.isNotEmpty()) {
                    reto.pasos.forEachIndexed { i, paso -> Paso(i + 1, paso) }
                } else if (reto.instrucciones.isNotBlank()) {
                    Text(reto.instrucciones, fontSize = 15.sp, color = TEXTO)
                }
                Button(onClick = onCerrar, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Volver al ejercicio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}