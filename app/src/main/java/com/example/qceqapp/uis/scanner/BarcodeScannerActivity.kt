package com.example.qceqapp.uis.scanner

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.qceqapp.R
import com.example.qceqapp.databinding.ActivityBarcodeScannerBinding
import com.example.qceqapp.utils.PermissionHelper
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeScannerBinding
    private var isTorchOn = false

    private var orderCode: String? = null
    private var orderNumber: String? = null
    private var scanType: String? = null

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initializeScanner()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("Camera permission is required to scan barcodes. Please grant the permission in settings.")
                .setPositiveButton("Exit") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderCode = intent.getStringExtra("ORDER_CODE")
        orderNumber = intent.getStringExtra("ORDER_NUMBER")
        scanType = intent.getStringExtra("TYPE")

        setupOrderInfo()
        setupButtons()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            PermissionHelper.hasCameraPermission(this) -> {
                initializeScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Camera Permission Required")
                    .setMessage("Camera permission is needed to scan barcodes. Please grant the permission to continue.")
                    .setPositiveButton("OK") { _, _ ->
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        finish()
                    }
                    .show()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupOrderInfo() {
        if (!orderCode.isNullOrEmpty()) {
            binding.orderInfoLayout.visibility = View.VISIBLE
            binding.tvOrderCodeScanner.text = "Order: $orderCode"

            if (!orderNumber.isNullOrEmpty()) {
                binding.tvOrderNumberScanner.text = "Code: $orderNumber"
            } else {
                binding.tvOrderNumberScanner.visibility = View.GONE
            }
        }
    }

    private fun setupButtons() {
        binding.btnToggleTorch.setOnClickListener {
            toggleTorch()
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun initializeScanner() {
        try {
            binding.barcodeView.decodeContinuous(object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult) {
                    handleBarcodeResult(result.text)
                }

                override fun possibleResultPoints(resultPoints: List<ResultPoint>) {
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing scanner: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleBarcodeResult(barcode: String) {
        binding.barcodeView.pause()

        val resultIntent = Intent().apply {
            putExtra("SCANNED_CODE", barcode)
            putExtra("ORDER_CODE", orderCode)
            putExtra("ORDER_NUMBER", orderNumber)
            putExtra("TYPE", scanType)
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun toggleTorch() {
        isTorchOn = !isTorchOn
        if (isTorchOn) {
            binding.barcodeView.setTorchOn()
            binding.btnToggleTorch.setImageResource(R.drawable.ic_flashlight_on)
        } else {
            binding.barcodeView.setTorchOff()
            binding.btnToggleTorch.setImageResource(R.drawable.ic_flashlight_off)
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionHelper.hasCameraPermission(this)) {
            binding.barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }
}