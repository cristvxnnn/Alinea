package com.example.alinea

import com.example.alinea.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EvaluadorGraciaTest {
    private val reto = Reto("g", "G", listOf(Criterio(listOf(11, 13, 15), 160.0, 180.0)), 3.0)

    private fun cuerpo(recto: Boolean): List<Punto> {
        val p = MutableList(33) { Punto(0f, 0f) }
        p[11] = Punto(0f, 0f)
        p[13] = Punto(1f, 0f)
        p[15] = if (recto) Punto(2f, 0f) else Punto(1f, 1f)
        return p
    }

    @Test
    fun unaInterrupcionCortaNoReinicia() {
        val ev = EvaluadorReto(reto, graciaSegundos = 0.6)
        ev.evaluar(cuerpo(true), 0.0)
        val r = ev.evaluar(cuerpo(false), 1.0)
        assertEquals(EstadoReto.SOSTENIENDO, r.estado)
        assertEquals(Postura.INCORRECTO, r.postura)
        assertNull(r.sonido)
        ev.evaluar(cuerpo(true), 1.3)
        assertEquals(EstadoReto.EXITO, ev.evaluar(cuerpo(true), 3.1).estado)
    }

    @Test
    fun unaInterrupcionLargaReinicia() {
        val ev = EvaluadorReto(reto, graciaSegundos = 0.6)
        ev.evaluar(cuerpo(true), 0.0)
        ev.evaluar(cuerpo(false), 1.0)
        val r = ev.evaluar(cuerpo(false), 1.7)
        assertEquals(EstadoReto.ANALIZANDO, r.estado)
        assertEquals(Sonido.ERROR, r.sonido)
    }

    @Test
    fun laGraciaSeReiniciaAlVolverALaPostura() {
        val ev = EvaluadorReto(reto, graciaSegundos = 0.6)
        ev.evaluar(cuerpo(true), 0.0)
        ev.evaluar(cuerpo(false), 1.0)
        ev.evaluar(cuerpo(true), 1.3)
        ev.evaluar(cuerpo(false), 2.0)   // nueva interrupción: cuenta desde 2.0
        assertEquals(EstadoReto.SOSTENIENDO, ev.evaluar(cuerpo(false), 2.5).estado)
        assertEquals(EstadoReto.ANALIZANDO, ev.evaluar(cuerpo(false), 2.7).estado)
    }

    @Test
    fun sinGraciaReiniciaAlInstante() {
        val ev = EvaluadorReto(reto)
        ev.evaluar(cuerpo(true), 0.0)
        assertEquals(EstadoReto.ANALIZANDO, ev.evaluar(cuerpo(false), 1.0).estado)
    }

    @Test
    fun elTiempoEnPausaNoCuentaParaLaGracia() {
        val ev = EvaluadorReto(reto, graciaSegundos = 0.6)
        ev.evaluar(cuerpo(true), 0.0)
        ev.evaluar(cuerpo(false), 1.0)   // empieza la gracia
        ev.pausar(1.1)
        ev.reanudar(21.1)
        assertEquals(EstadoReto.SOSTENIENDO, ev.evaluar(cuerpo(false), 21.2).estado)
    }
}