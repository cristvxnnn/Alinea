package com.example.alinea.core

data class Criterio(
    val indices: List<Int>,
    val rangoMin: Double,
    val rangoMax: Double,
)
data class Reto(
    val id: String,
    val nombre: String,
    val criterios: List<Criterio>,
    val tiempoObjetivo: Double,
)
val INDICE_LANDMARK = mapOf(
    "NOSE" to 0,
    "LEFT_EYE_INNER" to 1, "LEFT_EYE" to 2, "LEFT_EYE_OUTER" to 3,
    "RIGHT_EYE_INNER" to 4, "RIGHT_EYE" to 5, "RIGHT_EYE_OUTER" to 6,
    "LEFT_EAR" to 7, "RIGHT_EAR" to 8,
    "MOUTH_LEFT" to 9, "MOUTH_RIGHT" to 10,
    "LEFT_SHOULDER" to 11, "RIGHT_SHOULDER" to 12,
    "LEFT_ELBOW" to 13, "RIGHT_ELBOW" to 14,
    "LEFT_WRIST" to 15, "RIGHT_WRIST" to 16,
    "LEFT_PINKY" to 17, "RIGHT_PINKY" to 18,
    "LEFT_INDEX" to 19, "RIGHT_INDEX" to 20,
    "LEFT_THUMB" to 21, "RIGHT_THUMB" to 22,
    "LEFT_HIP" to 23, "RIGHT_HIP" to 24,
    "LEFT_KNEE" to 25, "RIGHT_KNEE" to 26,
    "LEFT_ANKLE" to 27, "RIGHT_ANKLE" to 28,
    "LEFT_HEEL" to 29, "RIGHT_HEEL" to 30,
    "LEFT_FOOT_INDEX" to 31, "RIGHT_FOOT_INDEX" to 32,
)