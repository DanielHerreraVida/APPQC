package com.example.qceqapp.uis.QCInspection

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.qceqapp.R
import com.example.qceqapp.utils.PermissionHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QCMediaActivity : AppCompatActivity() {

    private val viewModel: QCMediaViewModel by viewModels()
    private var isNavigating = false

    // ✅ Launcher para solicitar permisos con tipo explícito
    private val requestPermissionsLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions: Map<String, Boolean> ->
            val allGranted = permissions.entries.all { it.value }

            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                // Algunos permisos fueron denegados
                AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("Camera and storage permissions are required for this feature. Some functionality may not work without these permissions.")
                    .setPositiveButton("Grant Permissions") { _, _ ->
                        requestPermissionsLauncher.launch(PermissionHelper.getRequiredPermissions())
                    }
                    .setNegativeButton("Continue Anyway", null)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qc_media)

        // ✅ Verificar permisos al inicio
        if (!PermissionHelper.hasAllPermissions(this)) {
            requestPermissionsLauncher.launch(PermissionHelper.getRequiredPermissions())
        }

        val navigation = findViewById<BottomNavigationView>(R.id.navigationMediaBtns)
        viewModel.boxIdToInspect = intent.getStringExtra("boxIdToInspect") ?: ""
        viewModel.orderNum = intent.getStringExtra("orderNum") ?: ""
        val jsonImages = intent.getStringExtra("listImagesCaptured")
        val jsonVideos = intent.getStringExtra("listVideosCaptured")

        val gson = Gson()

        if (!jsonImages.isNullOrEmpty()) {
            val imagesList: MutableList<String> = gson.fromJson(
                jsonImages,
                object : TypeToken<MutableList<String>>() {}.type
            )
            viewModel.photoPaths.value = imagesList
        }

        if (!jsonVideos.isNullOrEmpty()) {
            val videosList: MutableList<String> = gson.fromJson(
                jsonVideos,
                object : TypeToken<MutableList<String>>() {}.type
            )
            viewModel.videoPaths.value = videosList
        }

        if (savedInstanceState == null) {
            openPhotoFragment()
        }

        navigation.setOnItemSelectedListener { item ->
            if (isNavigating) return@setOnItemSelectedListener true

            when (item.itemId) {
                R.id.navigation_pic -> {
                    openPhotoFragment()
                    true
                }
                R.id.navigation_vid -> {
                    openVideoFragment()
                    true
                }
                else -> false
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            onContinue()
        }
    }

    private fun openPhotoFragment() {
        isNavigating = true

        val fragment = QCEvidencePhotoFragment().apply {
            arguments = Bundle().apply {
                putString("boxIdToInspect", viewModel.boxIdToInspect)
                putString("orderNum", viewModel.orderNum)

                val gson = Gson()
                if (!viewModel.photoPaths.value.isNullOrEmpty()) {
                    putString("listImagesCaptured", gson.toJson(viewModel.photoPaths.value))
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        val bottomNav = findViewById<BottomNavigationView>(R.id.navigationMediaBtns)
        bottomNav.selectedItemId = R.id.navigation_pic

        isNavigating = false
    }

    fun openVideoFragment(listImagesCaptured: String? = null) {
        isNavigating = true

        val fragment = QCEvidenceVideoFragment().apply {
            arguments = Bundle().apply {
                putString("boxIdToInspect", viewModel.boxIdToInspect)
                putString("orderNum", viewModel.orderNum)

                if (listImagesCaptured != null) {
                    putString("listImagesCaptured", listImagesCaptured)
                }

                val gson = Gson()
                if (!viewModel.videoPaths.value.isNullOrEmpty()) {
                    putString("listVideosCaptured", gson.toJson(viewModel.videoPaths.value))
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        val bottomNav = findViewById<BottomNavigationView>(R.id.navigationMediaBtns)
        bottomNav.selectedItemId = R.id.navigation_vid

        isNavigating = false
    }

    fun onContinue() {
        val gson = Gson()

        val jsonImages = gson.toJson(viewModel.photoPaths.value ?: emptyList<String>())
        val jsonVideos = gson.toJson(viewModel.videoPaths.value ?: emptyList<String>())

        val resultIntent = Intent().apply {
            putExtra("listImagesCaptured", jsonImages)
            putExtra("listVideosCaptured", jsonVideos)
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }
}