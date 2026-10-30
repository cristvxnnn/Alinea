package com.example.alinea

import com.example.alinea.core.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgresoVistaTest {
    private val hoy = 19998L   // miércoles; la semana va del 19996 (lunes) al 20002 (domingo)

    private fun r(
        dia: Long,
        id: String = "yoga_montana",
        estado: EstadoEjercicio = EstadoEjercicio.COMPLETADO,
        seg: Double = 30.0,
    ) = ResultadoEjercicio(id, dia, 10.0, seg, estado)

    @Test
    fun totalesSumanTodo() {
        val t = calcularTotales(
            listOf(
                r(19996),
                r(19997, seg = 60.0),
                r(19998),
                r(19998, estado = EstadoEjercicio.INTERRUMPIDO),
            ),
            hoy,
        )
        assertEquals(3, t.ejerciciosCompletados)
        assertEquals(3, t.diasEntrenados)
        assertEquals(3, t.rachaActual)
        assertEquals(2.5, t.minutos, 0.0001)
    }

    @Test
    fun laSemanaTieneSieteDiasDesdeElLunes() {
        val s = semanaActual(emptyList(), hoy)
        assertEquals(7, s.size)
        assertEquals(19996L, s.first().dia)
        assertEquals(20002L, s.last().dia)
    }

    @Test
    fun laSemanaCuentaCompletadosPorDia() {
        val s = semanaActual(listOf(r(19996), r(19996), r(19998), r(19995)), hoy)
        assertEquals(2, s[0].completados)
        assertEquals(0, s[1].completados)
        assertEquals(1, s[2].completados)
    }

    @Test
    fun elHistorialAgrupaPorDiaYOrdena() {
        val h = historialPorDia(
            listOf(
                r(19996, "a"),
                r(19998, "b"),
                r(19998, "c", EstadoEjercicio.SALTADO),
                r(19998, "b"),
            ),
        )
        assertEquals(2, h.size)
        assertEquals(19998L, h[0].dia)
        assertEquals(2, h[0].completados)
        assertEquals(1, h[0].saltados)
        assertEquals(listOf("b", "c"), h[0].ejercicios)
    }

    @Test
    fun elHistorialRespetaElLimite() {
        val h = historialPorDia(List(5) { r(19990L + it) }, limite = 3)
        assertEquals(3, h.size)
        assertEquals(19994L, h[0].dia)
    }
}