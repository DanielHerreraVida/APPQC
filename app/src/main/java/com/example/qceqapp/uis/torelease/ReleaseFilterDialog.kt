package com.example.qceqapp.uis.torelease

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.view.isVisible
import com.example.qceqapp.R
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class ReleaseFilterDialog(
    private val context: Context,
    private val allUsers: List<String>,
    private val allBoxes: List<String>,
    private val currentFilters: FilterOptions,
    private val isPendingView: Boolean,
    private val onApplyFilters: (FilterOptions) -> Unit,
    private val onScanRequested: () -> Unit
) {

    private var selectedUsers = currentFilters.selectedUsers.toMutableSet()
    private var startDate: Date? = currentFilters.startDate
    private var endDate: Date? = currentFilters.endDate
    private var selectedBoxes = currentFilters.scannedBoxes.toMutableSet()
    private var dialog: Dialog? = null

    private lateinit var cbDateAll: CheckBox
    private lateinit var cbUsersAll: CheckBox
    private lateinit var cbBoxesAll: CheckBox
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var tvUsersSelected: TextView
    private lateinit var tvFilterCount: TextView
    private lateinit var btnSelectStartDate: MaterialButton
    private lateinit var btnSelectEndDate: MaterialButton
    private lateinit var btnClearDates: MaterialButton
    private lateinit var usersContainer: LinearLayout
    private lateinit var etBoxSearch: EditText
    private lateinit var tvBoxesCount: TextView
    private lateinit var tvBoxesList: TextView
    private lateinit var spinnerBoxSelector: Spinner
    private lateinit var btnScanBox: MaterialButton

    private lateinit var llDateSection: View
    private lateinit var llUsersSection: View
    private val userCheckBoxes = mutableListOf<CheckBox>()

    fun show() {
        val dialogView = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_release_filters, null)

        dialog = Dialog(context, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen).apply {
            setContentView(dialogView)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }

        initializeViews(dialogView)
        configureSectionsVisibility()

        if (!isPendingView) {
            setupDateSection()
            setupUsersSection()
        }
        setupBoxSection()
        setupBottomButtons()
        updateFilterCount()

        etBoxSearch.postDelayed({
            etBoxSearch.requestFocus()
        }, 100)

        dialog?.show()
    }

    fun addScannedBox(boxId: String) {
        val trimmedBox = boxId.trim()
        if (trimmedBox.isNotEmpty() && !selectedBoxes.contains(trimmedBox)) {
            selectedBoxes.add(trimmedBox)
            cbBoxesAll.isChecked = false
            updateBoxesText()
            updateFilterCount()
            etBoxSearch.setText("")

            etBoxSearch.postDelayed({
                etBoxSearch.requestFocus()
            }, 100)
        }
    }

    private fun initializeViews(view: View) {
        tvFilterCount = view.findViewById(R.id.tvFilterCount)

        llDateSection = view.findViewById(R.id.llDateSection)
        llUsersSection = view.findViewById(R.id.llUsersSection)

        cbDateAll = view.findViewById(R.id.cbDateAll)
        cbUsersAll = view.findViewById(R.id.cbUsersAll)
        cbBoxesAll = view.findViewById(R.id.cbBoxesAll)
        tvStartDate = view.findViewById(R.id.tvStartDate)
        tvEndDate = view.findViewById(R.id.tvEndDate)
        tvUsersSelected = view.findViewById(R.id.tvUsersSelected)
        btnSelectStartDate = view.findViewById(R.id.btnSelectStartDate)
        btnSelectEndDate = view.findViewById(R.id.btnSelectEndDate)
        btnClearDates = view.findViewById(R.id.btnClearDates)
        usersContainer = view.findViewById(R.id.usersContainer)
        etBoxSearch = view.findViewById(R.id.etBoxSearch)
        tvBoxesCount = view.findViewById(R.id.tvBoxesCount)
        tvBoxesList = view.findViewById(R.id.tvBoxesList)
        spinnerBoxSelector = view.findViewById(R.id.spinnerBoxSelector)
        btnScanBox = view.findViewById(R.id.btnScanBox)
    }

    private fun configureSectionsVisibility() {
        if (isPendingView) {
            llDateSection.isVisible = false
            llUsersSection.isVisible = false
        } else {
            llDateSection.isVisible = true
            llUsersSection.isVisible = true
        }
    }


    private fun setupDateSection() {
        updateDateDisplay(tvStartDate, startDate, "Start Date: ")
        updateDateDisplay(tvEndDate, endDate, "End Date: ")

        cbDateAll.isChecked = startDate == null && endDate == null
        updateDateButtonsAppearance()

        cbDateAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startDate = null
                endDate = null
                updateDateDisplay(tvStartDate, null, "Start Date: ")
                updateDateDisplay(tvEndDate, null, "End Date: ")
            }
            updateDateButtonsAppearance()
            updateFilterCount()
        }

        btnSelectStartDate.setOnClickListener {
            if (cbDateAll.isChecked) {
                cbDateAll.isChecked = false
            }

            showDatePicker { date ->
                startDate = date
                updateDateDisplay(tvStartDate, startDate, "Start Date: ")
                updateFilterCount()
            }
        }

        btnSelectEndDate.setOnClickListener {
            if (cbDateAll.isChecked) {
                cbDateAll.isChecked = false
            }

            showDatePicker { date ->
                endDate = date
                updateDateDisplay(tvEndDate, endDate, "End Date: ")
                updateFilterCount()
            }
        }

        btnClearDates.setOnClickListener {
            startDate = null
            endDate = null
            updateDateDisplay(tvStartDate, null, "Start Date: ")
            updateDateDisplay(tvEndDate, null, "End Date: ")
            cbDateAll.isChecked = true
            updateFilterCount()
        }
    }

    private fun updateDateButtonsAppearance() {
        val isAllChecked = cbDateAll.isChecked
        btnSelectStartDate.alpha = if (isAllChecked) 0.5f else 1.0f
        btnSelectEndDate.alpha = if (isAllChecked) 0.5f else 1.0f
        btnClearDates.alpha = if (isAllChecked) 0.5f else 1.0f
    }

    private fun setupUsersSection() {
        cbUsersAll.setOnCheckedChangeListener { _, isChecked ->
            userCheckBoxes.forEach { it.isEnabled = !isChecked }
            tvUsersSelected.isEnabled = !isChecked

            if (isChecked) {
                selectedUsers.clear()
                userCheckBoxes.forEach { it.isChecked = false }
                updateUsersSelectedText()
            }
            updateFilterCount()
        }

        userCheckBoxes.clear()
        allUsers.sorted().forEach { user ->
            val checkBox = CheckBox(context).apply {
                text = user
                isChecked = selectedUsers.contains(user)
                textSize = 14f
                setTextColor(context.getColor(android.R.color.black))
                setPadding(12, 12, 12, 12)

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedUsers.add(user)
                        cbUsersAll.isChecked = false
                    } else {
                        selectedUsers.remove(user)
                        if (selectedUsers.isEmpty()) {
                            cbUsersAll.isChecked = true
                        }
                    }
                    updateUsersSelectedText()
                    updateFilterCount()
                }
            }
            userCheckBoxes.add(checkBox)
            usersContainer.addView(checkBox)
        }

        if (selectedUsers.isNotEmpty()) {
            cbUsersAll.isChecked = false
        }

        updateUsersSelectedText()
    }

    private fun setupBoxSection() {
        cbBoxesAll.isChecked = selectedBoxes.isEmpty()
        updateBoxControlsAppearance()

        cbBoxesAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedBoxes.clear()
                spinnerBoxSelector.setSelection(0)
                etBoxSearch.setText("")
                updateBoxesText()
            }
            updateBoxControlsAppearance()
            updateFilterCount()

            if (!isChecked) {
                etBoxSearch.postDelayed({
                    etBoxSearch.requestFocus()
                }, 100)
            }
        }

        val boxOptions = mutableListOf("-- Select a box --").apply {
            addAll(allBoxes.sorted())
        }

        val spinnerAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            boxOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerBoxSelector.adapter = spinnerAdapter

        spinnerBoxSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    if (cbBoxesAll.isChecked) {
                        cbBoxesAll.isChecked = false
                    }

                    val selectedBox = boxOptions[position]
                    if (!selectedBoxes.contains(selectedBox)) {
                        selectedBoxes.add(selectedBox)
                        updateBoxesText()
                        updateFilterCount()
                        spinnerBoxSelector.setSelection(0)

                        etBoxSearch.postDelayed({
                            etBoxSearch.requestFocus()
                        }, 100)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etBoxSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && cbBoxesAll.isChecked) {
                cbBoxesAll.isChecked = false
            }
        }

        etBoxSearch.setOnEditorActionListener { v, actionId, event ->
            val isEnterPressed = event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.action == android.view.KeyEvent.ACTION_DOWN
            val isDoneAction = actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_GO

            if (isEnterPressed || isDoneAction) {
                val boxId = v.text.toString().trim()
                if (boxId.isNotEmpty()) {
                    addScannedBox(boxId)
                    return@setOnEditorActionListener true
                }
            }
            false
        }

        btnScanBox.setOnClickListener {
            if (cbBoxesAll.isChecked) {
                cbBoxesAll.isChecked = false
            }
            onScanRequested()
        }

        dialog?.findViewById<MaterialButton>(R.id.btnClearBoxes)?.setOnClickListener {
            selectedBoxes.clear()
            spinnerBoxSelector.setSelection(0)
            etBoxSearch.setText("")
            cbBoxesAll.isChecked = true
            updateBoxesText()
            updateFilterCount()
        }

        updateBoxesText()
    }

    private fun updateBoxControlsAppearance() {
        val isAllChecked = cbBoxesAll.isChecked
        spinnerBoxSelector.alpha = if (isAllChecked) 0.5f else 1.0f
        etBoxSearch.alpha = if (isAllChecked) 0.5f else 1.0f
        btnScanBox.alpha = if (isAllChecked) 0.5f else 1.0f
    }

    private fun setupBottomButtons() {
        dialog?.findViewById<MaterialButton>(R.id.btnClearFilters)?.setOnClickListener {
            clearAllFilters()
        }

        dialog?.findViewById<MaterialButton>(R.id.btnCancel)?.setOnClickListener {
            dialog?.dismiss()
        }

        dialog?.findViewById<MaterialButton>(R.id.btnApplyFilters)?.setOnClickListener {
            val filters = FilterOptions(
                selectedUsers = if (isPendingView) emptySet() else selectedUsers.toSet(),
                startDate = if (isPendingView) null else startDate,
                endDate = if (isPendingView) null else endDate,
                scannedBoxes = selectedBoxes.toList()
            )
            onApplyFilters(filters)
            dialog?.dismiss()
        }
    }

    private fun clearAllFilters() {
        if (!isPendingView) {
            startDate = null
            endDate = null
            updateDateDisplay(tvStartDate, null, "Start Date: ")
            updateDateDisplay(tvEndDate, null, "End Date: ")
            cbDateAll.isChecked = true

            selectedUsers.clear()
            userCheckBoxes.forEach { it.isChecked = false }
            cbUsersAll.isChecked = true
            updateUsersSelectedText()
        }

        selectedBoxes.clear()
        spinnerBoxSelector.setSelection(0)
        etBoxSearch.setText("")
        cbBoxesAll.isChecked = true
        updateBoxesText()

        updateFilterCount()

        etBoxSearch.postDelayed({
            etBoxSearch.requestFocus()
        }, 100)
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay(textView: TextView, date: Date?, prefix: String) {
        textView.text = if (date != null) {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            "$prefix${format.format(date)}"
        } else {
            "${prefix}Not selected"
        }
    }

    private fun updateUsersSelectedText() {
        tvUsersSelected.text = when {
            selectedUsers.isEmpty() -> "No users selected"
            selectedUsers.size == 1 -> "1 user selected"
            else -> "${selectedUsers.size} users selected"
        }
    }

    private fun updateBoxesText() {
        tvBoxesCount.text = when {
            selectedBoxes.isEmpty() -> "No boxes selected"
            selectedBoxes.size == 1 -> "1 box selected"
            else -> "${selectedBoxes.size} boxes selected"
        }

        tvBoxesList.text = if (selectedBoxes.isEmpty()) {
            ""
        } else {
            selectedBoxes.sorted().joinToString(", ")
        }
    }

    private fun updateFilterCount() {
        var count = 0

        if (!isPendingView) {
            if (!cbDateAll.isChecked && (startDate != null || endDate != null)) {
                count++
            }

            if (!cbUsersAll.isChecked && selectedUsers.isNotEmpty()) {
                count++
            }
        }

        if (!cbBoxesAll.isChecked && selectedBoxes.isNotEmpty()) {
            count++
        }

        tvFilterCount.text = when (count) {
            0 -> "0 active"
            1 -> "1 active"
            else -> "$count active"
        }
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    data class FilterOptions(
        val selectedUsers: Set<String> = emptySet(),
        val startDate: Date? = null,
        val endDate: Date? = null,
        val scannedBoxes: List<String> = emptyList()
    ) {
        fun hasActiveFilters(): Boolean {
            return selectedUsers.isNotEmpty() || startDate != null || endDate != null || scannedBoxes.isNotEmpty()
        }
    }
}