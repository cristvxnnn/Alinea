package com.example.alinea.core

private val GRUPOS = linkedMapOf(
    "cabeza" to listOf(0),
    "hombros" to listOf(11, 12),
    "caderas" to listOf(23, 24),
    "rodillas" to listOf(25, 26),
    "pies" to listOf(27, 28, 31, 32),
)

data class EstadoCalibracion(val completa: Boolean, val faltantes: List<String>)

fun evaluarSilueta(
    puntos: List<Punto>,
    visibilidad: List<Float>,
    minVisibilidad: Float = 0.5f,
    margen: Float = 0.02f,
): EstadoCalibracion {
    require(puntos.size >= 33 && visibilidad.size >= 33) { "Se esperan 33 landmarks" }
    val faltantes = GRUPOS.filter { (_, indices) ->
        indices.any { i ->
            val p = puntos[i]
            visibilidad[i] < minVisibilidad ||
                    p.x < margen || p.x > 1f - margen ||
                    p.y < margen || p.y > 1f - margen
        }
    }.keys.toList()
    return EstadoCalibracion(faltantes.isEmpty(), faltantes)
}

/** Exige que la silueta completa se mantenga estable un tiempo antes de dar el visto bueno. */
class Calibrador(private val segundosEstables: Double = 1.0) {
    private var desde = -1.0
    var listo = false
        private set

    fun reiniciar() {
        desde = -1.0
        listo = false
    }

    /** Devuelve el progreso 0..1; `listo` pasa a true al llegar a 1. */
    fun actualizar(completa: Boolean, ahora: Double): Float {
        if (listo) return 1f
        if (!completa) {
            desde = -1.0
            return 0f
        }
        if (desde < 0) desde = ahora
        val progreso = ((ahora - desde) / segundosEstables).coerceIn(0.0, 1.0).toFloat()
        if (progreso >= 1f) listo = true
        return progreso
    }
}