package com.example.alinea.core

enum class Postura { APROBADO, CORRIGIENDO, INCORRECTO }
enum class EstadoReto { ANALIZANDO, SOSTENIENDO, EXITO }
enum class Sonido { TICK, EXITO, ERROR, LISTO }

data class ResultadoCriterio(
    val angulo: Double,
    val postura: Postura,
    val indices: List<Int> = emptyList(),   // los 3 landmarks del criterio (el 2.º es el vértice)
)

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
    /** Segundos que se tolera una postura no aprobada mientras se sostiene, antes de reiniciar. */
    private val graciaSegundos: Double = 0.0,
) {
    var estado = EstadoReto.ANALIZANDO
        private set
    private var inicio = 0.0
    private var ultimoTick = 0
    private var pausadoDesde: Double? = null
    private var sinAprobarDesde: Double? = null
    val pausado: Boolean get() = pausadoDesde != null

    init {
        require(reto.criterios.isNotEmpty()) { "El reto necesita al menos un criterio" }
    }

    fun reiniciar() {
        estado = EstadoReto.ANALIZANDO
        inicio = 0.0
        ultimoTick = 0
        pausadoDesde = null
        sinAprobarDesde = null
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
            ResultadoCriterio(angulo, clasificar(angulo, c), c.indices)
        }
        val postura = when {
            porCriterio.all { it.postura == Postura.APROBADO } -> Postura.APROBADO
            porCriterio.any { it.postura == Postura.INCORRECTO } -> Postura.INCORRECTO
            else -> Postura.CORRIGIENDO
        }

        if (estado == EstadoReto.EXITO) {
            return Resultado(porCriterio, postura, estado, 1f, null)
        }

        val desdePausa = pausadoDesde
        if (desdePausa != null) {
            // En pausa: se informa el estado actual sin cambiar nada (sin sonidos ni transiciones)
            val congelado = if (estado == EstadoReto.SOSTENIENDO)
                ((desdePausa - inicio) / reto.tiempoObjetivo).coerceIn(0.0, 1.0).toFloat()
            else 0f
            return Resultado(porCriterio, postura, estado, congelado, null)
        }

        var progreso = 0f
        var sonido: Sonido? = null

        if (postura == Postura.APROBADO) {
            sinAprobarDesde = null
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
            val desde = sinAprobarDesde ?: ahora.also { sinAprobarDesde = it }
            if (ahora - desde >= graciaSegundos) {
                estado = EstadoReto.ANALIZANDO
                sonido = Sonido.ERROR
                sinAprobarDesde = null
            } else {
                // Dentro del margen de gracia: el intento sigue (evita reinicios por un fotograma ruidoso)
                progreso = ((ahora - inicio) / reto.tiempoObjetivo).coerceIn(0.0, 1.0).toFloat()
            }
        }
        return Resultado(porCriterio, postura, estado, progreso, sonido)
    }

    /** Congela el reloj del intento (por ejemplo, mientras el usuario consulta la guía). */
    fun pausar(ahora: Double) {
        if (pausadoDesde == null) pausadoDesde = ahora
    }

    /** Reanuda sin perder progreso: el tiempo en pausa no cuenta como tiempo sostenido. */
    fun reanudar(ahora: Double) {
        val desde = pausadoDesde ?: return
        pausadoDesde = null
        sinAprobarDesde = null
        if (estado == EstadoReto.SOSTENIENDO) inicio += ahora - desde
    }

    fun perderCuerpo(): Sonido? {
        if (pausadoDesde != null) return null
        if (estado != EstadoReto.SOSTENIENDO) return null
        estado = EstadoReto.ANALIZANDO
        sinAprobarDesde = null
        return Sonido.ERROR
    }
}