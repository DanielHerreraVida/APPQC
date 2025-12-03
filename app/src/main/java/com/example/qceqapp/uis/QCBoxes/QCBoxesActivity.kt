package com.example.qceqapp.uis.QCBoxes

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.uis.QCInspection.QCInspectionActivity
import android.content.Intent
import com.example.qceqapp.uis.QCOrderSent.QCOrderSentActivity

class QCBoxesActivity : AppCompatActivity() {

    private val TAG = "QCBoxesActivity"
    private lateinit var viewModel: QCBoxesViewModel

    private lateinit var awbTv: TextView
    private lateinit var telexTv: TextView
    private lateinit var farmTv: TextView
    private lateinit var barcodeTv: TextView
    private lateinit var selectedTotalTv: TextView
    private lateinit var relatedTotalTv: TextView
    private lateinit var allTotalTv: TextView

    private lateinit var rvSelected: RecyclerView
    private lateinit var rvRelated: RecyclerView
    private lateinit var rvAll: RecyclerView

    private lateinit var cbSelectAllRelated: CheckBox
    private lateinit var btnCancel: Button
    private lateinit var btnContinue: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var selectedAdapter: QCBoxAdapter
    private lateinit var relatedAdapter: QCBoxAdapter
    private lateinit var allAdapter: QCBoxAdapter

    private val selectedBoxesList = mutableListOf<Entities.BoxToInspect>()
    private var codeReaded: String = ""
    private var fromActivityInfo: String? = null
    private var fromQCOrderSent: String? = null
    private var previouslySelectedBoxes: List<Entities.BoxToInspect>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qc_boxes)

        codeReaded = intent.getStringExtra("codeReaded") ?: ""

        if (codeReaded.isEmpty()) {
            Toast.makeText(this, "Invalid barcode", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        fromActivityInfo = intent.getStringExtra("fromActivityInfo")
        fromQCOrderSent = intent.getStringExtra("fromQCOrderSent")

        val listSFromInfo = intent.getStringExtra("selectedBXS")
        if (!listSFromInfo.isNullOrEmpty()) {
            previouslySelectedBoxes = deserializeBoxes(listSFromInfo)
        }

        initViews()
        setupRecyclerViews()
        setupViewModel()
        setupListeners()

        viewModel.loadBoxData(codeReaded, previouslySelectedBoxes)
    }

    private fun initViews() {
        awbTv = findViewById(R.id.AWBTV)
        telexTv = findViewById(R.id.TELEXTV)
        farmTv = findViewById(R.id.FARMTV)
        barcodeTv = findViewById(R.id.BarcodeTV)
        selectedTotalTv = findViewById(R.id.SelectedBoxesTotalTV)
        relatedTotalTv = findViewById(R.id.RelatedBoxesTotalTV)
        allTotalTv = findViewById(R.id.AllBoxesTotalTV)
        rvSelected = findViewById(R.id.rv_QCBoxesSelected)
        rvRelated = findViewById(R.id.rv_QCBoxesRelated)
        rvAll = findViewById(R.id.rv_QCBoxesAll)
        cbSelectAllRelated = findViewById(R.id.CB_SelectAllRelated)
        btnCancel = findViewById(R.id.btn_CancelBI)
        btnContinue = findViewById(R.id.btn_ContinueBI)
        progressBar = findViewById(R.id.progressBar)

        barcodeTv.text = codeReaded
        progressBar.visibility = View.VISIBLE
    }

    private fun setupRecyclerViews() {
        selectedAdapter = QCBoxAdapter(selectedBoxesList, null)
        rvSelected.layoutManager = LinearLayoutManager(this)
        rvSelected.adapter = selectedAdapter

        relatedAdapter = QCBoxAdapter(selectedBoxesList) { box -> onBoxClicked(box) }
        rvRelated.layoutManager = LinearLayoutManager(this)
        rvRelated.adapter = relatedAdapter

        allAdapter = QCBoxAdapter(selectedBoxesList) { box -> onBoxClicked(box) }
        rvAll.layoutManager = LinearLayoutManager(this)
        rvAll.adapter = allAdapter
    }

    private fun setupViewModel() {
        viewModel = QCBoxesViewModel()

        viewModel.onOrderLoaded = { order ->
            runOnUiThread {
                awbTv.text = order.bxAWB ?: "-"
                telexTv.text = order.bxTELEX ?: "-"
                farmTv.text = order.grower ?: "-"
                barcodeTv.text = order.boxId ?: codeReaded
            }
        }

        viewModel.onDataLoaded = {
            runOnUiThread { updateUI() }
        }

        viewModel.onError = { error ->
            runOnUiThread {
                progressBar.visibility = View.GONE
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        cbSelectAllRelated.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectAllRelated() else deselectAllRelated()
        }

        btnCancel.setOnClickListener { finish() }

        btnContinue.setOnClickListener {
            if (codeReaded.isEmpty()) {
                Toast.makeText(this, "Scan a box first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedBoxesList.isEmpty()) {
                Toast.makeText(this, "Please select at least one box", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE

            viewModel.getInspectIdByBox(codeReaded) { idToInspect ->
                runOnUiThread {
                    progressBar.visibility = View.GONE

                    if (!idToInspect.isNullOrEmpty()) {
                        val serializedBoxes = serializeBoxes(selectedBoxesList)

                        when {
                            fromActivityInfo == "true" -> {
                                val intent = Intent()
                                intent.putExtra("selectedBoxes", serializedBoxes)
                                setResult(RESULT_OK, intent)
                                Toast.makeText(this, "Boxes updated: ${selectedBoxesList.size}", Toast.LENGTH_SHORT).show()
                                finish()
                            }

                            fromQCOrderSent == "true" -> {
                                val intent = Intent()
                                intent.putExtra("selectedBoxes", serializedBoxes)
                                setResult(RESULT_OK, intent)
                                Toast.makeText(this, "Boxes updated: ${selectedBoxesList.size}", Toast.LENGTH_SHORT).show()
                                finish()
                            }

                            else -> {
                                val intent = Intent(this, QCInspectionActivity::class.java)
                                intent.putExtra("codeReaded", codeReaded)
                                intent.putExtra("idBox", codeReaded)
                                intent.putExtra("selectedBoxes", serializedBoxes)
                                intent.putExtra("reason", viewModel.orderHeader?.reason ?: "Unknown reason")
                                intent.putExtra("idToInspect", idToInspect)

                                startActivity(intent)
                                Toast.makeText(this, "Total Boxes: ${selectedBoxesList.size}", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error getting inspection ID", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun onBoxClicked(box: Entities.BoxToInspect) {
        val existing = selectedBoxesList.find { it.barcode == box.barcode }
        if (existing != null) {
            selectedBoxesList.remove(existing)
            Log.d(TAG, "Unselected box: ${box.barcode}")
        } else {
            selectedBoxesList.add(box)
            Log.d(TAG, "Selected box: ${box.barcode}")
        }

        notifyAdapters()
    }

    private fun selectAllRelated() {
        relatedAdapter.getItems().forEach { box ->
            if (selectedBoxesList.none { it.barcode == box.barcode }) {
                selectedBoxesList.add(box)
            }
        }
        notifyAdapters()
    }

    private fun deselectAllRelated() {
        val relatedBarcodes = relatedAdapter.getItems().map { it.barcode }
        selectedBoxesList.removeAll { relatedBarcodes.contains(it.barcode) }
        notifyAdapters()
    }

    private fun notifyAdapters() {
        selectedAdapter.notifyDataSetChanged()
        relatedAdapter.notifyDataSetChanged()
        allAdapter.notifyDataSetChanged()
        updateSelectedCount()
        updateSelectAllCheckbox()
    }

    private fun updateSelectAllCheckbox() {
        val relatedItems = relatedAdapter.getItems()
        cbSelectAllRelated.isChecked = relatedItems.isNotEmpty() &&
                relatedItems.all { related ->
                    selectedBoxesList.any { it.barcode == related.barcode }
                }
    }

    private fun updateSelectedCount() {
        selectedTotalTv.text = "Selected Boxes: ${selectedBoxesList.size}"
    }

    private fun updateUI() {
        progressBar.visibility = View.GONE
        if (selectedBoxesList.isEmpty()) {
            selectedBoxesList.clear()
            selectedBoxesList.addAll(viewModel.selectedBoxes)
        } else {
            Log.d(TAG, "Keeping selectedBoxesList current: ${selectedBoxesList.size}")
        }

        selectedTotalTv.text = "Selected Boxes: ${selectedBoxesList.size}"
        relatedTotalTv.text = "Related Boxes: ${viewModel.relatedBoxes.size}"
        allTotalTv.text = "All Boxes: ${viewModel.allBoxes.size}"

        selectedAdapter.setItems(selectedBoxesList.toList())
        relatedAdapter.setItems(viewModel.relatedBoxes)
        allAdapter.setItems(viewModel.allBoxes)

        updateSelectAllCheckbox()
    }

    private fun serializeBoxes(boxes: List<Entities.BoxToInspect>): String {
        return com.google.gson.Gson().toJson(boxes)
    }

    private fun deserializeBoxes(json: String): List<Entities.BoxToInspect> {
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<Entities.BoxToInspect>>() {}.type
            com.google.gson.Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun newIntent(context: android.content.Context, codeReaded: String) =
            android.content.Intent(context, QCBoxesActivity::class.java).apply {
                putExtra("codeReaded", codeReaded)
            }
    }
}