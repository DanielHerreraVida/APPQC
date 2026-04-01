package com.example.qceqapp.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bus de eventos global para la sesión del usuario.
 *
 * Permite que la capa de red notifique a la UI cuando la sesión expira (401),
 * sin crear dependencias directas entre ambas capas.
 *
 * El [AtomicBoolean] garantiza que, aunque múltiples llamadas API
 * reciban 401 simultáneamente, solo se emite un único evento de logout.
 */
object SessionEventBus {

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    private val isHandlingExpiry = AtomicBoolean(false)

    /**
     * Emite el evento de sesión expirada.
     * Thread-safe: solo el primer llamador logra emitir; los demás son ignorados.
     */
    fun emitSessionExpired() {
        if (isHandlingExpiry.compareAndSet(false, true)) {
            _sessionExpired.tryEmit(Unit)
        }
    }

    /**
     * Resetea el flag para permitir que futuros eventos de 401
     * (tras un nuevo login) sean procesados correctamente.
     * Debe llamarse al completar el logout.
     */
    fun resetExpiry() {
        isHandlingExpiry.set(false)
    }
}
