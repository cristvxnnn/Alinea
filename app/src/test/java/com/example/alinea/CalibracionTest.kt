package com.example.alinea

import com.example.alinea.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibracionTest {

    private fun cuerpo(): Pair<MutableList<Punto>, MutableList<Float>> =
        MutableList(33) { Punto(0.5f, 0.5f) } to MutableList(33) { 0.99f }

    @Test
    fun siluetaCompletaCuandoTodoSeVe() {
        val (p, v) = cuerpo()
        assertTrue(evaluarSilueta(p, v).completa)
    }

    @Test
    fun faltanLosPiesSiSonPocoVisibles() {
        val (p, v) = cuerpo()
        v[27] = 0.1f
        val e = evaluarSilueta(p, v)
        assertFalse(e.completa)
        assertTrue(e.faltantes.contains("pies"))
    }

    @Test
    fun faltanLosPiesSiSalenDelEncuadre() {
        val (p, v) = cuerpo()
        p[31] = Punto(0.5f, 1.05f)
        val e = evaluarSilueta(p, v)
        assertFalse(e.completa)
        assertTrue(e.faltantes.contains("pies"))
    }

    @Test
    fun calibradorExigeEstabilidad() {
        val c = Calibrador(segundosEstables = 1.0)
        assertEquals(0f, c.actualizar(true, 0.0), 0.001f)
        assertEquals(0.5f, c.actualizar(true, 0.5), 0.001f)
        assertFalse(c.listo)
        assertEquals(1f, c.actualizar(true, 1.0), 0.001f)
        assertTrue(c.listo)
    }

    @Test
    fun calibradorSeReiniciaSiLaSilutaParpadea() {
        val c = Calibrador(segundosEstables = 1.0)
        c.actualizar(true, 0.0)
        c.actualizar(false, 0.5)
        assertEquals(0f, c.actualizar(true, 0.6), 0.001f)
        assertFalse(c.listo)
        c.actualizar(true, 1.7)
        assertTrue(c.listo)
    }
}