package com.example.alinea

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ImagenPose(nombre: String?, descripcion: String, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val recurso = remember(nombre) {
        if (nombre.isNullOrBlank()) 0
        else contexto.resources.getIdentifier(nombre, "drawable", contexto.packageName)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE1EDFA)),
        contentAlignment = Alignment.Center,
    ) {
        if (recurso != 0) {
            Image(
                painter = painterResource(recurso),
                contentDescription = descripcion,
                contentScale = ContentScale.Fit,   // nunca se deforma
                modifier = Modifier.fillMaxSize().padding(10.dp),
            )
        } else {
            Text("Sin imagen de referencia", fontSize = 12.sp, color = Color(0xFF4A607A))
        }
    }
}