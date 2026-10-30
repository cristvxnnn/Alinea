package com.example.alinea.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6FB),
    onPrimaryContainer = Color(0xFF0B2A4A),
    secondary = Color(0xFF4FC3F7),
    secondaryContainer = Color(0xFFD6E6FB),
    onSecondaryContainer = Color(0xFF0B2A4A),
    tertiary = Color(0xFF7C9DFF),
    background = Color(0xFFF3F8FD),
    onBackground = Color(0xFF0B2A4A),
    surface = Color(0xFFF3F8FD),
    onSurface = Color(0xFF0B2A4A),
    surfaceVariant = Color(0xFFE1EDFA),
    onSurfaceVariant = Color(0xFF4A607A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F8FD),
    surfaceContainer = Color(0xFFE8F1FB),
    surfaceContainerHigh = Color(0xFFE8F1FB),
    surfaceContainerHighest = Color.White,
    error = Color(0xFFD92D20),
)

@Composable
fun AlineaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        content = content,
    )
}