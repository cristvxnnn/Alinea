package com.example.alinea.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

data class Punto(val x: Float, val y:Float)

fun calcularAngulo(a: Punto, b: Punto, c: Punto): Double{
    val rad= atan2((c.y - b.y).toDouble(), (c.x-b.x).toDouble())-
    atan2((a.y-b.y).toDouble(), (a.x- b.x).toDouble())
    var grados = abs(rad*180.0/PI)
    if (grados>180.0)grados=360.0-grados
    return grados
}