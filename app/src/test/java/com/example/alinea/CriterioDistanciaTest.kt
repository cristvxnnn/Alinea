package com.example.alinea

import com.example.alinea.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CriterioDistanciaTest {
    private val reto = Reto(
        "d", "D",
        listOf(Criterio(listOf(25, 26), 1.4, 4.0, TipoCriterio.DISTANCIA)),
        3.0,
    )

    /** Hombros en (0,0) y (2,0): ancho 2. */
    private fun cuerpo(rodillaIzq: Punto, rodillaDer: Punto): List<Punto> {
        val p = MutableList(33) { Punto(0f, 0f) }
        p[11] = Punto(0f, 0f)
        p[12] = Punto(2f, 0f)
        p[25] = rodillaIzq
        p[26] = rodillaDer
        return p
    }

    @Test
    fun midelaDistanciaEnAnchosDeHombro() {
        val r = EvaluadorReto(reto).evaluar(cuerpo(Punto(-1f, 5f), Punto(3f, 5f)), 0.0)   // 4 / 2 = 2.0
        assertEquals(2.0, r.criterios[0].angulo, 0.0001)
        assertTrue(r.criterios[0].esDistancia)
        assertEquals(Postura.APROBADO, r.postura)
    }

    @Test
    fun demasiadoJuntoNoCumple() {
        val r = EvaluadorReto(reto).evaluar(cuerpo(Punto(0.5f, 5f), Punto(1.5f, 5f)), 0.0)   // 0.5
        assertEquals(Postura.INCORRECTO, r.postura)
    }

    @Test
    fun cercaDelRangoEsCorrigiendo() {
        val r = EvaluadorReto(reto).evaluar(cuerpo(Punto(-0.3f, 5f), Punto(2.3f, 5f)), 0.0)   // 1.3
        assertEquals(Postura.CORRIGIENDO, r.postura)
    }

    @Test
    fun sinAnchoDeHombrosNoAprueba() {
        val p = MutableList(33) { Punto(0f, 0f) }
        p[25] = Punto(-1f, 5f)
        p[26] = Punto(3f, 5f)
        assertEquals(Postura.INCORRECTO, EvaluadorReto(reto).evaluar(p, 0.0).postura)
    }

    @Test
    fun laDistanciaPintaSusDosPuntos() {
        val r = EvaluadorReto(reto).evaluar(cuerpo(Punto(-1f, 5f), Punto(3f, 5f)), 0.0)
        val m = estadoPorPunto(r.criterios)
        assertEquals(Postura.APROBADO, m[25])
        assertEquals(Postura.APROBADO, m[26])
    }
}