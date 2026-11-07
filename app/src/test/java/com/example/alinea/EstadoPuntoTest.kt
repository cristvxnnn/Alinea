package com.example.alinea

import com.example.alinea.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoPuntosTest {
    private fun c(estado: Postura, vararg idx: Int) = ResultadoCriterio(0.0, estado, idx.toList())

    @Test
    fun elVerticeTomaElEstadoDelCriterio() {
        val m = estadoPorPunto(
            listOf(c(Postura.APROBADO, 11, 13, 15), c(Postura.INCORRECTO, 23, 25, 27))
        )
        assertEquals(Postura.APROBADO, m[13])
        assertEquals(Postura.INCORRECTO, m[25])
        assertNull(m[11])   // los extremos que no son vértice no se colorean
    }

    @Test
    fun gananElPeorEstadoSiHayVariosCriteriosEnElMismoPunto() {
        val m = estadoPorPunto(
            listOf(c(Postura.APROBADO, 11, 13, 15), c(Postura.CORRIGIENDO, 12, 13, 16))
        )
        assertEquals(Postura.CORRIGIENDO, m[13])
        val n = estadoPorPunto(
            listOf(c(Postura.CORRIGIENDO, 11, 13, 15), c(Postura.INCORRECTO, 12, 13, 16))
        )
        assertEquals(Postura.INCORRECTO, n[13])
    }

    @Test
    fun losHuesosSeIdentificanSinImportarElOrden() {
        val m = estadoPorHueso(listOf(c(Postura.INCORRECTO, 15, 13, 11)))
        assertEquals(Postura.INCORRECTO, m[11 to 13])
        assertEquals(Postura.INCORRECTO, m[13 to 15])
        assertNull(m[11 to 15])
    }

    @Test
    fun losCriteriosSinIndicesSeIgnoran() {
        val m = estadoPorPunto(listOf(ResultadoCriterio(90.0, Postura.INCORRECTO)))
        assertTrue(m.isEmpty())
    }
}