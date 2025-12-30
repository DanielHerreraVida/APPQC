package com.example.qceqapp.uis.toinspect

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.databinding.ActivityFiltersBinding
import com.example.qceqapp.databinding.DialogMultiSelectionBinding
import com.example.qceqapp.uis.scanner.BarcodeScannerActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class FiltersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFiltersBinding
    private var currentFilter = Entities.FilterData()
    private var selectedGrowers = mutableListOf<String>()
    private var selectedCustomers = mutableListOf<String>()
    private var scannedBarcodes = mutableListOf<String>()

    private var allGrowers: List<Entities.QCGrowerResponse> = emptyList()
    private var allCustomers: List<Entities.QCCustomerResponse> = emptyList()
    private var allAuthors: List<String> = emptyList()
    private var allBoxIds: List<String> = emptyList()

    private val barcodeScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedCode = result.data?.getStringExtra("SCANNED_CODE")
            scannedCode?.let { code ->
                if (code.isNotBlank() && !scannedBarcodes.contains(code)) {
                    scannedBarcodes.add(code)
                    updateBarcodesText()
                    updateFilterCount()
                }
            }
        }
    }

    companion object {
        const val EXTRA_FILTER_DATA = "extra_filter_data"
        const val EXTRA_GROWERS = "extra_growers"
        const val EXTRA_CUSTOMERS = "extra_customers"
        const val EXTRA_AUTHORS = "extra_authors"
        const val EXTRA_BOX_IDS = "extra_box_ids"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFiltersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadExtras()
        setupUI()
        loadCurrentFilters()
        setupClickListeners()
        updateFilterCount()
        binding.etSearch.post {
            binding.etSearch.requestFocus()
        }
    }

    private fun loadExtras() {
        intent.getParcelableExtra<Entities.FilterData>(EXTRA_FILTER_DATA)?.let {
            currentFilter = it
        }

        intent.getParcelableArrayListExtra<Entities.QCGrowerResponse>(EXTRA_GROWERS)?.let {
            allGrowers = it
        }

        intent.getParcelableArrayListExtra<Entities.QCCustomerResponse>(EXTRA_CUSTOMERS)?.let {
            allCustomers = it
        }

        intent.getStringArrayListExtra(EXTRA_AUTHORS)?.let {
            allAuthors = it
        }

        intent.getStringArrayListExtra(EXTRA_BOX_IDS)?.let {
            allBoxIds = it.distinct().sorted()
        }
    }

    private fun setupUI() {
        setupAuthorSpinner()
        setupCheckboxListeners()

        binding.spinnerAuthor.isEnabled = true
        binding.btnGrowers.isEnabled = true
        binding.tvGrowerSelected.isEnabled = true
        binding.btnCustomers.isEnabled = true
        binding.tvCustomerSelected.isEnabled = true
        enableRadioGroup(binding.radioGroupSaved, true)
    }

    private fun setupAuthorSpinner() {
        val authors = if (allAuthors.isNotEmpty()) {
            listOf("") + allAuthors
        } else {
            listOf("", "QA", "S")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, authors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAuthor.adapter = adapter
    }

    private fun setupCheckboxListeners() {
        binding.cbAuthorAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.spinnerAuthor.setSelection(0)
            }
            binding.spinnerAuthor.isEnabled = true
            updateFilterCount()
        }

        binding.cbGrowersAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedGrowers.clear()
                updateGrowerText()
            }
            binding.btnGrowers.isEnabled = true
            binding.tvGrowerSelected.isEnabled = true
            updateFilterCount()
        }

        binding.cbCustomersAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedCustomers.clear()
                updateCustomerText()
            }
            binding.btnCustomers.isEnabled = true
            binding.tvCustomerSelected.isEnabled = true
            updateFilterCount()
        }

        binding.cbSavedAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.radioGroupSaved.clearCheck()
            }
            enableRadioGroup(binding.radioGroupSaved, true)
            updateFilterCount()
        }
    }

    private fun enableRadioGroup(radioGroup: android.widget.RadioGroup, enabled: Boolean) {
        for (i in 0 until radioGroup.childCount) {
            radioGroup.getChildAt(i).isEnabled = enabled
        }
    }

    private fun loadCurrentFilters() {
        if (!currentFilter.isEmpty()) {
            applyCurrentFiltersToUI()
        }
    }

    private fun applyCurrentFiltersToUI() {
        if (currentFilter.author.isNotEmpty()) {
            binding.cbAuthorAll.isChecked = false

            val adapter = binding.spinnerAuthor.adapter
            var position = 0
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i).toString() == currentFilter.author) {
                    position = i
                    break
                }
            }
            binding.spinnerAuthor.setSelection(position)
        }

        if (currentFilter.grower.isNotEmpty()) {
            binding.cbGrowersAll.isChecked = false
            selectedGrowers = currentFilter.grower.split(",").toMutableList()
            updateGrowerText()
        }

        if (currentFilter.customer.isNotEmpty()) {
            binding.cbCustomersAll.isChecked = false
            selectedCustomers = currentFilter.customer.split(",").toMutableList()
            updateCustomerText()
        }

        if (currentFilter.saved.isNotEmpty()) {
            binding.cbSavedAll.isChecked = false
            when (currentFilter.saved) {
                "1" -> binding.rbSavedYes.isChecked = true
                "0" -> binding.rbSavedNo.isChecked = true
            }
        }

        if (currentFilter.barcodes.isNotEmpty()) {
            scannedBarcodes = currentFilter.barcodes.split(",").toMutableList()
            updateBarcodesText()
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveFilters.setOnClickListener { saveFilters() }
        binding.btnClearFilters.setOnClickListener { clearFilters() }
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnGrowers.setOnClickListener {
            if (binding.cbGrowersAll.isChecked) {
                binding.cbGrowersAll.isChecked = false
            }
            showGrowersDialog()
        }

        binding.btnSelectBarcodes.setOnClickListener {
            showBoxIdsDialog()
        }

        binding.etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                keyCode == android.view.KeyEvent.KEYCODE_ENTER) {

                val code = binding.etSearch.text.toString().trim()
                if (code.isNotBlank()) {
                    if (!scannedBarcodes.contains(code)) {
                        scannedBarcodes.add(code)
                        updateBarcodesText()
                        updateFilterCount()
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
                    if (!scannedBarcodes.contains(code)) {
                        scannedBarcodes.add(code)
                        updateBarcodesText()
                        updateFilterCount()
                    }
                    binding.etSearch.setText("")
                    binding.etSearch.requestFocus()
                }
                true
            } else false
        }

        binding.btnCustomers.setOnClickListener {
            if (binding.cbCustomersAll.isChecked) {
                binding.cbCustomersAll.isChecked = false
            }
            showCustomersDialog()
        }

        binding.btnScanBarcode.setOnClickListener { openBarcodeScanner() }
        binding.btnClearBarcodes.setOnClickListener {
            scannedBarcodes.clear()
            updateBarcodesText()
            updateFilterCount()
            binding.etSearch.postDelayed({
                binding.etSearch.requestFocus()
            }, 100)
        }

        binding.spinnerAuthor.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            var isFirstSelection = true

            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (isFirstSelection) {
                    isFirstSelection = false
                    return
                }
                if (position > 0) {
                    binding.cbAuthorAll.isChecked = false
                } else {
                    binding.cbAuthorAll.isChecked = true
                }
                updateFilterCount()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        binding.radioGroupSaved.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                binding.cbSavedAll.isChecked = false
            }
            updateFilterCount()
        }
    }

    private fun openBarcodeScanner() {
        val intent = Intent(this, BarcodeScannerActivity::class.java).apply {
            putExtra("ORDER_CODE", "Filter Scan")
            putExtra("ORDER_NUMBER", "")
            putExtra("TYPE", "filter")
        }
        barcodeScannerLauncher.launch(intent)
    }

    private fun showBoxIdsDialog() {
        if (allBoxIds.isEmpty()) {
            android.widget.Toast.makeText(this, "No Box IDs available", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogMultiSelectionBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        setupMultiSelectionDialog(
            dialogBinding,
            allBoxIds,
            allBoxIds,
            scannedBarcodes,
            "Box IDs"
        ) { updatedList ->
            scannedBarcodes.clear()
            scannedBarcodes.addAll(updatedList)
            updateBarcodesText()
            updateFilterCount()
            dialog.dismiss()
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDialogClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showGrowersDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogMultiSelectionBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val growerNames = allGrowers.map { "${it.groCod} - ${it.proVendor}" }
        val growerCodes = allGrowers.map { it.groCod ?: "" }

        setupMultiSelectionDialog(
            dialogBinding,
            growerNames,
            growerCodes,
            selectedGrowers,
            "Growers"
        ) { updatedList ->
            selectedGrowers = updatedList.toMutableList()
            updateGrowerText()
            updateFilterCount()
            binding.cbGrowersAll.isChecked = selectedGrowers.isEmpty()
            dialog.dismiss()
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDialogClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCustomersDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogMultiSelectionBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val customerNames = allCustomers.map { "${it.codCustomer} - ${it.custCompany}" }
        val customerCodes = allCustomers.map { it.codCustomer ?: "" }

        setupMultiSelectionDialog(
            dialogBinding,
            customerNames,
            customerCodes,
            selectedCustomers,
            "Customers"
        ) { updatedList ->
            selectedCustomers = updatedList.toMutableList()
            updateCustomerText()
            updateFilterCount()
            binding.cbCustomersAll.isChecked = selectedCustomers.isEmpty()

            dialog.dismiss()
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDialogClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupMultiSelectionDialog(
        dialogBinding: DialogMultiSelectionBinding,
        displayItems: List<String>,
        itemCodes: List<String>,
        selectedItems: List<String>,
        title: String,
        onSelectionChanged: (List<String>) -> Unit
    ) {
        dialogBinding.tvDialogTitle.text = "Select $title"

        val originalDisplayItems = displayItems.toList()
        val originalItemCodes = itemCodes.toList()
        val currentlySelected = selectedItems.toMutableSet()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, displayItems.toMutableList())
        dialogBinding.listViewItems.adapter = adapter
        dialogBinding.listViewItems.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        fun updateCheckStates() {
            for (i in 0 until dialogBinding.listViewItems.count) {
                val item = adapter.getItem(i)
                val originalIndex = originalDisplayItems.indexOf(item)
                if (originalIndex >= 0) {
                    val code = originalItemCodes[originalIndex]
                    dialogBinding.listViewItems.setItemChecked(i, currentlySelected.contains(code))
                }
            }
        }

        updateCheckStates()
        updateSelectedCount(dialogBinding, dialogBinding.listViewItems)

        dialogBinding.listViewItems.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position)
            val originalIndex = originalDisplayItems.indexOf(item)

            if (originalIndex >= 0) {
                val code = originalItemCodes[originalIndex]

                if (dialogBinding.listViewItems.isItemChecked(position)) {
                    currentlySelected.add(code)
                } else {
                    currentlySelected.remove(code)
                }

                updateSelectedCount(dialogBinding, dialogBinding.listViewItems)
            }
        }

        dialogBinding.etDialogSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)

                dialogBinding.btnClearSearch.visibility = if (s.isNullOrEmpty()) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }

                dialogBinding.listViewItems.post {
                    updateCheckStates()
                    updateSelectedCount(dialogBinding, dialogBinding.listViewItems)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialogBinding.btnClearSearch.setOnClickListener {
            dialogBinding.etDialogSearch.setText("")
        }

        dialogBinding.btnDialogClear.setOnClickListener {
            currentlySelected.clear()
            updateCheckStates()
            updateSelectedCount(dialogBinding, dialogBinding.listViewItems)
        }

        dialogBinding.btnDialogApply.setOnClickListener {
            onSelectionChanged(currentlySelected.toList())
        }
    }

    private fun updateSelectedCount(binding: DialogMultiSelectionBinding, listView: ListView) {
        val count = (0 until listView.count).count { listView.isItemChecked(it) }
        binding.tvSelectedCount.text = "$count selected"
    }

    private fun updateGrowerText() {
        binding.tvGrowerSelected.text = if (selectedGrowers.isEmpty()) {
            "No growers selected"
        } else {
            "${selectedGrowers.size} grower(s) selected"
        }
    }

    private fun updateCustomerText() {
        binding.tvCustomerSelected.text = if (selectedCustomers.isEmpty()) {
            "No customers selected"
        } else {
            "${selectedCustomers.size} customer(s) selected"
        }
    }

    private fun updateBarcodesText() {
        binding.tvBarcodesCount.text = if (scannedBarcodes.isEmpty()) {
            "No barcodes scanned"
        } else {
            "${scannedBarcodes.size} barcode(s) scanned"
        }

        binding.tvBarcodesList.text = if (scannedBarcodes.isEmpty()) {
            ""
        } else {
            scannedBarcodes.joinToString(", ")
        }
    }

    private fun updateFilterCount() {
        var activeFilters = 0

        if (!binding.cbAuthorAll.isChecked && binding.spinnerAuthor.selectedItemPosition > 0) {
            activeFilters++
        }

        if (!binding.cbGrowersAll.isChecked && selectedGrowers.isNotEmpty()) {
            activeFilters++
        }

        if (!binding.cbCustomersAll.isChecked && selectedCustomers.isNotEmpty()) {
            activeFilters++
        }

        if (scannedBarcodes.isNotEmpty()) {
            activeFilters++
        }

        if (!binding.cbSavedAll.isChecked && binding.radioGroupSaved.checkedRadioButtonId != -1) {
            activeFilters++
        }

        binding.tvFilterCount.text = "$activeFilters active"
    }

    private fun saveFilters() {
        currentFilter = Entities.FilterData(
            author = if (binding.cbAuthorAll.isChecked || binding.spinnerAuthor.selectedItemPosition == 0) {
                ""
            } else {
                binding.spinnerAuthor.selectedItem?.toString() ?: ""
            },
            grower = if (binding.cbGrowersAll.isChecked) {
                ""
            } else {
                selectedGrowers.joinToString(",")
            },
            customer = if (binding.cbCustomersAll.isChecked) {
                ""
            } else {
                selectedCustomers.joinToString(",")
            },
            saved = if (binding.cbSavedAll.isChecked) {
                ""
            } else {
                when {
                    binding.rbSavedYes.isChecked -> "1"
                    binding.rbSavedNo.isChecked -> "0"
                    else -> ""
                }
            },
            awb = "",
            num = "",
            barcodes = scannedBarcodes.joinToString(",")
        )

        val resultIntent = Intent().apply {
            putExtra(EXTRA_FILTER_DATA, currentFilter)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun clearFilters() {
        binding.spinnerAuthor.setSelection(0)
        binding.cbAuthorAll.isChecked = true
        binding.cbGrowersAll.isChecked = true
        binding.cbCustomersAll.isChecked = true
        binding.cbSavedAll.isChecked = true
        binding.radioGroupSaved.clearCheck()

        selectedGrowers.clear()
        selectedCustomers.clear()
        scannedBarcodes.clear()

        updateGrowerText()
        updateCustomerText()
        updateBarcodesText()
        updateFilterCount()

        val resultIntent = Intent().apply {
            putExtra(EXTRA_FILTER_DATA, Entities.FilterData())
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}