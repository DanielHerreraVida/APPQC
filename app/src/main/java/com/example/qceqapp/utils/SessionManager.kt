package com.example.qceqapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val TAG = "SessionManager"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_COMPANY = "user_company"
        private const val KEY_BASE_URL = "base_url"
    }

    private val sharedPreferences: SharedPreferences = createEncryptedPrefs(context)

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            buildEncryptedPrefs(context)
        } catch (e: Exception) {
            // AEADBadTagException o KeyStoreException: la clave AES-GCM fue destruida
            // (reinstalación de app, limpieza del Keystore) pero el archivo cifrado sobrevivió.
            // El archivo es irrecuperable — se borra para crear uno limpio.
            // Consecuencia esperada: el usuario deberá iniciar sesión nuevamente.
            Log.w(TAG, "EncryptedSharedPreferences corrupted (key mismatch). Clearing and recreating. Cause: ${e.message}")
            context.deleteSharedPreferences(PREFS_NAME)
            try {
                buildEncryptedPrefs(context)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to recreate EncryptedSharedPreferences after clearing: ${e2.message}")
                throw e2
            }
        }
    }

    private fun buildEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(username: String, password: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            putBoolean(KEY_REMEMBER_ME, true)
            apply()
        }
    }

    fun saveUserData(response: com.example.qceqapp.data.model.Entities.LoginResponse) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, response.qcUsername)
            putString(KEY_USER_NAME, response.qcuName)
            putString(KEY_USER_ROLE, response.qcuRole)
            putString(KEY_USER_EMAIL, response.qcuEmail)
            putString(KEY_USER_COMPANY, response.qcuCompany)
            apply()
        }
    }

    fun getUsername(): String? = sharedPreferences.getString(KEY_USERNAME, null)

    fun getPassword(): String? = sharedPreferences.getString(KEY_PASSWORD, null)

    fun isRememberMeEnabled(): Boolean = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)

    fun clearCredentials() {
        sharedPreferences.edit().apply {
            remove(KEY_USERNAME)
            remove(KEY_PASSWORD)
            remove(KEY_REMEMBER_ME)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ROLE)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_COMPANY)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return !getUsername().isNullOrEmpty() && !getPassword().isNullOrEmpty()
    }

    fun saveUrl(url: String) {
        sharedPreferences.edit().apply {
            putString(KEY_BASE_URL, url)
            apply()
        }
    }

    fun getStoredUrl(): String? = sharedPreferences.getString(KEY_BASE_URL, null)

    fun clearUrl() {
        sharedPreferences.edit().apply {
            remove(KEY_BASE_URL)
            apply()
        }
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}