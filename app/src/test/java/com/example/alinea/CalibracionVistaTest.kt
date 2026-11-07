package com.example.alinea

import com.example.alinea.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibracionVistaTest {
    private fun cuerpo() = MutableList(33) { Punto(0.5f, 0.5f) } to MutableList(33) { 0.99f }

    @Test
    fun laVistaLateralIgnoraElLadoDerecho() {
        val (p, v) = cuerpo()
        listOf(12, 24, 26, 28, 32).forEach { v[it] = 0.1f }
        assertFalse(evaluarSilueta(p, v).completa)
        assertTrue(evaluarSilueta(p, v, vista = VistaCalibracion.LATERAL).completa)
    }

    @Test
    fun laVistaLateralExigeElLadoIzquierdo() {
        val (p, v) = cuerpo()
        v[27] = 0.1f
        val e = evaluarSilueta(p, v, vista = VistaCalibracion.LATERAL)
        assertFalse(e.completa)
        assertTrue(e.faltantes.contains("pie"))
    }

    @Test
    fun laVistaBustoSoloExigeCabezaOrejasYHombros() {
        val (p, v) = cuerpo()
        (23..32).forEach { v[it] = 0.1f }
        assertFalse(evaluarSilueta(p, v).completa)
        assertTrue(evaluarSilueta(p, v, vista = VistaCalibracion.BUSTO).completa)
    }

    @Test
    fun laVistaBustoExigeLasOrejas() {
        val (p, v) = cuerpo()
        v[7] = 0.1f
        val e = evaluarSilueta(p, v, vista = VistaCalibracion.BUSTO)
        assertFalse(e.completa)
        assertTrue(e.faltantes.contains("orejas"))
    }

    @Test
    fun desdeTextoInterpretaElJson() {
        assertEquals(VistaCalibracion.LATERAL, VistaCalibracion.desdeTexto("lateral"))
        assertEquals(VistaCalibracion.BUSTO, VistaCalibracion.desdeTexto(" Busto "))
        assertEquals(VistaCalibracion.COMPLETA, VistaCalibracion.desdeTexto(null))
        assertEquals(VistaCalibracion.COMPLETA, VistaCalibracion.desdeTexto("xyz"))
    }
}