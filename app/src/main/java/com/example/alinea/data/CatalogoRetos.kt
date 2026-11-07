package com.example.alinea.data

import android.content.Context
import com.example.alinea.core.Criterio
import com.example.alinea.core.INDICE_LANDMARK
import com.example.alinea.core.Reto
import com.example.alinea.core.TipoCriterio
import com.example.alinea.core.VistaCalibracion
import org.json.JSONObject

object CatalogoRetos {

    fun cargar(context: Context): List<Reto> {
        val texto = context.assets.open("ejercicios.json")
            .bufferedReader().use { it.readText() }
        return parsear(texto)
    }

    fun parsear(texto: String): List<Reto> {
        val raiz = JSONObject(texto)
        val retos = mutableListOf<Reto>()
        for (id in raiz.keys()) {
            val o = raiz.getJSONObject(id)
            val criteriosJson = o.getJSONArray("criterios")
            val criterios = (0 until criteriosJson.length()).map { i ->
                val c = criteriosJson.getJSONObject(i)
                val tipo = if (c.optString("tipo", "angulo").equals("distancia", ignoreCase = true))
                    TipoCriterio.DISTANCIA else TipoCriterio.ANGULO
                val esperados = if (tipo == TipoCriterio.DISTANCIA) 2 else 3
                val nombres = c.getJSONArray("puntos")
                require(nombres.length() == esperados) {
                    "$id: un criterio de $tipo necesita $esperados puntos"
                }
                val indices = (0 until esperados).map { j ->
                    val n = nombres.getString(j)
                    INDICE_LANDMARK[n]
                        ?: throw IllegalArgumentException("$id: punto desconocido '$n'")
                }
                val rango = c.getJSONArray("rango_esperado")
                val min = rango.getDouble(0)
                val max = rango.getDouble(1)
                require(min <= max) { "$id: rango_esperado inválido" }
                Criterio(indices, min, max, tipo)
            }
            retos.add(
                Reto(
                    id = id,
                    nombre = o.getString("nombre"),
                    criterios = criterios,
                    tiempoObjetivo = o.getDouble("hold_time_seconds"),
                    categoria = o.optString("categoria", "General"),
                    instrucciones = o.optString("instrucciones", ""),
                    imagen = o.optString("imagen", "").ifBlank { null },
                    pasos = o.optJSONArray("pasos")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList(),
                    vista = VistaCalibracion.desdeTexto(o.optString("vista", "")),
                )
            )
        }
        return retos
    }
}