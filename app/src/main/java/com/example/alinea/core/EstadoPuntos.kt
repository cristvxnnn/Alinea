package com.example.alinea.core

/**
 * Estado de cada articulación evaluada. Ángulo: se pinta el vértice (el punto central).
 * Distancia: se pintan los dos puntos.
 */
fun estadoPorPunto(criterios: List<ResultadoCriterio>): Map<Int, Postura> {
    val mapa = mutableMapOf<Int, Postura>()
    for (c in criterios) {
        val puntos = when (c.indices.size) {
            3 -> listOf(c.indices[1])
            2 -> c.indices
            else -> emptyList()
        }
        for (p in puntos) mapa[p] = peor(mapa[p], c.postura)
    }
    return mapa
}

/** Estado de cada hueso (par de puntos) que forma parte de un criterio. Clave: (menor, mayor). */
fun estadoPorHueso(criterios: List<ResultadoCriterio>): Map<Pair<Int, Int>, Postura> {
    val mapa = mutableMapOf<Pair<Int, Int>, Postura>()
    for (c in criterios) {
        val pares = when (c.indices.size) {
            3 -> listOf(c.indices[0] to c.indices[1], c.indices[1] to c.indices[2])
            2 -> listOf(c.indices[0] to c.indices[1])
            else -> emptyList()
        }
        for ((a, b) in pares) {
            val clave = minOf(a, b) to maxOf(a, b)
            mapa[clave] = peor(mapa[clave], c.postura)
        }
    }
    return mapa
}

private fun peor(a: Postura?, b: Postura): Postura = when {
    a == null -> b
    a == Postura.INCORRECTO || b == Postura.INCORRECTO -> Postura.INCORRECTO
    a == Postura.CORRIGIENDO || b == Postura.CORRIGIENDO -> Postura.CORRIGIENDO
    else -> Postura.APROBADO
}