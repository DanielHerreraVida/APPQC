package com.example.qceqapp.uis.QCInspection

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.*
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities

class BoxCompositionDialog(
    context: Context,
    private val compositions: List<Entities.BoxCompositionResponse>,
    private val onCompositionSelected: ((Entities.BoxCompositionResponse) -> Unit)? = null
) : Dialog(context) {

    private lateinit var tvQtyBox: TextView
    private lateinit var lvBoxComposition: ListView
    private lateinit var btnClose: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSelectedComp: TextView
    private lateinit var llSelectedSection: LinearLayout

    private var selectedComposition: Entities.BoxCompositionResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_box_composition)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.9).toInt()
        )

        initViews()
        setupData()
        setupListeners()
    }

    private fun initViews() {
        tvQtyBox = findViewById(R.id.tvQtyBox)
        lvBoxComposition = findViewById(R.id.lvBoxComposition)
        btnClose = findViewById(R.id.btnClose)
        progressBar = findViewById(R.id.progressBar)

    }

    private fun setupData() {
        tvQtyBox.text = compositions.size.toString()

        val adapter = QCCompBxAdapter(context, compositions)
        lvBoxComposition.adapter = adapter

        if (compositions.isEmpty()) {
            Toast.makeText(
                context,
                "No compositions found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupListeners() {
        btnClose.setOnClickListener {
            dismiss()
        }

        lvBoxComposition.setOnItemClickListener { _, _, position, _ ->
            val composition = compositions[position]
            showCompositionDetails(composition)
        }

        lvBoxComposition.setOnItemLongClickListener { _, _, position, _ ->
            val composition = compositions[position]
            selectedComposition = composition
            onCompositionSelected?.invoke(composition)

            Toast.makeText(
                context,
                "Selected: ${composition.composition}",
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }


    private fun showCompositionDetails(composition: Entities.BoxCompositionResponse) {
        val details = buildString {
            appendLine("Box: ${composition.inbNumBox}")
            appendLine("Composition: ${composition.composition}")
            appendLine("🏷Code: ${composition.floCod}")
            appendLine("Grade: ${composition.floGrade}")

            if (!composition.issues.isNullOrEmpty()) {
                appendLine("\n Issues (${composition.issues.size}):")
                composition.issues.forEachIndexed { index, issue ->
                    appendLine("  ${index + 1}. $issue")
                }
            } else {
                appendLine("\n No issues")
            }
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("Composition Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }
}