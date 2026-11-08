package com.example.alinea.core

import kotlin.math.roundToInt

data class DetalleCriterio(
    val etiqueta: String,
    val valor: Double,            // grados, o anchos de hombro si esDistancia
    val esDistancia: Boolean,
    val postura: Postura,         // estado en el instante del éxito
    val fotogramasFuera: Int,     // fotogramas del intento con este criterio fuera de rango
)

data class ResumenIntento(
    val intentos: Int,
    val fotogramasSostenidos: Int,
    val fotogramasCorrectos: Int,
    val criterios: List<DetalleCriterio>,
) {
    val reinicios: Int get() = (intentos - 1).coerceAtLeast(0)

    /** 0..1: parte del tiempo sostenido con TODOS los criterios en verde. */
    val precision: Float
        get() = if (fotogramasSostenidos == 0) 1f else fotogramasCorrectos.toFloat() / fotogramasSostenidos

    /** El criterio que más estuvo fuera de rango, o null si todos se mantuvieron estables. */
    val masExigido: DetalleCriterio?
        get() = criterios.maxByOrNull { it.fotogramasFuera }?.takeIf { it.fotogramasFuera > 0 }

    fun pctFuera(d: DetalleCriterio): Int =
        if (fotogramasSostenidos == 0) 0 else (100f * d.fotogramasFuera / fotogramasSostenidos).roundToInt()
}

/** Recoge los datos del intento que termina en éxito. Los contadores se refieren al último intento. */
class AnalisisIntento(private val reto: Reto) {
    private var intentos = 0
    private var estadoPrevio: EstadoReto? = null
    private var sostenidos = 0
    private var correctos = 0
    private var fuera = IntArray(reto.criterios.size)

    fun reiniciar() {
        intentos = 0
        estadoPrevio = null
        sostenidos = 0
        correctos = 0
        fuera = IntArray(reto.criterios.size)
    }

    /** Una vez por fotograma evaluado (no durante la pausa de la guía). */
    fun alEvaluar(r: Resultado) {
        val sosteniendo = r.estado == EstadoReto.SOSTENIENDO || r.estado == EstadoReto.EXITO
        if (sosteniendo && estadoPrevio != EstadoReto.SOSTENIENDO && estadoPrevio != EstadoReto.EXITO) {
            intentos++               // empieza un intento nuevo: se reinician los contadores
            sostenidos = 0
            correctos = 0
            fuera.fill(0)
        }
        if (sosteniendo) {
            sostenidos++
            if (r.postura == Postura.APROBADO) correctos++
            r.criterios.forEachIndexed { i, c ->
                if (i < fuera.size && c.postura != Postura.APROBADO) fuera[i]++
            }
        }
        estadoPrevio = r.estado
    }

    /** Si se pierde el cuerpo mientras se sostenía, el siguiente intento cuenta como nuevo. */
    fun alPerderCuerpo() {
        estadoPrevio = EstadoReto.ANALIZANDO
    }

    fun resumen(r: Resultado): ResumenIntento = ResumenIntento(
        intentos = maxOf(intentos, 1),
        fotogramasSostenidos = sostenidos,
        fotogramasCorrectos = correctos,
        criterios = r.criterios.mapIndexed { i, c ->
            DetalleCriterio(
                etiqueta = etiquetaCriterio(c.indices, c.esDistancia),
                valor = c.angulo,
                esDistancia = c.esDistancia,
                postura = c.postura,
                fotogramasFuera = fuera.getOrElse(i) { 0 },
            )
        },
    )
}