package com.example.qceqapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para persistir los pending items de ToReleaseFragment.
 *
 * Refleja exactamente los campos de [com.example.qceqapp.uis.torelease.PendingReleaseItem]:
 *  - [box]: código de caja (String numérico). Es PRIMARY KEY natural — previene duplicados a nivel DB.
 *  - [scannedAt]: timestamp Unix (ms) para restaurar el orden de escaneo.
 *
 * La tabla se llama "pending_release" para evitar colisiones con posibles tablas futuras.
 */
@Entity(tableName = "pending_release")
data class PendingReleaseEntity(
    @PrimaryKey
    val box: String,
    val scannedAt: Long
)
