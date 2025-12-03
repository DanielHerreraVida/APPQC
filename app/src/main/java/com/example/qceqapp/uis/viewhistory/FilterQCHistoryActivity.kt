package com.example.qceqapp.uis.viewhistory

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.databinding.ActivityFilterQcHistoryBinding
import com.example.qceqapp.databinding.DialogMultiSelectBinding
import com.example.qceqapp.uis.scanner.BarcodeScannerActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class FilterQCHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilterQcHistoryBinding
    private val filterDictionary = mutableMapOf<String, String>()
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val barcodes = mutableListOf<String>()
    private var authors: List<String> = emptyList()
    private var historyList: List<Entities.QCHistoryResponse> = emptyList()
    private var uniqueGrowers: List<String> = emptyList()
    private var uniqueCustomers: List<String> = emptyList()
    private var uniqueAwbs: List<String> = emptyList()
    private var uniqueOrderNums: List<String> = emptyList()
    private var isClearing = false
    private val selectedGrowers = mutableListOf<String>()
    private val selectedCustomers = mutableListOf<String>()
    private val selectedAwbs = mutableListOf<String>()
    private val selectedOrderNums = mutableListOf<String>()

    private val barcodeScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedCode = result.data?.getStringExtra("SCANNED_CODE")
            scannedCode?.let { code ->
                if (code.isNotBlank() && !barcodes.contains(code)) {
                    barcodes.add(code)
                    updateBarcodeUI()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilterQcHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Filter History"
            setDisplayHomeAsUpEnabled(true)
        }

        setupBackPressed()
        loadIntentData()
        extractUniqueValues()
        setupCheckboxes()
        setupSpinners()
        setupDatePickers()
        setupButtons()
        loadExistingFilters()
        binding.etSearch.post {
            binding.etSearch.requestFocus()
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        })
    }

    private fun loadIntentData() {
        try {
            intent.getStringExtra("Filters")?.let { filtersJson ->
                val type = object : TypeToken<Map<String, String>>() {}.type
                val filters = Gson().fromJson<Map<String, String>>(filtersJson, type)
                filterDictionary.putAll(filters)
            }
            intent.getStringExtra("authors")?.let { authorsJson ->
                val type = object : TypeToken<List<String>>() {}.type
                authors = Gson().fromJson(authorsJson, type) ?: emptyList()
            }

            intent.getStringExtra("historyList")?.let { historyJson ->
                val type = object : TypeToken<List<Entities.QCHistoryResponse>>() {}.type
                historyList = Gson().fromJson(historyJson, type) ?: emptyList()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading filter data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractUniqueValues() {
        uniqueGrowers = historyList
            .mapNotNull { it.grower }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        uniqueCustomers = historyList
            .mapNotNull { it.customerId }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        uniqueAwbs = historyList
            .mapNotNull { it.bxAWB }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        uniqueOrderNums = historyList
            .mapNotNull { it.orderNum }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    private fun setupCheckboxes() {
        binding.checkboxAuthorAll.setOnCheckedChangeListener { _, isChecked ->
            if (isClearing) return@setOnCheckedChangeListener
            if (isChecked) {
                binding.spinnerAuthor.setText("", false)
            }
            binding.spinnerAuthor.isEnabled = true
        }
        binding.radioGroupInspectionStatus.setOnCheckedChangeListener { _, checkedId ->
            // Si selecciona algún radio button, desmarcar "All"
            if (checkedId != -1) {
                binding.checkboxInspectionStatusAll.isChecked = false
            }
        }
        binding.checkboxGrowersAll.setOnCheckedChangeListener { _, isChecked ->
            if (isClearing) return@setOnCheckedChangeListener
            if (isChecked) {
                selectedGrowers.clear()
                binding.tvGrowerChoosed.text = ""
            }
            binding.btnGrowers.isEnabled = true
            binding.tvGrowerChoosed.isEnabled = true
        }

        binding.checkboxCustomersAll.setOnCheckedChangeListener { _, isChecked ->
            if (isClearing) return@setOnCheckedChangeListener
            if (isChecked) {
                selectedCustomers.clear()
                binding.tvCustomerChoosed.text = ""
            }
            binding.btnCustomers.isEnabled = true
            binding.tvCustomerChoosed.isEnabled = true
        }

        binding.checkboxAwbAll.setOnCheckedChangeListener { _, isChecked ->
            if (isClearing) return@setOnCheckedChangeListener
            if (isChecked) {
                selectedAwbs.clear()
                binding.tvAwbChoosed.text = ""
            }
            binding.btnAwb.isEnabled = true
            binding.tvAwbChoosed.isEnabled = true
        }

        binding.checkboxOrderNumAll.setOnCheckedChangeListener { _, isChecked ->
            if (isClearing) return@setOnCheckedChangeListener
            if (isChecked) {
                selectedOrderNums.clear()
                binding.tvOrderNumChoosed.text = ""
            }
            binding.btnOrderNum.isEnabled = true
            binding.tvOrderNumChoosed.isEnabled = true
        }

        binding.checkboxInspectionStatusAll.setOnCheckedChangeListener { _, isChecked ->
            if (isClearing) return@setOnCheckedChangeListener
            if (isChecked) {
                binding.radioGroupInspectionStatus.clearCheck()
            }
            binding.rbAccepted.isEnabled = true
            binding.rbRejected.isEnabled = true
        }

        binding.checkboxInspectionDateAll.setOnCheckedChangeListener { _, isChecked ->
            if (isClearing) return@setOnCheckedChangeListener
            if (isChecked) {
                binding.etFechaInicial.setText("MM/DD/YYYY")
                binding.etFechaFinal.setText("MM/DD/YYYY")
            }
            binding.etFechaInicial.isEnabled = true
            binding.etFechaFinal.isEnabled = true
        }
    }
    private fun setupSpinners() {
        val authorList = mutableListOf("")
        authorList.addAll(listOf("QA", "SOURCING"))
        val authorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, authorList)
        binding.spinnerAuthor.setAdapter(authorAdapter)

        binding.spinnerAuthor.setOnItemClickListener { _, _, position, _ ->
            if (position > 0) {
                binding.checkboxAuthorAll.isChecked = false
            } else {
                binding.checkboxAuthorAll.isChecked = true
            }
        }
    }
    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        binding.etFechaInicial.setOnClickListener {
            val currentDate = if (binding.etFechaInicial.text.toString() != "MM/DD/YYYY") {
                try {
                    dateFormat.parse(binding.etFechaInicial.text.toString())
                } catch (e: Exception) {
                    calendar.time
                }
            } else {
                calendar.time
            }

            currentDate?.let { calendar.time = it }

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val dateString = dateFormat.format(calendar.time)
                    binding.etFechaInicial.setText(dateString)
                    binding.checkboxInspectionDateAll.isChecked = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etFechaFinal.setOnClickListener {
            val currentDate = if (binding.etFechaFinal.text.toString() != "MM/DD/YYYY") {
                try {
                    dateFormat.parse(binding.etFechaFinal.text.toString())
                } catch (e: Exception) {
                    calendar.time
                }
            } else {
                calendar.time
            }

            currentDate?.let { calendar.time = it }

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val dateString = dateFormat.format(calendar.time)
                    binding.etFechaFinal.setText(dateString)
                    // ✅ Desmarcar "All" cuando se selecciona una fecha
                    binding.checkboxInspectionDateAll.isChecked = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

    }

    private fun setupButtons() {
        binding.btnSaveFilters.setOnClickListener {
            saveFilters()
        }

        binding.btnClearFilters.setOnClickListener {
            clearAllFilters()
        }

        // ✅ Growers: desmarcar "All" al abrir el diálogo
        binding.btnGrowers.setOnClickListener {
            if (uniqueGrowers.isEmpty()) {
                Toast.makeText(this, "No Grower data available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Desmarcar "All" automáticamente al abrir el diálogo
            if (binding.checkboxGrowersAll.isChecked) {
                binding.checkboxGrowersAll.isChecked = false
            }

            showMultiSelectDialog(
                title = "Select Growers",
                items = uniqueGrowers,
                selectedItems = selectedGrowers,
                onConfirm = { selected ->
                    selectedGrowers.clear()
                    selectedGrowers.addAll(selected)
                    binding.tvGrowerChoosed.text = if (selected.isEmpty()) {
                        ""
                    } else {
                        "${selected.size} Grower(s)"
                    }
                    // ✅ Si no hay selecciones, marcar "All"
                    binding.checkboxGrowersAll.isChecked = selected.isEmpty()
                }
            )
        }

        // ✅ Customers: desmarcar "All" al abrir el diálogo
        binding.btnCustomers.setOnClickListener {
            if (uniqueCustomers.isEmpty()) {
                Toast.makeText(this, "No Customer data available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Desmarcar "All" automáticamente al abrir el diálogo
            if (binding.checkboxCustomersAll.isChecked) {
                binding.checkboxCustomersAll.isChecked = false
            }

            showMultiSelectDialog(
                title = "Select Customers",
                items = uniqueCustomers,
                selectedItems = selectedCustomers,
                onConfirm = { selected ->
                    selectedCustomers.clear()
                    selectedCustomers.addAll(selected)
                    binding.tvCustomerChoosed.text = if (selected.isEmpty()) {
                        ""
                    } else {
                        "${selected.size} Customer(s)"
                    }
                    // ✅ Si no hay selecciones, marcar "All"
                    binding.checkboxCustomersAll.isChecked = selected.isEmpty()
                }
            )
        }

        // ✅ AWB: desmarcar "All" al abrir el diálogo
        binding.btnAwb.setOnClickListener {
            if (uniqueAwbs.isEmpty()) {
                Toast.makeText(this, "No AWB data available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Desmarcar "All" automáticamente al abrir el diálogo
            if (binding.checkboxAwbAll.isChecked) {
                binding.checkboxAwbAll.isChecked = false
            }

            showMultiSelectDialog(
                title = "Select AWB",
                items = uniqueAwbs,
                selectedItems = selectedAwbs,
                onConfirm = { selected ->
                    selectedAwbs.clear()
                    selectedAwbs.addAll(selected)
                    binding.tvAwbChoosed.text = if (selected.isEmpty()) {
                        ""
                    } else {
                        "${selected.size} AWB(s)"
                    }
                    // ✅ Si no hay selecciones, marcar "All"
                    binding.checkboxAwbAll.isChecked = selected.isEmpty()
                }
            )
        }

        // ✅ Order Numbers: desmarcar "All" al abrir el diálogo
        binding.btnOrderNum.setOnClickListener {
            if (uniqueOrderNums.isEmpty()) {
                Toast.makeText(this, "No Order Number data available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Desmarcar "All" automáticamente al abrir el diálogo
            if (binding.checkboxOrderNumAll.isChecked) {
                binding.checkboxOrderNumAll.isChecked = false
            }

            showMultiSelectDialog(
                title = "Select Order Numbers",
                items = uniqueOrderNums,
                selectedItems = selectedOrderNums,
                onConfirm = { selected ->
                    selectedOrderNums.clear()
                    selectedOrderNums.addAll(selected)
                    binding.tvOrderNumChoosed.text = if (selected.isEmpty()) {
                        ""
                    } else {
                        "${selected.size} Order(s)"
                    }
                    // ✅ Si no hay selecciones, marcar "All"
                    binding.checkboxOrderNumAll.isChecked = selected.isEmpty()
                }
            )
        }

        binding.btnScanBarcode.setOnClickListener {
            openBarcodeScanner()
        }

        binding.btnClearBarcodes.setOnClickListener {
            barcodes.clear()
            updateBarcodeUI()
            binding.etSearch.postDelayed({
                binding.etSearch.requestFocus()
            }, 100)
        }

        binding.etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                keyCode == android.view.KeyEvent.KEYCODE_ENTER) {

                val code = binding.etSearch.text.toString().trim()
                if (code.isNotBlank()) {
                    if (!barcodes.contains(code)) {
                        barcodes.add(code)
                        updateBarcodeUI()
                    }
                    binding.etSearch.setText("")

                    binding.etSearch.postDelayed({
                        binding.etSearch.requestFocus()
                        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                                as android.view.inputmethod.InputMethodManager
                        imm.showSoftInput(binding.etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    }, 100)
                }
                true
            } else {
                false
            }
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {

                val code = binding.etSearch.text.toString().trim()
                if (code.isNotBlank()) {
                    if (!barcodes.contains(code)) {
                        barcodes.add(code)
                        updateBarcodeUI()
                    }
                    binding.etSearch.setText("")
                    binding.etSearch.requestFocus()
                }
                true
            } else false
        }
    }
    private fun openBarcodeScanner() {
        val intent = Intent(this, BarcodeScannerActivity::class.java).apply {
            putExtra("ORDER_CODE", "History Filter Scan")
            putExtra("ORDER_NUMBER", "")
            putExtra("TYPE", "filter")
        }
        barcodeScannerLauncher.launch(intent)
    }

    private fun updateBarcodeUI() {
        binding.tvBarcodesCount.text = if (barcodes.isEmpty()) {
            "No barcodes scanned"
        } else {
            "${barcodes.size} barcode(s) scanned"
        }

        binding.tvBarcodesList.text = if (barcodes.isEmpty()) {
            ""
        } else {
            barcodes.joinToString(", ")
        }
    }

    private fun showMultiSelectDialog(
        title: String,
        items: List<String>,
        selectedItems: MutableList<String>,
        onConfirm: (List<String>) -> Unit
    ) {
        val dialog = Dialog(this)
        val dialogBinding = DialogMultiSelectBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        dialogBinding.tvDialogTitle.text = title

        val tempSelectedItems = selectedItems.toMutableList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        dialogBinding.listView.adapter = adapter

        fun updateSelectedText() {
            dialogBinding.tvListOfItems.text = if (tempSelectedItems.isEmpty()) {
                "No items selected"
            } else {
                tempSelectedItems.joinToString(", ") { "[$it]" }
            }
        }
        updateSelectedText()

        dialogBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return true
            }
        })

        dialogBinding.listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position) ?: return@setOnItemClickListener

            if (tempSelectedItems.contains(item)) {
                tempSelectedItems.remove(item)
            } else {
                tempSelectedItems.add(item)
            }
            updateSelectedText()
        }

        dialogBinding.btnDelete.setOnClickListener {
            tempSelectedItems.clear()
            updateSelectedText()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnContinue.setOnClickListener {
            onConfirm(tempSelectedItems.toList())
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun clearAllFilters() {
        isClearing = true

        filterDictionary.clear()
        selectedGrowers.clear()
        selectedCustomers.clear()
        selectedAwbs.clear()
        selectedOrderNums.clear()
        barcodes.clear()

        filterDictionary["Author"] = ""
        filterDictionary["Grower"] = ""
        filterDictionary["Customer"] = ""
        filterDictionary["AWB"] = ""
        filterDictionary["OrderNum"] = ""
        filterDictionary["InspectionState"] = ""
        filterDictionary["Fechai"] = ""
        filterDictionary["Fechaf"] = ""
        filterDictionary["Barcodes"] = ""

        binding.checkboxAuthorAll.isChecked = true
        binding.spinnerAuthor.setText("", false)
        binding.spinnerAuthor.isEnabled = true

        binding.checkboxGrowersAll.isChecked = true
        binding.tvGrowerChoosed.text = ""
        binding.btnGrowers.isEnabled = true

        binding.checkboxCustomersAll.isChecked = true
        binding.tvCustomerChoosed.text = ""
        binding.btnCustomers.isEnabled = true

        binding.checkboxAwbAll.isChecked = true
        binding.tvAwbChoosed.text = ""
        binding.btnAwb.isEnabled = true

        binding.checkboxOrderNumAll.isChecked = true
        binding.tvOrderNumChoosed.text = ""
        binding.btnOrderNum.isEnabled = true

        binding.checkboxInspectionStatusAll.isChecked = true
        binding.radioGroupInspectionStatus.clearCheck()
        binding.rbAccepted.isEnabled = true
        binding.rbRejected.isEnabled = true

        binding.checkboxInspectionDateAll.isChecked = true
        binding.etFechaInicial.setText("MM/DD/YYYY")
        binding.etFechaFinal.setText("MM/DD/YYYY")
        binding.etFechaInicial.isEnabled = true
        binding.etFechaFinal.isEnabled = true

        updateBarcodeUI()

        isClearing = false

        val filtersJson = Gson().toJson(filterDictionary)
        val resultIntent = Intent().apply {
            putExtra("Filters", filtersJson)
        }
        setResult(Activity.RESULT_OK, resultIntent)

        Toast.makeText(this, "All filters cleared", Toast.LENGTH_SHORT).show()
        finish()
    }
    private fun loadExistingFilters() {
        try {
            val authorValue = filterDictionary["Author"] ?: ""
            if (authorValue.isEmpty()) {
                binding.checkboxAuthorAll.isChecked = true
            } else {
                val displayValue = if (authorValue == "S") "SOURCING" else authorValue
                binding.spinnerAuthor.setText(displayValue, false)
            }

            val growerValue = filterDictionary["Grower"] ?: ""
            if (growerValue.isEmpty()) {
                binding.checkboxGrowersAll.isChecked = true
            } else {
                selectedGrowers.clear()
                selectedGrowers.addAll(deserializeFilterStringList(growerValue))
                binding.tvGrowerChoosed.text = "${selectedGrowers.size} Grower(s)"
            }

            val customerValue = filterDictionary["Customer"] ?: ""
            if (customerValue.isEmpty()) {
                binding.checkboxCustomersAll.isChecked = true
            } else {
                selectedCustomers.clear()
                selectedCustomers.addAll(deserializeFilterStringList(customerValue))
                binding.tvCustomerChoosed.text = "${selectedCustomers.size} Customer(s)"
            }

            val awbValue = filterDictionary["AWB"] ?: ""
            if (awbValue.isEmpty()) {
                binding.checkboxAwbAll.isChecked = true
            } else {
                selectedAwbs.clear()
                selectedAwbs.addAll(deserializeFilterStringList(awbValue))
                binding.tvAwbChoosed.text = "${selectedAwbs.size} AWB(s)"
            }

            val orderNumValue = filterDictionary["OrderNum"] ?: ""
            if (orderNumValue.isEmpty()) {
                binding.checkboxOrderNumAll.isChecked = true
            } else {
                selectedOrderNums.clear()
                selectedOrderNums.addAll(deserializeFilterStringList(orderNumValue))
                binding.tvOrderNumChoosed.text = "${selectedOrderNums.size} Order(s)"
            }

            val inspectionStatus = filterDictionary["InspectionState"] ?: ""
            when (inspectionStatus) {
                "1" -> binding.rbAccepted.isChecked = true
                "0" -> binding.rbRejected.isChecked = true
                else -> binding.checkboxInspectionStatusAll.isChecked = true
            }

            val fechaI = filterDictionary["Fechai"] ?: ""
            val fechaF = filterDictionary["Fechaf"] ?: ""

            if (fechaI.isEmpty() && fechaF.isEmpty()) {
                binding.checkboxInspectionDateAll.isChecked = true
            } else {
                if (fechaI.isNotEmpty()) binding.etFechaInicial.setText(fechaI)
                if (fechaF.isNotEmpty()) binding.etFechaFinal.setText(fechaF)
            }

            val barcodesValue = filterDictionary["Barcodes"] ?: ""
            if (barcodesValue.isNotEmpty()) {
                barcodes.clear()
                barcodes.addAll(barcodesValue.split(",").filter { it.isNotBlank() })
                updateBarcodeUI()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading existing filters: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFilters() {
        try {

            val authorValue = if (binding.checkboxAuthorAll.isChecked) {
                ""
            } else {
                val selected = binding.spinnerAuthor.text.toString()
                when (selected) {
                    "SOURCING" -> "S"
                    else -> selected
                }
            }
            val growerValues = if (binding.checkboxGrowersAll.isChecked) {
                ""
            } else {
                serializeFilterStringList(selectedGrowers)
            }

            val customerValues = if (binding.checkboxCustomersAll.isChecked) {
                ""
            } else {
                serializeFilterStringList(selectedCustomers)
            }

            val awbValues = if (binding.checkboxAwbAll.isChecked) {
                ""
            } else {
                serializeFilterStringList(selectedAwbs)
            }

            val orderNumValues = if (binding.checkboxOrderNumAll.isChecked) {
                ""
            } else {
                serializeFilterStringList(selectedOrderNums)
            }

            val inspectionStatus = if (binding.checkboxInspectionStatusAll.isChecked) {
                ""
            } else {
                when {
                    binding.rbAccepted.isChecked -> "1"
                    binding.rbRejected.isChecked -> "0"
                    else -> ""
                }
            }

            val fechaI = if (binding.checkboxInspectionDateAll.isChecked ||
                binding.etFechaInicial.text.toString() == "MM/DD/YYYY") {
                ""
            } else {
                binding.etFechaInicial.text.toString()
            }

            val fechaF = if (binding.checkboxInspectionDateAll.isChecked ||
                binding.etFechaFinal.text.toString() == "MM/DD/YYYY") {
                ""
            } else {
                binding.etFechaFinal.text.toString()
            }

            val barcodesValue = barcodes.joinToString(",")

            filterDictionary["Author"] = authorValue
            filterDictionary["Grower"] = growerValues
            filterDictionary["Customer"] = customerValues
            filterDictionary["AWB"] = awbValues
            filterDictionary["OrderNum"] = orderNumValues
            filterDictionary["InspectionState"] = inspectionStatus
            filterDictionary["Fechai"] = fechaI
            filterDictionary["Fechaf"] = fechaF
            filterDictionary["Barcodes"] = barcodesValue

            val filtersJson = Gson().toJson(filterDictionary)
            val resultIntent = Intent().apply {
                putExtra("Filters", filtersJson)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving filters: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun serializeFilterStringList(list: List<String>): String {
        return Gson().toJson(list)
    }

    private fun deserializeFilterStringList(json: String): List<String> {
        if (json.isEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        setResult(Activity.RESULT_CANCELED)
        finish()
        return true
    }
}