package com.example.qceqapp.uis.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.qceqapp.R
import com.example.qceqapp.data.network.Constants
import com.example.qceqapp.databinding.ActivityLoginBinding
import com.example.qceqapp.uis.main.MainActivity
import com.example.qceqapp.utils.SessionManager
import com.example.qceqapp.utils.UpdateManager
import com.example.qceqapp.data.model.GithubRelease
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var isPasswordVisible = false
    private lateinit var sessionManager: SessionManager
    private lateinit var updateManager: UpdateManager  // ⭐ AGREGAR ESTA LÍNEA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        updateManager = UpdateManager(this)  // ⭐ INICIALIZAR AQUÍ

        initializeBaseUrl()
        updateEnvLabel()

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
        checkForUpdates()

    }
    private fun checkForUpdates() {
        lifecycleScope.launch {
            try {
                val (hasUpdate, newVersion, release) = updateManager.checkForUpdates()

                if (hasUpdate && release != null) {
                    showUpdateDialog(release)
                }
            } catch (e: Exception) {
                // Silenciosamente falla si no hay internet o GitHub no está disponible
                // No mostramos error al usuario para no interrumpir el flujo de login
                e.printStackTrace()
            }
        }
    }
    private fun initializeBaseUrl() {
        val savedUrl = sessionManager.getStoredUrl()
        if (!savedUrl.isNullOrEmpty()) {
            Constants.BASE_URL = savedUrl
        } else {
            sessionManager.saveUrl(Constants.BASE_URL)
        }
    }

    private fun environmentName(url: String): String = when (url) {
        Constants.DEV_URL -> "DEV"
        Constants.STAGE_URL -> "STAGE"
        Constants.PROD_URL -> "PROD"
        else -> "CUSTOM"
    }

    private fun updateEnvLabel() {
        binding.tvCurrentEnv.text =
            "${environmentName(Constants.BASE_URL)} • ${Constants.BASE_URL}"
    }

    private fun showEnvironmentDialog() {
        val environments = arrayOf(
            "DEV • ${Constants.DEV_URL}",
            "STAGE • ${Constants.STAGE_URL}",
            "PROD • ${Constants.PROD_URL}",
            "Custom URL..."
        )
        val urls = arrayOf(Constants.DEV_URL, Constants.STAGE_URL, Constants.PROD_URL)
        val checkedItem = urls.indexOf(Constants.BASE_URL).let { if (it == -1) 3 else it }

        MaterialAlertDialogBuilder(this)
            .setTitle("Server environment")
            .setSingleChoiceItems(environments, checkedItem) { dialog, which ->
                if (which == 3) {
                    dialog.dismiss()
                    showCustomUrlDialog()
                } else {
                    applyNewUrl(urls[which])
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomUrlDialog() {
        val input = android.widget.EditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setText(Constants.BASE_URL)
            setSelection(text.length)
        }
        val container = android.widget.FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Custom server URL")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                var url = input.text.toString().trim()
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    Toast.makeText(this, "URL must start with http:// or https://", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                if (!url.endsWith("/")) url += "/"
                applyNewUrl(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyNewUrl(newUrl: String) {
        if (newUrl == Constants.BASE_URL) {
            updateEnvLabel()
            return
        }
        Constants.BASE_URL = newUrl
        Constants.token = ""   // el token del ambiente anterior no sirve en el nuevo
        sessionManager.saveUrl(newUrl)
        updateEnvLabel()
        Toast.makeText(
            this,
            "Server changed to ${environmentName(newUrl)}",
            Toast.LENGTH_SHORT
        ).show()
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

        // Acceso discreto al selector de ambiente: long-press sobre el logo.
        // El botón de settings y la etiqueta de URL están ocultos (visibility=gone)
        // para mantener la pantalla de login limpia.
        binding.logoCard.setOnLongClickListener {
            showEnvironmentDialog()
            true
        }
    }
    private fun showUpdateDialog(release: GithubRelease) {
        val apkAsset = release.assets.find { it.name.endsWith(".apk") }

        if (apkAsset == null) {
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(" Nueva actualización disponible")
            .setMessage(
                "Versión ${release.tagName} está disponible\n\n" +
                        "${release.description?.take(200) ?: "Nuevas mejoras y correcciones"}\n\n" +
                        "Tamaño: ${formatFileSize(apkAsset.size)}"
            )
            .setPositiveButton("Actualizar ahora") { _, _ ->
                updateManager.downloadAndInstallUpdate(
                    apkAsset.downloadUrl,
                    release.tagName
                )
                Toast.makeText(
                    this,
                    "Descargando actualización...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Más tarde", null)
            .setCancelable(false)
            .show()
    }
    private fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1) {
            String.format("%.2f MB", mb)
        } else {
            String.format("%.2f KB", kb)
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