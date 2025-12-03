package com.example.qceqapp.uis.toinspect

import android.app.AlertDialog
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.qceqapp.R
import com.example.qceqapp.uis.scanner.BarcodeScannerActivity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.collectLatest
import com.example.qceqapp.uis.QCBoxes.QCBoxesActivity
class ScanOrder : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnOpenCamera: Button
    private val viewModel: ScanOrderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_scan)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = "Scan Box"
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        etSearch = findViewById(R.id.etSearch)
        btnOpenCamera = findViewById(R.id.btnOpenCamera)
        etSearch.requestFocus()

        val orderCode = intent.getStringExtra("ORDER_CODE") ?: ""

        lifecycleScope.launchWhenStarted {
            viewModel.error.collectLatest { msg ->
                msg?.let {
                    showErrorDialog(it)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.success.collectLatest { orderInfo ->
                orderInfo?.let {
                    openQCBoxesActivity(it)
                }
            }
        }

        etSearch.setOnEditorActionListener { v, actionId, event ->
            val isEnterKey = event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN
            val isImeDone = actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND

            if (isEnterKey || isImeDone) {
                val typedCode = etSearch.text.toString().trim()
                if (typedCode.isNotEmpty()) {
                    sendManualScan(typedCode)

                    val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                } else {
                    Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }



        btnOpenCamera.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java).apply {
                putExtra("ORDER_CODE", orderCode)
                putExtra("TYPE", "order")
            }
            startActivityForResult(intent, 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            val scannedCode = data?.getStringExtra("SCANNED_CODE") ?: ""
            etSearch.setText(scannedCode)
            sendManualScan(scannedCode)
        }
    }

    private fun sendManualScan(code: String) {
        Toast.makeText(this, "Sending code: $code", Toast.LENGTH_SHORT).show()
        viewModel.processScan(code)
    }

    private fun openQCBoxesActivity(orderInfo: String) {
        val intent = QCBoxesActivity.newIntent(this, codeReaded = orderInfo)
        startActivity(intent)
        finish()
    }


    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
