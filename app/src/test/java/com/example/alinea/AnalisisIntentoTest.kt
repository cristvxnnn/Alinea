package com.example.alinea

import com.example.alinea.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalisisIntentoTest {
    private val reto = Reto(
        "t", "T",
        listOf(
            Criterio(listOf(11, 13, 15), 160.0, 180.0),
            Criterio(listOf(23, 25, 27), 80.0, 100.0),
        ),
        3.0,
    )

    private fun r(
        estado: EstadoReto,
        a: Postura = Postura.APROBADO,
        b: Postura = Postura.APROBADO,
    ): Resultado {
        val global = when {
            a == Postura.INCORRECTO || b == Postura.INCORRECTO -> Postura.INCORRECTO
            a == Postura.CORRIGIENDO || b == Postura.CORRIGIENDO -> Postura.CORRIGIENDO
            else -> Postura.APROBADO
        }
        return Resultado(
            listOf(
                ResultadoCriterio(170.0, a, listOf(11, 13, 15)),
                ResultadoCriterio(90.0, b, listOf(23, 25, 27)),
            ),
            global, estado, 0f, null,
        )
    }

    @Test
    fun laPrecisionEsLaParteDeFotogramasConTodoEnVerde() {
        val an = AnalisisIntento(reto)
        repeat(3) { an.alEvaluar(r(EstadoReto.SOSTENIENDO)) }
        an.alEvaluar(r(EstadoReto.SOSTENIENDO, b = Postura.INCORRECTO))
        val fin = r(EstadoReto.EXITO)
        an.alEvaluar(fin)

        val s = an.resumen(fin)
        assertEquals(1, s.intentos)
        assertEquals(5, s.fotogramasSostenidos)
        assertEquals(4, s.fotogramasCorrectos)
        assertEquals(0.8f, s.precision, 0.001f)
        assertEquals(0, s.criterios[0].fotogramasFuera)
        assertEquals(1, s.criterios[1].fotogramasFuera)
        assertEquals("Rodilla izquierda", s.masExigido?.etiqueta)
        assertEquals(20, s.pctFuera(s.criterios[1]))
    }

    @Test
    fun unReinicioEsUnIntentoNuevoYReseteaLosContadores() {
        val an = AnalisisIntento(reto)
        an.alEvaluar(r(EstadoReto.SOSTENIENDO))
        an.alEvaluar(r(EstadoReto.SOSTENIENDO))
        an.alEvaluar(r(EstadoReto.ANALIZANDO, a = Postura.INCORRECTO))
        an.alEvaluar(r(EstadoReto.SOSTENIENDO))
        val fin = r(EstadoReto.EXITO)
        an.alEvaluar(fin)

        val s = an.resumen(fin)
        assertEquals(2, s.intentos)
        assertEquals(1, s.reinicios)
        assertEquals(2, s.fotogramasSostenidos)
    }

    @Test
    fun perderElCuerpoCuentaComoReinicio() {
        val an = AnalisisIntento(reto)
        an.alEvaluar(r(EstadoReto.SOSTENIENDO))
        an.alPerderCuerpo()
        an.alEvaluar(r(EstadoReto.SOSTENIENDO))
        val fin = r(EstadoReto.EXITO)
        an.alEvaluar(fin)
        assertEquals(2, an.resumen(fin).intentos)
    }

    @Test
    fun sinFallosNoHayCriterioMasExigido() {
        val an = AnalisisIntento(reto)
        an.alEvaluar(r(EstadoReto.SOSTENIENDO))
        val fin = r(EstadoReto.EXITO)
        an.alEvaluar(fin)
        assertNull(an.resumen(fin).masExigido)
    }

    @Test
    fun lasEtiquetasUsanElNombreDeLaArticulacion() {
        assertEquals("Codo izquierdo", etiquetaCriterio(listOf(11, 13, 15), false))
        assertEquals(
            "Distancia: rodilla izquierda – rodilla derecha",
            etiquetaCriterio(listOf(25, 26), true),
        )
    }
}