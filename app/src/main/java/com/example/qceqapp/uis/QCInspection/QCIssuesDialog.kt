package com.example.qceqapp.uis.QCInspection

import android.app.Dialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities

class QCIssuesDialog(
    context: Context,
    private val issues: List<Entities.QCIssueResponse>,
    private val preselected: List<Entities.QCIssueResponse> = emptyList(),
    private val onSelectionConfirmed: (List<Entities.QCIssueResponse>) -> Unit
) : Dialog(context) {

    private val selectedIssues = mutableListOf<Entities.QCIssueResponse>()
    private lateinit var adapter: ArrayAdapter<String>
    private var filteredIssues = issues.toMutableList()
    private var currentDisplayedIssues = issues.toMutableList()

    init {
        setContentView(R.layout.dialog_qc_issues)
        setCancelable(true)
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val spinnerCategories = findViewById<Spinner>(R.id.spinnerCategories)
        val etSearchIssue = findViewById<EditText>(R.id.etSearchIssue)
        val listView = findViewById<ListView>(R.id.listIssues)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnContinue = findViewById<Button>(R.id.btnContinue)
        val btnClearAll = findViewById<ImageButton>(R.id.btnClearAll)
        val txtSelected = findViewById<TextView>(R.id.txtSelectedIssues)

        val categories = issues.map { it.categoryI }.distinct()
        val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, listOf("ALL") + categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = categoryAdapter

        selectedIssues.addAll(preselected)

        adapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_multiple_choice,
            currentDisplayedIssues.map { "${it.descriptionIes}" }
        )
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.adapter = adapter

        restoreSelection(listView)
        txtSelected.text = "Total: ${selectedIssues.size}"

        spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedCategory = spinnerCategories.selectedItem.toString()
                filteredIssues = if (selectedCategory == "ALL") {
                    issues.toMutableList()
                } else {
                    issues.filter { it.categoryI == selectedCategory }.toMutableList()
                }

                val searchQuery = etSearchIssue.text.toString().trim().lowercase()
                currentDisplayedIssues = if (searchQuery.isEmpty()) {
                    filteredIssues.toMutableList()
                } else {
                    filteredIssues.filter {
                        it.descriptionIes.lowercase().contains(searchQuery) ||
                                it.categoryI.lowercase().contains(searchQuery)
                    }.toMutableList()
                }

                updateListView(listView)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etSearchIssue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""

                currentDisplayedIssues = if (query.isEmpty()) {
                    filteredIssues.toMutableList()
                } else {
                    filteredIssues.filter {
                        it.descriptionIes.lowercase().contains(query) ||
                                it.categoryI.lowercase().contains(query)
                    }.toMutableList()
                }

                updateListView(listView)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = currentDisplayedIssues[position]
            if (selectedIssues.any { it.idIssue == item.idIssue }) {
                selectedIssues.removeAll { it.idIssue == item.idIssue }
            } else {
                selectedIssues.add(item)
            }
            txtSelected.text = "Total: ${selectedIssues.size}"
        }

        btnContinue.setOnClickListener {
            onSelectionConfirmed(selectedIssues.distinctBy { it.idIssue })
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }

        btnClearAll.setOnClickListener {
            selectedIssues.clear()
            listView.clearChoices()
            adapter.notifyDataSetChanged()
            txtSelected.text = "Total: 0"
            Toast.makeText(context, "Selection cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateListView(listView: ListView) {
        adapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_multiple_choice,
            currentDisplayedIssues.map { "${it.descriptionIes}" }
        )
        listView.adapter = adapter
        restoreSelection(listView)
    }

    private fun restoreSelection(listView: ListView) {
        for ((index, issue) in currentDisplayedIssues.withIndex()) {
            if (selectedIssues.any { it.idIssue == issue.idIssue }) {
                listView.setItemChecked(index, true)
            }
        }
    }
}