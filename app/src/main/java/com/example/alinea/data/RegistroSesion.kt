package com.example.alinea.data

import android.util.Log
import com.example.alinea.core.EstadoEjercicio
import com.example.alinea.core.ResultadoEjercicio
import com.example.alinea.core.diaLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TimeZone
import java.util.concurrent.Executors

/** Un solo hilo: las escrituras ocurren en el orden pedido y no se cancelan al salir de la pantalla. */
object ProgresoAlcance {
    val alcance = CoroutineScope(
        SupervisorJob() + Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )
}

/**
 * Una sesión agrupa los ejercicios hechos en un mismo entrenamiento. La sesión se crea al guardar
 * el primer resultado, así que entrar y salir sin hacer nada no deja registros vacíos.
 */
class RegistroSesion(
    private val repo: ProgresoRepositorio,
    private val alcance: CoroutineScope,
) {
    private val candado = Mutex()
    private var sesionId: Long? = null

    fun registrar(
        ejercicioId: String,
        segundosObjetivo: Double,
        segundosSostenidos: Double,
        estado: EstadoEjercicio,
    ) {
        val ahora = System.currentTimeMillis()
        val dia = diaLocal(ahora, TimeZone.getDefault().getOffset(ahora))
        alcance.launch {
            try {
                candado.withLock {
                    val id = sesionId ?: repo.iniciarSesion(ahora).also { sesionId = it }
                    repo.registrar(
                        id,
                        ResultadoEjercicio(ejercicioId, dia, segundosObjetivo, segundosSostenidos, estado),
                    )
                }
            } catch (e: Exception) {
                // Un fallo al guardar no debe cerrar la app
                Log.e("RegistroSesion", "No se pudo guardar el resultado: ${e.message}")
            }
        }
    }

    fun cerrar(completada: Boolean) {
        val ahora = System.currentTimeMillis()
        alcance.launch {
            try {
                candado.withLock {
                    val id = sesionId ?: return@withLock
                    repo.cerrarSesion(id, ahora, completada)
                }
            } catch (e: Exception) {
                Log.e("RegistroSesion", "No se pudo cerrar la sesión: ${e.message}")
            }
        }
    }
}