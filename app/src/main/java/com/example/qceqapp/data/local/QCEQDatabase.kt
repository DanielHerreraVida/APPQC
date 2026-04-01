package com.example.qceqapp.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room de la aplicación QCEQ.
 *
 * ## Patrón de acceso (sin Context en ViewModel):
 *
 * 1. [QCEQApplication.onCreate] llama [init] una sola vez con el Application context.
 * 2. El ViewModel llama [getInstance] (sin parámetros) para obtener la instancia ya inicializada.
 *
 * Esto desacopla completamente el ViewModel de cualquier dependencia de Context,
 * manteniendo la arquitectura existente intacta.
 *
 * exportSchema = false: no genera JSON de schema (no hay tests de migración configurados).
 */
@Database(
    entities = [PendingReleaseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QCEQDatabase : RoomDatabase() {

    abstract fun pendingReleaseDao(): PendingReleaseDao

    companion object {
        private const val DB_NAME = "qceq_database"
        private const val TAG = "QCEQDatabase"

        @Volatile
        private var INSTANCE: QCEQDatabase? = null

        /**
         * Inicializa la base de datos. Debe llamarse UNA SOLA VEZ desde [QCEQApplication.onCreate].
         * Es idempotente — si ya fue inicializada, no hace nada.
         */
        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            QCEQDatabase::class.java,
                            DB_NAME
                        ).build()
                        Log.d(TAG, "SQLite: database initialized")
                    }
                }
            }
        }

        /**
         * Retorna la instancia ya inicializada.
         * Lanza [IllegalStateException] si se llama antes de [init] —
         * lo que nunca ocurre en uso normal porque [QCEQApplication.onCreate]
         * se ejecuta antes que cualquier Activity/Fragment/ViewModel.
         */
        fun getInstance(): QCEQDatabase {
            return INSTANCE
                ?: throw IllegalStateException(
                    "QCEQDatabase.init() must be called before getInstance(). " +
                    "Ensure QCEQApplication is registered in AndroidManifest.xml."
                )
        }
    }
}
