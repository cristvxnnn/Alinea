package com.example.alinea.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.alinea.core.EstadoEjercicio
import com.example.alinea.core.ResultadoEjercicio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "sesiones")
data class SesionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inicioMs: Long,
    val finMs: Long? = null,
    val estado: String,   // EN_CURSO | COMPLETADA | INTERRUMPIDA
)

@Entity(
    tableName = "resultados_ejercicio",
    foreignKeys = [
        ForeignKey(
            entity = SesionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sesionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sesionId"), Index("dia")],
)
data class ResultadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sesionId: Long,
    val ejercicioId: String,
    val dia: Long,                 // día local (ver diaLocal)
    val segundosObjetivo: Double,
    val segundosSostenidos: Double,
    val estado: String,            // COMPLETADO | SALTADO | INTERRUMPIDO
)

@Dao
interface ProgresoDao {
    @Insert
    suspend fun insertarSesion(sesion: SesionEntity): Long

    @Insert
    suspend fun insertarResultado(resultado: ResultadoEntity): Long

    @Query("UPDATE sesiones SET finMs = :finMs, estado = :estado WHERE id = :id")
    suspend fun cerrarSesion(id: Long, finMs: Long, estado: String)

    @Query("SELECT * FROM resultados_ejercicio")
    fun observarResultados(): Flow<List<ResultadoEntity>>

    // Borra también los resultados (ON DELETE CASCADE)
    @Query("DELETE FROM sesiones")
    suspend fun borrarSesiones()
}

@Database(entities = [SesionEntity::class, ResultadoEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progresoDao(): ProgresoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alinea.db",
                ).build().also { instancia = it }
            }
    }
}

class ProgresoRepositorio(private val dao: ProgresoDao) {

    /** Se actualiza solo cada vez que se guarda un resultado nuevo. */
    fun observarResultados(): Flow<List<ResultadoEjercicio>> =
        dao.observarResultados().map { lista ->
            lista.map {
                ResultadoEjercicio(
                    ejercicioId = it.ejercicioId,
                    dia = it.dia,
                    segundosObjetivo = it.segundosObjetivo,
                    segundosSostenidos = it.segundosSostenidos,
                    estado = EstadoEjercicio.valueOf(it.estado),
                )
            }
        }

    suspend fun iniciarSesion(ahoraMs: Long): Long =
        dao.insertarSesion(SesionEntity(inicioMs = ahoraMs, estado = "EN_CURSO"))

    /** Se llama al terminar CADA ejercicio, no al final de la sesión. */
    suspend fun registrar(sesionId: Long, r: ResultadoEjercicio) {
        dao.insertarResultado(
            ResultadoEntity(
                sesionId = sesionId,
                ejercicioId = r.ejercicioId,
                dia = r.dia,
                segundosObjetivo = r.segundosObjetivo,
                segundosSostenidos = r.segundosSostenidos,
                estado = r.estado.name,
            )
        )
    }

    suspend fun cerrarSesion(sesionId: Long, ahoraMs: Long, completada: Boolean) =
        dao.cerrarSesion(sesionId, ahoraMs, if (completada) "COMPLETADA" else "INTERRUMPIDA")

    suspend fun borrarHistorial() = dao.borrarSesiones()
}