package com.example.alinea

import com.example.alinea.core.EstadoReto
import com.example.alinea.core.SeguimientoIntento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeguimientoIntentoTest {

    @Test
    fun guardaElExitoUnaSolaVez() {
        val s = SeguimientoIntento()
        assertFalse(s.alEvaluar(EstadoReto.SOSTENIENDO, 1.0))
        assertTrue(s.alEvaluar(EstadoReto.EXITO, 3.0))
        // Aunque se pierda el cuerpo y vuelva a aparecer, sigue siendo el mismo éxito
        assertFalse(s.alEvaluar(EstadoReto.EXITO, 3.0))
        assertFalse(s.alEvaluar(EstadoReto.EXITO, 3.0))
    }

    @Test
    fun repetirPermiteGuardarOtroExito() {
        val s = SeguimientoIntento()
        assertTrue(s.alEvaluar(EstadoReto.EXITO, 3.0))
        s.reiniciar()
        assertFalse(s.alEvaluar(EstadoReto.SOSTENIENDO, 1.0))
        assertTrue(s.alEvaluar(EstadoReto.EXITO, 3.0))
    }

    @Test
    fun interrumpidoSoloSiSostuvoAlMenosUnSegundo() {
        val a = SeguimientoIntento()
        a.alEvaluar(EstadoReto.SOSTENIENDO, 0.5)
        assertNull(a.segundosInterrumpidos())

        val b = SeguimientoIntento()
        b.alEvaluar(EstadoReto.SOSTENIENDO, 2.0)
        assertEquals(2.0, b.segundosInterrumpidos()!!, 0.0001)
    }

    @Test
    fun trasUnExitoNoHayInterrumpido() {
        val s = SeguimientoIntento()
        s.alEvaluar(EstadoReto.SOSTENIENDO, 2.0)
        s.alEvaluar(EstadoReto.EXITO, 3.0)
        assertNull(s.segundosInterrumpidos())
        assertTrue(s.hayExito)
    }
}
