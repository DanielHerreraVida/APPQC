package com.example.qceqapp.uis.scanner

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.qceqapp.R
import com.example.qceqapp.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    // Para CameraX
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var useCameraX = false
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var currentZoomRatio = 1.0f
    private var isFlashOn = false

    companion object {
        private const val TAG = "CameraActivity"
        const val EXTRA_IMAGE_PATH = "image_path"
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_IMAGE_PATH, path)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        } else {
            Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        scaleGestureDetector = ScaleGestureDetector(this, PinchToZoomListener())

        if (hasCameraApp()) {
            useCameraX = false
            binding.previewView.isVisible = false
            binding.controlsLayout.isVisible = false
            binding.zoomIndicator.isVisible = false
            binding.tvLoading.isVisible = true
            launchNativeCamera()
        } else {
            useCameraX = true
            binding.tvLoading.isVisible = false
            binding.previewView.isVisible = true
            binding.controlsLayout.isVisible = true
            binding.zoomIndicator.isVisible = true
            startCameraX()
        }

        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        binding.btnToggleFlash.setOnClickListener {
            toggleFlash()
        }

        binding.previewView.setOnTouchListener { _, event ->
            if (useCameraX) {
                scaleGestureDetector.onTouchEvent(event)
            }
            true
        }
    }

    private fun hasCameraApp(): Boolean {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return intent.resolveActivity(packageManager) != null
    }

    private fun launchNativeCamera() {
        try {
            val photoFile = File(
                cacheDir,
                "photo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            )

            currentPhotoPath = photoFile.absolutePath

            photoUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }

            takePictureLauncher.launch(takePictureIntent)

        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startCameraX() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f
                updateZoomIndicator()
                updateFlashIcon()

            } catch (exc: Exception) {
                Log.e(TAG, "Error al iniciar CameraX", exc)
                Toast.makeText(this, "Error al iniciar la cámara", Toast.LENGTH_SHORT).show()
                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            cacheDir,
            "photo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Error al capturar foto: ${exc.message}", exc)
                    Toast.makeText(
                        baseContext,
                        "Error al capturar foto: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_IMAGE_PATH, photoFile.absolutePath)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        )
    }

    private fun toggleFlash() {
        camera?.let { cam ->
            isFlashOn = !isFlashOn
            cam.cameraControl.enableTorch(isFlashOn)

            updateFlashIcon()

            val message = if (isFlashOn) "Flash activado" else "Flash desactivado"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFlashIcon() {
        val iconRes = if (isFlashOn) {
            R.drawable.ic_flash_on
        } else {
            R.drawable.ic_flash_off
        }
        binding.btnToggleFlash.setImageResource(iconRes)
    }

    private fun setZoom(scaleFactor: Float) {
        camera?.let { cam ->
            val zoomState = cam.cameraInfo.zoomState.value
            val maxZoom = zoomState?.maxZoomRatio ?: 1.0f
            val minZoom = zoomState?.minZoomRatio ?: 1.0f
            currentZoomRatio *= scaleFactor
            currentZoomRatio = max(minZoom, min(currentZoomRatio, maxZoom))

            cam.cameraControl.setZoomRatio(currentZoomRatio)

            updateZoomIndicator()
        }
    }

    private fun updateZoomIndicator() {
        camera?.let { cam ->
            val zoomState = cam.cameraInfo.zoomState.value
            val maxZoom = zoomState?.maxZoomRatio ?: 1.0f
            val minZoom = zoomState?.minZoomRatio ?: 1.0f

            binding.tvZoomLevel.text = String.format("%.1fx", currentZoomRatio)

            val progress = ((currentZoomRatio - minZoom) / (maxZoom - minZoom) * 100).toInt()
            binding.zoomProgressBar.progress = progress
        }
    }

    private inner class PinchToZoomListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            setZoom(detector.scaleFactor)
            return true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (useCameraX) {
            // Apagar flash antes de cerrar
            camera?.cameraControl?.enableTorch(false)
            cameraExecutor.shutdown()
        }
    }
}