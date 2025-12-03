package com.example.qceqapp.uis.torelease

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.qceqapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class ReleaseFilterDialog(
    private val context: Context,
    private val allUsers: List<String>,
    private val currentFilters: FilterOptions,
    private val onApplyFilters: (FilterOptions) -> Unit,
    private val onScanRequested: () -> Unit
) {

    private var selectedUsers = currentFilters.selectedUsers.toMutableSet()
    private var startDate: Date? = currentFilters.startDate
    private var endDate: Date? = currentFilters.endDate
    private var scannedBoxes = currentFilters.scannedBoxes.toMutableList()
    private var dialog: Dialog? = null

    private lateinit var cbDateAll: CheckBox
    private lateinit var cbUsersAll: CheckBox
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

    private val userCheckBoxes = mutableListOf<CheckBox>()

    fun show() {
        val dialogView = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_release_filters, null)

        dialog = Dialog(context, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen).apply {
            setContentView(dialogView)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }

        initializeViews(dialogView)
        setupDateSection()
        setupUsersSection()
        setupBoxSection()
        setupBottomButtons()
        updateFilterCount()

        // Solicitar foco en el EditText de búsqueda de boxes
        etBoxSearch.postDelayed({
            etBoxSearch.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(etBoxSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 300)

        dialog?.show()
    }

    fun addScannedBox(boxId: String) {
        val trimmedBox = boxId.trim()
        if (trimmedBox.isNotEmpty() && !scannedBoxes.contains(trimmedBox)) {
            scannedBoxes.add(trimmedBox)
            updateBoxesText()
            updateFilterCount()
            etBoxSearch.setText("")
        }
    }

    private fun initializeViews(view: android.view.View) {
        tvFilterCount = view.findViewById(R.id.tvFilterCount)
        cbDateAll = view.findViewById(R.id.cbDateAll)
        cbUsersAll = view.findViewById(R.id.cbUsersAll)
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
    }

    private fun setupDateSection() {
        // Configurar checkbox "All" para fechas
        cbDateAll.setOnCheckedChangeListener { _, isChecked ->
            btnSelectStartDate.isEnabled = !isChecked
            btnSelectEndDate.isEnabled = !isChecked
            btnClearDates.isEnabled = !isChecked
            tvStartDate.isEnabled = !isChecked
            tvEndDate.isEnabled = !isChecked

            if (isChecked) {
                startDate = null
                endDate = null
                updateDateDisplay(tvStartDate, null, "Start Date: ")
                updateDateDisplay(tvEndDate, null, "End Date: ")
            }
            updateFilterCount()
        }

        // Configurar fechas actuales
        updateDateDisplay(tvStartDate, startDate, "Start Date: ")
        updateDateDisplay(tvEndDate, endDate, "End Date: ")

        // Si hay fechas seleccionadas, desmarcar "All"
        if (startDate != null || endDate != null) {
            cbDateAll.isChecked = false
        }

        // Botón para seleccionar fecha de inicio
        btnSelectStartDate.setOnClickListener {
            // Auto-desmarcar "All" cuando se selecciona una fecha
            cbDateAll.isChecked = false

            showDatePicker { date ->
                startDate = date
                updateDateDisplay(tvStartDate, startDate, "Start Date: ")
                updateFilterCount()
            }
        }

        // Botón para seleccionar fecha de fin
        btnSelectEndDate.setOnClickListener {
            // Auto-desmarcar "All" cuando se selecciona una fecha
            cbDateAll.isChecked = false

            showDatePicker { date ->
                endDate = date
                updateDateDisplay(tvEndDate, endDate, "End Date: ")
                updateFilterCount()
            }
        }

        // Botón para limpiar fechas
        btnClearDates.setOnClickListener {
            startDate = null
            endDate = null
            updateDateDisplay(tvStartDate, null, "Start Date: ")
            updateDateDisplay(tvEndDate, null, "End Date: ")
            cbDateAll.isChecked = true
        }
    }

    private fun setupUsersSection() {
        // Configurar checkbox "All" para usuarios
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

        // Crear checkboxes para usuarios
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
                        // Auto-desmarcar "All" cuando se selecciona un usuario
                        cbUsersAll.isChecked = false
                    } else {
                        selectedUsers.remove(user)
                        // Si se deseleccionan todos, marcar "All"
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

        // Si hay usuarios seleccionados, desmarcar "All"
        if (selectedUsers.isNotEmpty()) {
            cbUsersAll.isChecked = false
        }

        updateUsersSelectedText()
    }

    private fun setupBoxSection() {
        // Configurar EditText para detectar Enter
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

        // Botón Scan
        dialog?.findViewById<MaterialButton>(R.id.btnScanBox)?.setOnClickListener {
            onScanRequested()
        }

        // Botón Clear Boxes
        dialog?.findViewById<MaterialButton>(R.id.btnClearBoxes)?.setOnClickListener {
            scannedBoxes.clear()
            updateBoxesText()
            updateFilterCount()
            etBoxSearch.requestFocus()
        }

        // Actualizar display inicial
        updateBoxesText()
    }

    private fun setupBottomButtons() {
        // Botón Clear
        dialog?.findViewById<MaterialButton>(R.id.btnClearFilters)?.setOnClickListener {
            clearAllFilters()
        }

        // Botón Cancel
        dialog?.findViewById<MaterialButton>(R.id.btnCancel)?.setOnClickListener {
            dialog?.dismiss()
        }

        // Botón Apply
        dialog?.findViewById<MaterialButton>(R.id.btnApplyFilters)?.setOnClickListener {
            val filters = FilterOptions(
                selectedUsers = selectedUsers.toSet(),
                startDate = startDate,
                endDate = endDate,
                scannedBoxes = scannedBoxes.toList()
            )
            onApplyFilters(filters)
            dialog?.dismiss()
        }
    }

    private fun clearAllFilters() {
        // Limpiar fechas
        startDate = null
        endDate = null
        updateDateDisplay(tvStartDate, null, "Start Date: ")
        updateDateDisplay(tvEndDate, null, "End Date: ")
        cbDateAll.isChecked = true

        // Limpiar usuarios
        selectedUsers.clear()
        userCheckBoxes.forEach { it.isChecked = false }
        cbUsersAll.isChecked = true
        updateUsersSelectedText()

        // Limpiar boxes
        scannedBoxes.clear()
        etBoxSearch.setText("")
        updateBoxesText()

        updateFilterCount()
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
            scannedBoxes.isEmpty() -> "No boxes scanned"
            scannedBoxes.size == 1 -> "1 box scanned"
            else -> "${scannedBoxes.size} boxes scanned"
        }

        tvBoxesList.text = if (scannedBoxes.isEmpty()) {
            ""
        } else {
            scannedBoxes.joinToString(", ")
        }
    }

    private fun updateFilterCount() {
        var count = 0

        if (!cbDateAll.isChecked && (startDate != null || endDate != null)) {
            count++
        }

        if (!cbUsersAll.isChecked && selectedUsers.isNotEmpty()) {
            count++
        }

        if (scannedBoxes.isNotEmpty()) {
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