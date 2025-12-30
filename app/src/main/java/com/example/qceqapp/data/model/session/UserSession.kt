package com.example.qceqapp.data.model.session
import com.example.qceqapp.data.model.Entities

object UserSession {

    private var loginResponse: Entities.LoginResponse? = null

    fun saveUser(user: Entities.LoginResponse) {
        loginResponse = user
    }

    fun clearSession() {
        loginResponse = null
    }

    fun getUsername(): String = loginResponse?.qcUsername ?: ""
    fun getName(): String = loginResponse?.qcuName ?: ""
    fun getRole(): String = loginResponse?.qcuRole ?: ""
    fun getEmail(): String = loginResponse?.qcuEmail ?: ""
    fun getCompany(): String = loginResponse?.qcuCompany ?: ""

    fun isLoggedIn(): Boolean = loginResponse != null && !loginResponse?.qcUsername.isNullOrBlank()

    fun getLoginResponse(): Entities.LoginResponse? = loginResponse
}