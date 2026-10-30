package com.example.alinea.core

enum class EstadoEjercicio { COMPLETADO, SALTADO, INTERRUMPIDO }

/** dia: días desde 1970-01-01 en hora LOCAL (ver diaLocal). */
data class ResultadoEjercicio(
    val ejercicioId: String,
    val dia: Long,
    val segundosObjetivo: Double,
    val segundosSostenidos: Double,
    val estado: EstadoEjercicio,
)

data class MetasDiarias(val ejercicios: Int = 4, val minutos: Int = 5) {
    init {
        require(ejercicios > 0 && minutos > 0) { "Las metas deben ser positivas" }
    }
}

data class ResumenProgreso(
    val completadosHoy: Int,
    val minutosHoy: Double,
    val diasActivosSemana: Int,
    val racha: Int,
    val anilloEjercicios: Float,   // 0..1
    val anilloMinutos: Float,      // 0..1
    val anilloDias: Float,         // 0..1
)

data class Totales(
    val ejerciciosCompletados: Int,
    val minutos: Double,
    val diasEntrenados: Int,
    val rachaActual: Int,
)

data class DiaSemana(val dia: Long, val completados: Int, val minutos: Double)

data class ActividadDia(
    val dia: Long,
    val completados: Int,
    val saltados: Int,
    val interrumpidos: Int,
    val minutos: Double,
    val ejercicios: List<String>,   // ids distintos, en el orden en que se hicieron
)

private const val MS_POR_DIA = 86_400_000L

/** Día local a partir de milisegundos y el desfase de la zona horaria (TimeZone.getOffset). */
fun diaLocal(millis: Long, desfaseZonaMs: Int): Long =
    (millis + desfaseZonaMs).floorDiv(MS_POR_DIA)

/** 0 = lunes ... 6 = domingo. El día 0 (1970-01-01) fue jueves. */
fun diaDeSemana(dia: Long): Int = (dia + 3).mod(7L).toInt()

/** Racha: si hoy aún no hay un ejercicio completado, se cuenta desde ayer. */
private fun rachaHasta(diasConCompletado: Set<Long>, hoy: Long): Int {
    var d = if (hoy in diasConCompletado) hoy else hoy - 1
    var racha = 0
    while (d in diasConCompletado) {
        racha++
        d--
    }
    return racha
}

fun calcularProgreso(
    resultados: List<ResultadoEjercicio>,
    hoy: Long,
    metas: MetasDiarias = MetasDiarias(),
): ResumenProgreso {
    val completados = resultados.filter { it.estado == EstadoEjercicio.COMPLETADO }
    val diasConCompletado = completados.map { it.dia }.toSet()

    val completadosHoy = completados.count { it.dia == hoy }
    // Tiempo real sostenido (de cualquier estado), no el tiempo con la cámara abierta
    val minutosHoy = resultados.filter { it.dia == hoy }.sumOf { it.segundosSostenidos } / 60.0

    val lunes = hoy - diaDeSemana(hoy)
    val diasActivos = diasConCompletado.count { it in lunes..(lunes + 6) }

    return ResumenProgreso(
        completadosHoy = completadosHoy,
        minutosHoy = minutosHoy,
        diasActivosSemana = diasActivos,
        racha = rachaHasta(diasConCompletado, hoy),
        anilloEjercicios = (completadosHoy.toFloat() / metas.ejercicios).coerceIn(0f, 1f),
        anilloMinutos = (minutosHoy / metas.minutos).toFloat().coerceIn(0f, 1f),
        anilloDias = (diasActivos / 7f).coerceIn(0f, 1f),
    )
}

fun calcularTotales(resultados: List<ResultadoEjercicio>, hoy: Long): Totales {
    val completados = resultados.filter { it.estado == EstadoEjercicio.COMPLETADO }
    val dias = completados.map { it.dia }.toSet()
    return Totales(
        ejerciciosCompletados = completados.size,
        minutos = resultados.sumOf { it.segundosSostenidos } / 60.0,
        diasEntrenados = dias.size,
        rachaActual = rachaHasta(dias, hoy),
    )
}

/** Los 7 días de la semana en curso, de lunes a domingo. */
fun semanaActual(resultados: List<ResultadoEjercicio>, hoy: Long): List<DiaSemana> {
    val lunes = hoy - diaDeSemana(hoy)
    return (0L..6L).map { i ->
        val dia = lunes + i
        val delDia = resultados.filter { it.dia == dia }
        DiaSemana(
            dia = dia,
            completados = delDia.count { it.estado == EstadoEjercicio.COMPLETADO },
            minutos = delDia.sumOf { it.segundosSostenidos } / 60.0,
        )
    }
}

/** Actividad agrupada por día, del más reciente al más antiguo. */
fun historialPorDia(resultados: List<ResultadoEjercicio>, limite: Int = 30): List<ActividadDia> =
    resultados.groupBy { it.dia }
        .map { (dia, lista) ->
            ActividadDia(
                dia = dia,
                completados = lista.count { it.estado == EstadoEjercicio.COMPLETADO },
                saltados = lista.count { it.estado == EstadoEjercicio.SALTADO },
                interrumpidos = lista.count { it.estado == EstadoEjercicio.INTERRUMPIDO },
                minutos = lista.sumOf { it.segundosSostenidos } / 60.0,
                ejercicios = lista.map { it.ejercicioId }.distinct(),
            )
        }
        .sortedByDescending { it.dia }
        .take(limite)