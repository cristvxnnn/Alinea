package com.example.alinea.core

private val NOMBRES = mapOf(
    0 to "nariz",
    7 to "oreja izquierda", 8 to "oreja derecha",
    11 to "hombro izquierdo", 12 to "hombro derecho",
    13 to "codo izquierdo", 14 to "codo derecho",
    15 to "muñeca izquierda", 16 to "muñeca derecha",
    23 to "cadera izquierda", 24 to "cadera derecha",
    25 to "rodilla izquierda", 26 to "rodilla derecha",
    27 to "tobillo izquierdo", 28 to "tobillo derecho",
)

fun nombreLandmark(indice: Int): String = NOMBRES[indice] ?: "punto $indice"

/** Ángulo: se nombra por la articulación central. Distancia: por los dos puntos. */
fun etiquetaCriterio(indices: List<Int>, esDistancia: Boolean): String = when {
    esDistancia && indices.size == 2 ->
        "Distancia: ${nombreLandmark(indices[0])} – ${nombreLandmark(indices[1])}"
    indices.size == 3 -> nombreLandmark(indices[1]).replaceFirstChar { it.uppercase() }
    else -> "Criterio"
}