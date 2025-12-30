package com.example.qceqapp.uis.scanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import com.example.qceqapp.databinding.ActivityCameraVideoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraVideoBinding
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var isRecording = false

    companion object {
        private const val TAG = "CameraVideoActivity"
        const val EXTRA_VIDEO_PATH = "video_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.btnCaptureVideo.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.btnClose.setOnClickListener {
            if (isRecording) {
                stopRecording()
            }
            finish()
        }

        binding.btnToggleFlash.setOnClickListener {
            toggleFlash()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startRecording() {
        val videoCapture = this.videoCapture ?: return

        val videoFile = File(
            cacheDir,
            "video_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        )

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        updateRecordingUI(true)
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        updateRecordingUI(false)

                        if (!recordEvent.hasError()) {
                            val resultIntent = Intent().apply {
                                putExtra(EXTRA_VIDEO_PATH, videoFile.absolutePath)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        } else {
                            val errorMsg = "Video recording error: ${recordEvent.error}"
                            Log.e(TAG, errorMsg)
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                            videoFile.delete()
                        }
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    private fun updateRecordingUI(recording: Boolean) {
        if (recording) {
            binding.btnCaptureVideo.text = "⏹ Stop"
            binding.btnCaptureVideo.setBackgroundColor(getColor(android.R.color.holo_red_dark))
            binding.recordingIndicator.visibility = android.view.View.VISIBLE
            binding.btnClose.isEnabled = false
            binding.btnToggleFlash.isEnabled = false
        } else {
            binding.btnCaptureVideo.text = "⏺ Record"
            binding.btnCaptureVideo.setBackgroundColor(getColor(android.R.color.holo_red_light))
            binding.recordingIndicator.visibility = android.view.View.GONE
            binding.btnClose.isEnabled = true
            binding.btnToggleFlash.isEnabled = true
        }
    }

    private fun toggleFlash() {
        camera?.let {
            val currentMode = it.cameraInfo.torchState.value ?: TorchState.OFF
            val newMode = currentMode == TorchState.OFF
            it.cameraControl.enableTorch(newMode)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording()
        }
        cameraExecutor.shutdown()
    }
}