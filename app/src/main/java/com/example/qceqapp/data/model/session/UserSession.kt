package com.example.qceqapp.data.model.session
import com.example.qceqapp.data.model.Entities

object UserSession {

    private var loginResponse: Entities.LoginResponse? = null

    // Guardar datos de login
    fun saveUser(user: Entities.LoginResponse) {
        loginResponse = user
    }

    // Limpiar sesión
    fun clearSession() {
        loginResponse = null
    }

    // Obtener datos del usuario
    fun getUsername(): String = loginResponse?.qcUsername ?: ""
    fun getName(): String = loginResponse?.qcuName ?: ""
    fun getRole(): String = loginResponse?.qcuRole ?: ""
    fun getEmail(): String = loginResponse?.qcuEmail ?: ""
    fun getCompany(): String = loginResponse?.qcuCompany ?: ""

    // Verificar si hay sesión activa
    fun isLoggedIn(): Boolean = loginResponse != null && !loginResponse?.qcUsername.isNullOrBlank()

    // Obtener respuesta completa
    fun getLoginResponse(): Entities.LoginResponse? = loginResponse
}