package com.example.qceqapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO para operaciones sobre la tabla [PendingReleaseEntity].
 *
 * Todas las funciones son suspend — Room las ejecuta en Dispatchers.IO automáticamente
 * cuando se usan con room-ktx. No se necesita withContext(Dispatchers.IO) en el llamador.
 */
@Dao
interface PendingReleaseDao {

    /**
     * Carga todos los pending items ordenados por [PendingReleaseEntity.scannedAt] DESC.
     * Los más recientes primero — consistente con cómo se agregan en memoria (add(0, item)).
     */
    @Query("SELECT * FROM pending_release ORDER BY scannedAt DESC")
    suspend fun getAll(): List<PendingReleaseEntity>

    /**
     * Inserta un item. IGNORE previene crashes si el box ya existe (segunda línea de defensa
     * después de la validación en memoria de addPendingItem).
     * @return rowId insertado, o -1 si fue ignorado por conflicto (box ya existía).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: PendingReleaseEntity): Long

    /**
     * Elimina un item específico por su código de caja.
     * Usado en removePendingItem().
     * @return número de filas eliminadas (0 = el box no existía en la tabla).
     */
    @Query("DELETE FROM pending_release WHERE box = :box")
    suspend fun deleteByBox(box: String): Int

    /**
     * Elimina múltiples items por sus códigos de caja.
     * Usado en releaseAllPending() para eliminar los items exitosamente liberados.
     * @return número de filas eliminadas.
     */
    @Query("DELETE FROM pending_release WHERE box IN (:boxes)")
    suspend fun deleteByBoxes(boxes: List<String>): Int

    /**
     * Elimina todos los items de la tabla.
     * Usado en clearPendingItems().
     * @return número de filas eliminadas.
     */
    @Query("DELETE FROM pending_release")
    suspend fun deleteAll(): Int
}
