package com.example.alinea.core

/** Decide cuándo guardar: un éxito se guarda UNA sola vez, aunque se pierda y se recupere el cuerpo. */
class SeguimientoIntento {
    var mejorSostenido = 0.0
        private set
    var hayExito = false
        private set
    private var exitoGuardado = false

    /** @return true si ahora hay que guardar un COMPLETADO (una sola vez por éxito). */
    fun alEvaluar(estado: EstadoReto, segundosSostenidos: Double): Boolean {
        if (estado != EstadoReto.EXITO) {
            mejorSostenido = maxOf(mejorSostenido, segundosSostenidos)
            return false
        }
        if (exitoGuardado) return false
        exitoGuardado = true
        hayExito = true
        mejorSostenido = 0.0
        return true
    }

    /** Al pulsar Repetir. */
    fun reiniciar() {
        exitoGuardado = false
        mejorSostenido = 0.0
    }

    /** Al salir: segundos a guardar como INTERRUMPIDO, o null si no llegó a sostener 1 s. */
    fun segundosInterrumpidos(): Double? = if (mejorSostenido >= 1.0) mejorSostenido else null
}