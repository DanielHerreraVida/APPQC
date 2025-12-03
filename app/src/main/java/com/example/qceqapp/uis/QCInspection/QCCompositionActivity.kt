package com.example.qceqapp.uis.QCInspection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QCCompositionActivity : AppCompatActivity() {
    private val TAG = "QCCompositionActivity"
    private lateinit var tvQtyBox: TextView
    private lateinit var tvSelectedBox: TextView
    private lateinit var tvIssueChoosed: TextView
    private lateinit var lvBoxes: ListView
    private lateinit var btnIssues: ImageButton
    private lateinit var btnCancel: Button
    private lateinit var btnContinue: Button
    private lateinit var progressBar: ProgressBar
    private var compositionBoxes: MutableList<Entities.BoxCompositionResponse> = mutableListOf()
    private var actualComposition: Entities.BoxCompositionResponse? = null
    private var qcIssues: List<Entities.QCIssueResponse> = emptyList()
    private var selectedIssues: MutableList<Entities.QCIssueResponse> = mutableListOf()
    private var fromActivityInfo: String = ""
    private lateinit var adapter: QCCompBxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qc_composition)
        initViews()
        loadIssues()
        loadIntentData()
        setupListeners()
    }

    private fun initViews() {
        tvQtyBox = findViewById(R.id.tvQtyBox)
        tvSelectedBox = findViewById(R.id.tvSelectedComp)
        tvIssueChoosed = findViewById(R.id.tvIssueChoosed)
        lvBoxes = findViewById(R.id.lvBoxComposition)
        btnIssues = findViewById(R.id.btnIssues)
        btnCancel = findViewById(R.id.btnClose)
        btnContinue = findViewById(R.id.btnContinue)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun loadIntentData() {
        fromActivityInfo = intent.getStringExtra("fromActivityInfo") ?: ""
        val compositionsJson = intent.getStringExtra("selectedBXS") ?: ""

        if (compositionsJson.isNotEmpty()) {
            try {
                val type = object : TypeToken<List<Entities.BoxCompositionResponse>>() {}.type
                compositionBoxes = Gson().fromJson(compositionsJson, type)

                val uniqueBoxes = compositionBoxes.map { it.inbNumBox }.distinct().size
                tvQtyBox.text = uniqueBoxes.toString()

                updateCompositionsList()
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading compositions", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "No compositions data received", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupListeners() {
        lvBoxes.setOnItemClickListener { _, _, position, _ ->
            val item = compositionBoxes[position]
            saveCurrentCompositionIssues()
            selectComposition(item)
        }

        lvBoxes.setOnItemLongClickListener { _, _, position, _ ->
            val item = compositionBoxes[position]
            saveCurrentCompositionIssues()
            selectComposition(item)
            Toast.makeText(this, "Selected: ${item.composition}", Toast.LENGTH_SHORT).show()
            true
        }

        btnIssues.setOnClickListener {
            openIssuesDialog()
        }
        btnContinue.setOnClickListener {
            saveCurrentCompositionIssues()
            updateAllCompositionsToServer()
        }
        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun loadIssues() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = com.example.qceqapp.data.network.Service()
                val result = service.getQCIssues()

                withContext(Dispatchers.Main) {
                    result.getOrNull()?.let { issues ->
                        qcIssues = issues
                        progressBar.visibility = View.GONE
                        if (compositionBoxes.isNotEmpty()) {
                            selectComposition(compositionBoxes[0])
                        }
                    } ?: run {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@QCCompositionActivity,
                            "Failed to load issues",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@QCCompositionActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun selectComposition(composition: Entities.BoxCompositionResponse) {
        actualComposition = composition
        tvSelectedBox.text = "Selected Comp: ${composition.inbNumBox} - ${composition.composition}"
        selectedIssues = getIssuesForComposition(composition)
        updateIssuesDisplay()
        updateCompositionsList()
    }

    private fun openIssuesDialog() {
        if (actualComposition == null) {
            Toast.makeText(this, "Please select a composition first", Toast.LENGTH_SHORT).show()
            return
        }

        if (qcIssues.isEmpty()) {
            Toast.makeText(this, "Loading issues, please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        QCIssuesDialog(
            this@QCCompositionActivity,
            qcIssues,
            selectedIssues
        ) { newSelectedIssues ->
            selectedIssues = newSelectedIssues.toMutableList()
            updateIssuesDisplay()
            saveCurrentCompositionIssues()
            updateCompositionsList()
        }.show()
    }
    private fun updateCompositionsList() {
        adapter = QCCompBxAdapter(this, compositionBoxes, actualComposition)
        lvBoxes.adapter = adapter
        adapter.notifyDataSetChanged()
    }
    private fun saveCurrentCompositionIssues() {
        actualComposition?.let { current ->
            val index = compositionBoxes.indexOfFirst {
                it.inbNumBox == current.inbNumBox &&
                        it.floCod == current.floCod
            }

            if (index != -1) {
                compositionBoxes[index].issues = selectedIssues.map { it.idIssue }
            }
        }
    }
    private fun updateAllCompositionsToServer() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = com.example.qceqapp.data.network.Service()
                var successCount = 0
                var failCount = 0
                val compositionsWithIssues = compositionBoxes.filter {
                    !it.issues.isNullOrEmpty()
                }

                for (composition in compositionsWithIssues) {
                    try {
                        val result = service.updateCompositionIssues(
                            idBox = composition.inbNumBox,
                            floGrade = composition.floGrade,
                            floCod = composition.floCod,
                            issues = composition.issues ?: emptyList()
                        )

                        result.getOrNull()?.let { response ->
                            if (response.success) {
                                successCount++
                            } else {
                                failCount++
                            }
                        } ?: run {
                            failCount++
                        }
                    } catch (e: Exception) {
                        failCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

//                    val message = when {
//                        failCount == 0 -> "All compositions updated successfully ($successCount)"
//                        successCount == 0 -> "Failed to update compositions"
//                        else -> "Updated $successCount, failed $failCount compositions"
//                    }

//                    Toast.makeText(
//                        this@QCCompositionActivity,
//                        message,
//                        Toast.LENGTH_LONG
//                    ).show()
                    returnResultAndFinish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@QCCompositionActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getIssuesForComposition(composition: Entities.BoxCompositionResponse): MutableList<Entities.QCIssueResponse> {
        val issues = mutableListOf<Entities.QCIssueResponse>()

        composition.issues?.forEach { issueId ->
            val foundIssue = qcIssues.find { it.idIssue == issueId }
            if (foundIssue != null) {
                issues.add(foundIssue)
            } else {
            }
        }

        return issues
    }

    private fun updateIssuesDisplay() {
        tvIssueChoosed.text = if (selectedIssues.isNotEmpty()) {
            selectedIssues.joinToString(", ") { it.descriptionIen }
        } else {
            "No issues selected..."
        }
    }

    private fun returnResultAndFinish() {
        val resultIntent = Intent().apply {
            putExtra("compBoxes", Gson().toJson(compositionBoxes))
        }

        setResult(Activity.RESULT_OK, resultIntent)

        Toast.makeText(
            this,
            "Saved ${compositionBoxes.size} compositions",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }
}