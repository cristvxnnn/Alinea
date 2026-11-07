package com.example.alinea.core

/** Estado de cada articulación evaluada: el vértice de cada criterio (el punto cuyo ángulo se mide). */
fun estadoPorPunto(criterios: List<ResultadoCriterio>): Map<Int, Postura> {
    val mapa = mutableMapOf<Int, Postura>()
    for (c in criterios) {
        if (c.indices.size < 3) continue
        val vertice = c.indices[1]
        mapa[vertice] = peor(mapa[vertice], c.postura)
    }
    return mapa
}

/** Estado de cada hueso (par de puntos) que forma parte de un criterio. Clave: (menor, mayor). */
fun estadoPorHueso(criterios: List<ResultadoCriterio>): Map<Pair<Int, Int>, Postura> {
    val mapa = mutableMapOf<Pair<Int, Int>, Postura>()
    for (c in criterios) {
        if (c.indices.size < 3) continue
        for ((a, b) in listOf(c.indices[0] to c.indices[1], c.indices[1] to c.indices[2])) {
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