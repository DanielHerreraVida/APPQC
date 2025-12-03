package com.example.qceqapp.uis.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.qceqapp.data.model.session.UserSession
import com.example.qceqapp.data.network.Constants
import com.example.qceqapp.databinding.ActivitySettingsBinding
import com.example.qceqapp.uis.login.LoginActivity
import com.example.qceqapp.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.qceqapp.data.network.Service
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sessionManager: SessionManager
    private var tempUrl: String = ""
    private val service = com.example.qceqapp.data.network.Service()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupToolbar()
        loadCurrentUrl()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Connection Settings"
        }
    }

    private fun loadCurrentUrl() {
        val currentUrl = sessionManager.getStoredUrl() ?: Constants.BASE_URL
        binding.tvCurrentUrl.text = currentUrl
        binding.etNewUrl.setText(currentUrl)
        tempUrl = currentUrl
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val newUrl = binding.etNewUrl.text.toString().trim()

            if (newUrl.isEmpty()) {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newUrl == tempUrl) {
                Toast.makeText(this, "URL hasn't changed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveNewUrl(newUrl)
        }

//        binding.btnViewLog.setOnClickListener {
//            Toast.makeText(this, "View Log - To be implemented", Toast.LENGTH_SHORT).show()
//        }
    }

    private fun saveNewUrl(newUrl: String) {
        lifecycleScope.launch {
            try {
                binding.btnSave.isEnabled = false
                binding.btnSave.text = "Saving..."
                val oldUrl = Constants.BASE_URL
                Constants.BASE_URL = newUrl
                val isHealthy = checkServiceHealth(newUrl)

                if (isHealthy) {
                    if (UserSession.isLoggedIn()) {
                        Constants.BASE_URL = tempUrl

                        withContext(Dispatchers.Main) {
                            showSuccessDialog(newUrl)
                        }
                    } else {
                        saveUrlAndGoToLogin(newUrl)
                    }
                } else {
                    Constants.BASE_URL = oldUrl

                    withContext(Dispatchers.Main) {
                        showErrorDialog("Cannot connect to the new URL. Please verify and try again.")
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Save"
                    }
                }

            } catch (e: Exception) {
                Constants.BASE_URL = tempUrl
                withContext(Dispatchers.Main) {
                    showErrorDialog("Error: ${e.message}")
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                }
            }
        }
    }

    private suspend fun checkServiceHealth(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = service.healthCheck()

                result.isSuccess && result.getOrNull()?.trim()?.uppercase() == "OK"
            } catch (e: Exception) {
                false
            }
        }
    }


    private fun showSuccessDialog(newUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("URL changed successfully! You need to log in again.")
            .setPositiveButton("OK") { _, _ ->
                performLogoutAndSaveUrl(newUrl)
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun performLogoutAndSaveUrl(newUrl: String) {
        lifecycleScope.launch {
            try {
                // Intentar hacer logout en el servidor
                // val response = servicio.logout(UserSession.getUserId())

                // Limpiar sesión local
                UserSession.clearSession()
                Constants.token = ""
                sessionManager.clearCredentials()

                // Guardar la nueva URL
                sessionManager.saveUrl(newUrl)
                Constants.BASE_URL = newUrl

                // Ir al login
                goToLogin()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Error during logout: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Aún así, limpiar sesión local y continuar
                UserSession.clearSession()
                Constants.token = ""
                sessionManager.clearCredentials()
                sessionManager.saveUrl(newUrl)
                Constants.BASE_URL = newUrl
                goToLogin()
            }
        }
    }

    private fun saveUrlAndGoToLogin(newUrl: String) {
        sessionManager.saveUrl(newUrl)
        Constants.BASE_URL = newUrl

        Toast.makeText(
            this,
            "URL changed successfully!",
            Toast.LENGTH_SHORT
        ).show()

        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}