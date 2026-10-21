package com.example.alinea.core

enum class Postura { APROBADO, CORRIGIENDO, INCORRECTO }
enum class EstadoReto { ANALIZANDO, SOSTENIENDO, EXITO }
enum class Sonido { TICK, EXITO, ERROR, LISTO }
data class ResultadoCriterio(val angulo: Double, val postura: Postura)

data class Resultado(
    val criterios: List<ResultadoCriterio>,
    val postura: Postura,
    val estado: EstadoReto,
    val progreso: Float,
    val sonido: Sonido?,
)

class EvaluadorReto(
    private val reto: Reto,
    private val tolerancia: Double = 20.0,
) {
    var estado = EstadoReto.ANALIZANDO
        private set
    private var inicio = 0.0
    private var ultimoTick = 0

    init {
        require(reto.criterios.isNotEmpty()) { "El reto necesita al menos un criterio" }
    }

    fun reiniciar() {
        estado = EstadoReto.ANALIZANDO
        inicio = 0.0
        ultimoTick = 0
    }

    private fun clasificar(angulo: Double, c: Criterio) = when {
        angulo in c.rangoMin..c.rangoMax -> Postura.APROBADO
        angulo in (c.rangoMin - tolerancia)..c.rangoMin ||
                angulo in c.rangoMax..(c.rangoMax + tolerancia) -> Postura.CORRIGIENDO
        else -> Postura.INCORRECTO
    }

    /** puntos: los 33 landmarks ya escalados a píxeles. ahora: segundos. */
    fun evaluar(puntos: List<Punto>, ahora: Double): Resultado {
        require(puntos.size >= 33) { "Se esperan 33 landmarks" }

        val porCriterio = reto.criterios.map { c ->
            val angulo = calcularAngulo(
                puntos[c.indices[0]], puntos[c.indices[1]], puntos[c.indices[2]]
            )
            ResultadoCriterio(angulo, clasificar(angulo, c))
        }
        val postura = when {
            porCriterio.all { it.postura == Postura.APROBADO } -> Postura.APROBADO
            porCriterio.any { it.postura == Postura.INCORRECTO } -> Postura.INCORRECTO
            else -> Postura.CORRIGIENDO
        }

        if (estado == EstadoReto.EXITO) {
            return Resultado(porCriterio, postura, estado, 1f, null)
        }

        var progreso = 0f
        var sonido: Sonido? = null

        if (postura == Postura.APROBADO) {
            if (estado == EstadoReto.ANALIZANDO) {
                estado = EstadoReto.SOSTENIENDO
                inicio = ahora
                ultimoTick = 0
            }
            val transcurrido = ahora - inicio
            progreso = (transcurrido / reto.tiempoObjetivo).coerceAtMost(1.0).toFloat()
            if (transcurrido >= reto.tiempoObjetivo) {
                estado = EstadoReto.EXITO
                sonido = Sonido.EXITO
            } else if (transcurrido.toInt() > ultimoTick) {
                ultimoTick = transcurrido.toInt()
                sonido = Sonido.TICK
            }
        } else if (estado == EstadoReto.SOSTENIENDO) {
            estado = EstadoReto.ANALIZANDO
            sonido = Sonido.ERROR
        }
        return Resultado(porCriterio, postura, estado, progreso, sonido)
    }

    fun perderCuerpo(): Sonido? {
        if (estado != EstadoReto.SOSTENIENDO) return null
        estado = EstadoReto.ANALIZANDO
        return Sonido.ERROR
    }
}