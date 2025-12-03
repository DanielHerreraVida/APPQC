package com.example.qceqapp.uis.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.qceqapp.R
import com.example.qceqapp.data.network.Constants
import com.example.qceqapp.databinding.ActivityLoginBinding
import com.example.qceqapp.uis.main.MainActivity
import com.example.qceqapp.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var isPasswordVisible = false
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // IMPORTANTE: Cargar URL guardada ANTES de cualquier otra operación
        initializeBaseUrl()

        setupListeners()
        observeViewModel()

        if (sessionManager.isLoggedIn()) {
            val username = sessionManager.getUsername()
            val password = sessionManager.getPassword()
            if (username != null && password != null) {
                binding.etUsername.setText(username)
                binding.etPassword.setText(password)
                viewModel.login(username, password)
            }
        }
    }

    /**
     * Inicializa la URL base desde SharedPreferences
     * Si no existe, guarda la URL por defecto actual
     */
    private fun initializeBaseUrl() {
        val savedUrl = sessionManager.getStoredUrl()
        if (!savedUrl.isNullOrEmpty()) {
            // Hay una URL guardada, usarla
            Constants.BASE_URL = savedUrl
        } else {
            // No hay URL guardada, guardar la actual como default
            sessionManager.saveUrl(Constants.BASE_URL)
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sessionManager.saveCredentials(username, password)
            viewModel.login(username, password)
        }

        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }

            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.errorMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.loginSuccess.observe(this) { response ->
            response?.let {
                sessionManager.saveUserData(it)

                val welcomeMessage = "Welcome ${it.qcuName ?: it.qcUsername ?: ""}"
                Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("USER_ID", it.qcUsername)
                    putExtra("USER_NAME", it.qcuName)
                    putExtra("USER_ROLE", it.qcuRole)
                    putExtra("USER_EMAIL", it.qcuEmail)
                    putExtra("USER_COMPANY", it.qcuCompany)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etUsername.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.btnTogglePassword.isEnabled = !isLoading
    }
}