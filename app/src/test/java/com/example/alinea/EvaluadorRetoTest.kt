package com.example.alinea

import com.example.alinea.core.*
import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluadorRetoTest {

    private val reto = Reto(
        id = "prueba",
        nombre = "Prueba",
        criterios = listOf(
            Criterio(listOf(11, 13, 15), 160.0, 180.0),  // brazo izquierdo
            Criterio(listOf(23, 25, 27), 80.0, 100.0),   // pierna izquierda
        ),
        tiempoObjetivo = 3.0,
    )

    private fun nuevo() = EvaluadorReto(reto)

    /** Cuerpo de 33 puntos: brazo recto (180°) o doblado (90°); pierna a 90° o recta. */
    private fun cuerpo(brazoRecto: Boolean, piernaA90: Boolean): List<Punto> {
        val p = MutableList(33) { Punto(0f, 0f) }
        p[11] = Punto(0f, 0f)
        p[13] = Punto(1f, 0f)
        p[15] = if (brazoRecto) Punto(2f, 0f) else Punto(1f, 1f)
        p[23] = Punto(0f, 5f)
        p[25] = Punto(1f, 5f)
        p[27] = if (piernaA90) Punto(1f, 6f) else Punto(2f, 5f)
        return p
    }

    @Test
    fun anguloRectoEs180() {
        assertEquals(180.0, calcularAngulo(Punto(0f, 0f), Punto(1f, 0f), Punto(2f, 0f)), 0.001)
    }

    @Test
    fun anguloDobladoEs90() {
        assertEquals(90.0, calcularAngulo(Punto(0f, 0f), Punto(1f, 0f), Punto(1f, 1f)), 0.001)
    }

    @Test
    fun exitoSoloSiTodosLosCriteriosCumplen() {
        val ev = nuevo()
        val bien = cuerpo(brazoRecto = true, piernaA90 = true)
        assertEquals(EstadoReto.SOSTENIENDO, ev.evaluar(bien, 0.0).estado)
        assertEquals(EstadoReto.EXITO, ev.evaluar(bien, 3.1).estado)
    }

    @Test
    fun unCriterioFallidoImpideAvanzar() {
        val ev = nuevo()
        val r = ev.evaluar(cuerpo(brazoRecto = true, piernaA90 = false), 0.0)
        assertEquals(Postura.INCORRECTO, r.postura)
        assertEquals(EstadoReto.ANALIZANDO, r.estado)
    }

    @Test
    fun perderUnCriterioMientrasSostieneReiniciaYAvisa() {
        val ev = nuevo()
        ev.evaluar(cuerpo(true, true), 0.0)
        val r = ev.evaluar(cuerpo(true, false), 1.0)
        assertEquals(EstadoReto.ANALIZANDO, r.estado)
        assertEquals(Sonido.ERROR, r.sonido)
    }

    @Test
    fun enExitoSigueDandoLosAngulosReales() {
        val ev = nuevo()
        ev.evaluar(cuerpo(true, true), 0.0)
        ev.evaluar(cuerpo(true, true), 3.1)
        val r = ev.evaluar(cuerpo(brazoRecto = false, piernaA90 = true), 4.0)
        assertEquals(EstadoReto.EXITO, r.estado)
        assertEquals(90.0, r.criterios[0].angulo, 0.001)
    }

    @Test
    fun corrigiendoCuandoEstaCercaDelRango() {
        val retoSimple = Reto("s", "S", listOf(Criterio(listOf(11, 13, 15), 160.0, 180.0)), 3.0)
        val p = MutableList(33) { Punto(0f, 0f) }
        p[11] = Punto(0f, 0f)
        p[13] = Punto(1f, 0f)
        p[15] = Punto(1.866f, 0.5f)   // ángulo de ~150°
        val r = EvaluadorReto(retoSimple).evaluar(p, 0.0)
        assertEquals(Postura.CORRIGIENDO, r.postura)
    }

    @Test
    fun perderCuerpoSoloReiniciaSiEstabaSosteniendo() {
        val ev = nuevo()
        assertEquals(null, ev.perderCuerpo())
        ev.evaluar(cuerpo(true, true), 0.0)
        assertEquals(Sonido.ERROR, ev.perderCuerpo())
        assertEquals(EstadoReto.ANALIZANDO, ev.estado)
    }
}