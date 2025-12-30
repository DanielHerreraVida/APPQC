package com.example.qceqapp.uis.QCInspection

import android.app.Dialog
import android.content.Context
import android.widget.*
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities

class QCActionsDialog(
    context: Context,
    private val actions: List<Entities.QCActionResponse>,
    private val preselected: List<Entities.QCActionResponse> = emptyList(),
    private val onSelectionConfirmed: (List<Entities.QCActionResponse>) -> Unit
) : Dialog(context) {

    private val selectedActions = mutableListOf<Entities.QCActionResponse>()
    private var filteredActions = actions.toMutableList()

    init {
        setContentView(R.layout.dialog_qc_actions)
        setCancelable(true)
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val listView = findViewById<ListView>(R.id.listActions)
        val searchView = findViewById<SearchView>(R.id.searchAction)
        val txtSelected = findViewById<TextView>(R.id.txtSelectedActions)
        val btnContinue = findViewById<Button>(R.id.btnContinueActions)
        val btnCancel = findViewById<Button>(R.id.btnCancelActions)
        val btnClear = findViewById<ImageButton>(R.id.btnClearActions)

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_multiple_choice,
            filteredActions.map { it.descriptionAes }
        )

        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.adapter = adapter

        preselected.forEach { selected ->
            val index = actions.indexOfFirst { it.idAction == selected.idAction }
            if (index >= 0) {
                listView.setItemChecked(index, true)
                selectedActions.add(actions[index])
            }
        }
        txtSelected.text = "Total: ${selectedActions.size}"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filteredActions.clear()
                if (newText.isNullOrBlank()) {
                    filteredActions.addAll(actions)
                } else {
                    filteredActions.addAll(
                        actions.filter {
                            it.descriptionAes.contains(newText, ignoreCase = true)
                        }
                    )
                }
                adapter.clear()
                adapter.addAll(filteredActions.map { it.descriptionAes })
                adapter.notifyDataSetChanged()
                for (i in filteredActions.indices) {
                    val item = filteredActions[i]
                    val isSelected = selectedActions.any { it.idAction == item.idAction }
                    listView.setItemChecked(i, isSelected)
                }

                return true
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = filteredActions[position]

            if (selectedActions.any { it.idAction == item.idAction }) {
                selectedActions.removeAll { it.idAction == item.idAction }
            } else {
                selectedActions.add(item)
            }
            txtSelected.text = "Total: ${selectedActions.size}"
        }

        btnContinue.setOnClickListener {
            onSelectionConfirmed(selectedActions.distinctBy { it.idAction })
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }

        btnClear.setOnClickListener {
            selectedActions.clear()
            listView.clearChoices()
            adapter.notifyDataSetChanged()
            txtSelected.text = "Total: 0"
        }
    }
}