package com.example.alinea.core

private val GRUPOS_COMPLETA = linkedMapOf(
    "cabeza" to listOf(0),
    "hombros" to listOf(11, 12),
    "caderas" to listOf(23, 24),
    "rodillas" to listOf(25, 26),
    "pies" to listOf(27, 28, 31, 32),
)

// De lado solo se exige el lado izquierdo (el más cercano a la cámara)
private val GRUPOS_LATERAL = linkedMapOf(
    "cabeza" to listOf(0),
    "hombro" to listOf(11),
    "cadera" to listOf(23),
    "rodilla" to listOf(25),
    "pie" to listOf(27, 31),
)

private val GRUPOS_BUSTO = linkedMapOf(
    "cabeza" to listOf(0),
    "orejas" to listOf(7, 8),
    "hombros" to listOf(11, 12),
)

private fun gruposDe(vista: VistaCalibracion) = when (vista) {
    VistaCalibracion.COMPLETA -> GRUPOS_COMPLETA
    VistaCalibracion.LATERAL -> GRUPOS_LATERAL
    VistaCalibracion.BUSTO -> GRUPOS_BUSTO
}
/** Nombres de las partes del cuerpo que exige cada vista, para la lista de verificación. */
fun gruposRequeridos(vista: VistaCalibracion): List<String> = gruposDe(vista).keys.toList()

data class EstadoCalibracion(val completa: Boolean, val faltantes: List<String>)

/**
 * puntos: landmarks NORMALIZADOS (0..1). visibilidad: probabilidad de cada landmark (0..1).
 * Un grupo "falta" si algún landmark es poco visible o está fuera del encuadre.
 */
fun evaluarSilueta(
    puntos: List<Punto>,
    visibilidad: List<Float>,
    minVisibilidad: Float = 0.5f,
    margen: Float = 0.02f,
    vista: VistaCalibracion = VistaCalibracion.COMPLETA,
): EstadoCalibracion {
    require(puntos.size >= 33 && visibilidad.size >= 33) { "Se esperan 33 landmarks" }
    val faltantes = gruposDe(vista).filter { (_, indices) ->
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