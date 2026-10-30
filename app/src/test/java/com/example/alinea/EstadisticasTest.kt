package com.example.alinea

import com.example.alinea.core.*
import org.junit.Assert.assertEquals
import org.junit.Test

class EstadisticasTest {
    private val hoy = 19998L   // un miércoles; la semana va del 19996 (lunes) al 20002

    private fun r(
        dia: Long,
        estado: EstadoEjercicio = EstadoEjercicio.COMPLETADO,
        seg: Double = 30.0,
    ) = ResultadoEjercicio("yoga_montana", dia, 10.0, seg, estado)

    @Test
    fun diaDeSemanaCorrecto() {
        assertEquals(2, diaDeSemana(hoy))
        assertEquals(3, diaDeSemana(0L))
    }

    @Test
    fun diaLocalUsaLaZonaHoraria() {
        assertEquals(0L, diaLocal(0L, 0))
        assertEquals(-1L, diaLocal(0L, -5 * 3_600_000))
    }

    @Test
    fun diasActivosSoloDeEstaSemana() {
        val p = calcularProgreso(listOf(r(19995), r(19996), r(19997), r(19998)), hoy)
        assertEquals(3, p.diasActivosSemana)
    }

    @Test
    fun saltadosEInterrumpidosNoCuentan() {
        val p = calcularProgreso(
            listOf(r(19998, EstadoEjercicio.SALTADO), r(19997, EstadoEjercicio.INTERRUMPIDO)),
            hoy,
        )
        assertEquals(0, p.completadosHoy)
        assertEquals(0, p.diasActivosSemana)
        assertEquals(0, p.racha)
    }

    @Test
    fun rachaCuentaDiasSeguidos() {
        assertEquals(3, calcularProgreso(listOf(r(19996), r(19997), r(19998)), hoy).racha)
    }

    @Test
    fun rachaContinuaDesdeAyerSiHoyNoHayNada() {
        assertEquals(2, calcularProgreso(listOf(r(19996), r(19997)), hoy).racha)
    }

    @Test
    fun huecoRompeLaRacha() {
        assertEquals(1, calcularProgreso(listOf(r(19998), r(19996)), hoy).racha)
    }

    @Test
    fun minutosHoySumaSoloElTiempoSostenidoDeHoy() {
        val p = calcularProgreso(
            listOf(
                r(19998, seg = 90.0),
                r(19998, EstadoEjercicio.INTERRUMPIDO, seg = 30.0),
                r(19997, seg = 600.0),
            ),
            hoy,
        )
        assertEquals(2.0, p.minutosHoy, 0.0001)
    }

    @Test
    fun losAnillosSeLimitanAUno() {
        val seis = List(6) { r(19998) }
        val p = calcularProgreso(seis, hoy)
        assertEquals(1f, p.anilloEjercicios, 0.001f)
        assertEquals(0.6f, p.anilloMinutos, 0.001f)
    }
}