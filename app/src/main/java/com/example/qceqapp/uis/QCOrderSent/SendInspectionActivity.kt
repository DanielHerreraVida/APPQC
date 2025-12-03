package com.example.qceqapp.uis.QCOrderSent

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities.QCHistoryResponse
import com.example.qceqapp.data.network.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SendInspectionActivity : AppCompatActivity() {

    private lateinit var rvQCInspections: RecyclerView
    private lateinit var fabSend: FloatingActionButton
    private lateinit var adapter: SendInspectionsAdapter

    private val qcHistoryItems = mutableListOf<QCHistoryResponse>()
    private val service = Service()
    private val gson = Gson()

    private var progressDialog: Dialog? = null
    private var progressDialogVisible = false

    companion object {
        const val EXTRA_INSPECTIONS = "InspectionsS"
        const val EXTRA_INSPECTIONS_SENT = "InspectionsSent"
        const val REQUEST_CODE_QC_ORDER = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_inspections)
        rvQCInspections = findViewById(R.id.rvQCInspections)
        fabSend = findViewById(R.id.fabSend)
        val listSelectedInspections = intent.getStringExtra(EXTRA_INSPECTIONS) ?: ""

        if (listSelectedInspections.isNotEmpty()) {
            qcHistoryItems.clear()
            qcHistoryItems.addAll(deserializeQCHistoryItems(listSelectedInspections))
            adapter = SendInspectionsAdapter(qcHistoryItems) { item, position ->
                onItemClick(item, position)
            }
            rvQCInspections.apply {
                layoutManager = LinearLayoutManager(this@SendInspectionActivity)
                adapter = this@SendInspectionActivity.adapter
            }
            fabSend.setOnClickListener {
                sendInspections()
            }
        }
    }

    private fun sendInspections() {
        startProgressBar()

        val inspects = getInspectionsToSend()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = service.sendGroupInspection(inspects)

                withContext(Dispatchers.Main) {
                    val resultString = result?.toString() ?: ""

                    if (resultString.contains("success", ignoreCase = true)) {
                        Toast.makeText(
                            this@SendInspectionActivity,
                            "[${qcHistoryItems.size}] Inspections Sent",
                            Toast.LENGTH_LONG
                        ).show()

                        val intentBack = Intent().apply {
                            putExtra(EXTRA_INSPECTIONS_SENT, "OK")
                        }
                        setResult(Activity.RESULT_OK, intentBack)
                        cancelProgressBar()
                        finish()
                    } else {
                        cancelProgressBar()
                        Toast.makeText(
                            this@SendInspectionActivity,
                            "ERROR: $resultString",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    cancelProgressBar()
                    Toast.makeText(
                        this@SendInspectionActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun getInspectionsToSend(): String {
        val inspections = mutableListOf<String>()
        for (item in qcHistoryItems) {
            item.boxIdToInspect?.let { inspections.add(it) }
        }
        return inspections.joinToString(",")
    }

    private fun onItemClick(item: QCHistoryResponse, position: Int) {
        val qco = adapter.getQCHistoryItem(position)

        if (!qco.inspectionStatus.isNullOrEmpty()) {
            startProgressBar()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val barcodes = qco.boxId?.split(',') ?: emptyList()

                    if (barcodes.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            cancelProgressBar()
                            Toast.makeText(
                                this@SendInspectionActivity,
                                "Invalid barcode",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    val barcodeBase = barcodes[0]

                    val biModel = service.scanToInspect(barcodeBase)

                    withContext(Dispatchers.Main) {
                        biModel.onSuccess { model ->
                            val lstSelectedBoxes = model.lstSelectedBoxes

                            if (!lstSelectedBoxes.isNullOrEmpty() && lstSelectedBoxes.isNotEmpty()) {
                                val intent = Intent(
                                    this@SendInspectionActivity,
                                    QCOrderSentActivity::class.java
                                ).apply {
                                    putExtra("codeReaded", barcodeBase)
                                    putExtra("selectedBoxes", gson.toJson(lstSelectedBoxes))
                                    putExtra("infobx", qco.boxIdToInspect)
                                }
                                startActivityForResult(intent, REQUEST_CODE_QC_ORDER)
                                cancelProgressBar()
                            } else {
                                cancelProgressBar()
                                Toast.makeText(
                                    this@SendInspectionActivity,
                                    "Error trying to get Selected Boxes, please try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }.onFailure {
                            cancelProgressBar()
                            Toast.makeText(
                                this@SendInspectionActivity,
                                "Error trying to get Selected Boxes, please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        cancelProgressBar()
                        Toast.makeText(
                            this@SendInspectionActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun startProgressBar() {
        if (!progressDialogVisible) {
            progressDialog = Dialog(this).apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                window?.setLayout(200, 200)
                setContentView(R.layout.loading_layout)
                //findViewById<ProgressBar>(R.id.progressBar_Loading)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                show()
            }
            progressDialogVisible = true
        }
    }

    private fun cancelProgressBar() {
        if (progressDialogVisible) {
            val finishProgressDialog = Runnable {
                progressDialog?.let {
                    it.cancel()
                    it.dismiss()
                }
                progressDialogVisible = false
            }
            Handler(Looper.getMainLooper()).postDelayed(finishProgressDialog, 10)
        }
    }

    private fun deserializeQCHistoryItems(json: String): List<QCHistoryResponse> {
        return try {
            val type = object : TypeToken<List<QCHistoryResponse>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_QC_ORDER && resultCode == Activity.RESULT_OK) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelProgressBar()
    }
}