package com.example.alinea.data

import android.content.Context
import com.example.alinea.core.Criterio
import com.example.alinea.core.INDICE_LANDMARK
import com.example.alinea.core.Reto
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
                val nombres = c.getJSONArray("puntos")
                require(nombres.length() == 3) { "$id: cada criterio necesita 3 puntos" }
                val indices = (0 until 3).map { j ->
                    val n = nombres.getString(j)
                    INDICE_LANDMARK[n]
                        ?: throw IllegalArgumentException("$id: punto desconocido '$n'")
                }
                val rango = c.getJSONArray("rango_esperado")
                val min = rango.getDouble(0)
                val max = rango.getDouble(1)
                require(min <= max) { "$id: rango_esperado inválido" }
                Criterio(indices, min, max)
            }
            retos.add(
                Reto(id, o.getString("nombre"), criterios, o.getDouble("hold_time_seconds"))
            )
        }
        return retos
    }
}