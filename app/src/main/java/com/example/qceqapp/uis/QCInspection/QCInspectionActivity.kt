package com.example.qceqapp.uis.QCInspection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.qceqapp.R
import com.example.qceqapp.uis.QCBoxes.QCBoxesActivity
import kotlinx.coroutines.launch
import com.example.qceqapp.data.model.Entities
import com.google.gson.Gson
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.qceqapp.utils.GlobalReason
import com.example.qceqapp.data.model.session.UserSession
import com.example.qceqapp.utils.GlobalOrder
import com.google.gson.reflect.TypeToken

class QCInspectionActivity : AppCompatActivity() {

    private val TAG = "QCInspectionActivity"
    private val viewModel: QCInspectionViewModel by viewModels()
    private var idToInspect: String? = null
    private lateinit var gridSelectedIssues: GridLayout
    private lateinit var containerSelectedIssues: LinearLayout
    private lateinit var tV_Grower: TextView
    private lateinit var tV_flogra: TextView
    private lateinit var tV_flocolor: TextView
    private lateinit var tV_boxType: TextView
    private lateinit var tV_description: TextView
    private lateinit var tV_customer: TextView
    private lateinit var tv_AWBNumber: TextView
    private lateinit var tv_TELEXNumber: TextView
    private lateinit var tv_OrdInfo: TextView
    private lateinit var btnCamera: ImageButton
    private lateinit var btn_SaveReport: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnIssues: ImageButton
    private lateinit var scrollIssues: ScrollView

    private lateinit var TVIssueChoosed: TextView
    private lateinit var tvTotalDescript: TextView
    private lateinit var btnBoxesToInspect: ImageButton
    private lateinit var btnBoxesBadge: TextView
    private lateinit var cameraBadge: TextView
    private lateinit var btnCompBoxes: ImageButton
    private lateinit var btnCompBXBadge: TextView
    private var hasAttemptedCompositionLoad = false
    private lateinit var btnActions: ImageButton
    private lateinit var TVActionChoosed: TextView
    private lateinit var tVAction: TextView
    private lateinit var rB_QCRelease: RadioButton

    private var reason: String? = null
    private lateinit var rB_QCNotification: RadioButton
    private lateinit var rB_QCAction: RadioButton
    private lateinit var btn_SendReport: Button
    private lateinit var descriptionIssue: EditText
    private var idBox: String = ""
    private lateinit var tVNotification: TextView
    private lateinit var TVNotificationChoosed: TextView
    private lateinit var btnNotifications: ImageButton
    private var compositionBoxes: List<Entities.BoxCompositionResponse> = emptyList()

    private val boxesActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val selectedJson = data?.getStringExtra("selectedBoxes")

            if (!selectedJson.isNullOrEmpty()) {
                try {
                    val selectedList = viewModel.deserializeBoxes(selectedJson)
                    viewModel.selectedBoxes = selectedList
                    btnBoxesBadge.text = selectedList.size.toString()
                    Toast.makeText(this, "Boxes updated: ${selectedList.size}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializando cajas: ${e.message}", e)
                }
            }
        }
    }

    private val compositionActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val compositionsJson = data?.getStringExtra("compBoxes")

            if (!compositionsJson.isNullOrEmpty()) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<Entities.BoxCompositionResponse>>() {}.type
                    val compositions: List<Entities.BoxCompositionResponse> = Gson().fromJson(compositionsJson, type)

                    viewModel.updateBoxComposition(compositions)
                    btnCompBXBadge.text = compositions.size.toString()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing compositions: ${e.message}", e)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qc_inspection)

        initViews()
        setupObservers()
        setupListeners()
        setupRadioButtons()

        idBox = intent.getStringExtra("idBox") ?: intent.getStringExtra("codeReaded") ?: ""
        if (idBox.isEmpty()) {
            Toast.makeText(this, "Box ID not received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        lifecycleScope.launch {
            viewModel.loadInspectIdByBox(idBox)
        }
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            viewModel.loadInspectionData(idBox)
        }
        val selectedBoxesJson = intent.getStringExtra("selectedBoxes")
        if (!selectedBoxesJson.isNullOrEmpty()) {
            try {
                val boxes = viewModel.deserializeBoxes(selectedBoxesJson)
                viewModel.selectedBoxes = boxes
                btnBoxesBadge.text = boxes.size.toString()

                Toast.makeText(
                    this,
                    "Boxes loaded: ${boxes.size}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
            }
        } else {
        }
        reason = intent.getStringExtra("reason")
        idToInspect = intent.getStringExtra("idToInspect")

        progressBar.visibility = View.VISIBLE

    }

    private fun initViews() {
        tV_Grower = findViewById(R.id.tV_Grower)
        tV_flogra = findViewById(R.id.tV_flogra)
        tV_flocolor = findViewById(R.id.tV_flocolor)
        tV_boxType = findViewById(R.id.tV_boxType)
        tV_description = findViewById(R.id.tV_description)
        tV_customer = findViewById(R.id.tV_customer)
        tv_AWBNumber = findViewById(R.id.tv_AWBNumber)
        tv_TELEXNumber = findViewById(R.id.tv_TELEXNumber)
        tv_OrdInfo = findViewById(R.id.tv_OrdInfo)
        containerSelectedIssues = findViewById(R.id.containerSelectedIssues)
        scrollIssues = findViewById(R.id.scrollIssues)
       // TVIssueChoosed = findViewById(R.id.TVIssueChoosed)
        btnCamera = findViewById(R.id.btnCamera)
        btn_SaveReport = findViewById(R.id.btn_SaveReport)
        progressBar = findViewById(R.id.progressBar)
        btnIssues = findViewById(R.id.btnIssues)
        tvTotalDescript = findViewById(R.id.tvTotalDescript)
        btnBoxesToInspect = findViewById(R.id.btnBoxesToInspect)
        btnBoxesBadge = findViewById(R.id.btnBoxesBadge)
        descriptionIssue = findViewById(R.id.descriptionIssue)
        btnCompBoxes = findViewById(R.id.btnCompBoxes)
        btnCompBXBadge = findViewById(R.id.btnCompBXBadge)
        cameraBadge = findViewById(R.id.btnCameraBadge)
        btn_SendReport = findViewById(R.id.btn_SendReport)
        btnActions = findViewById(R.id.btnActions)
        TVActionChoosed = findViewById(R.id.TVActionChoosed)
        tVAction = findViewById(R.id.tVAction)
        tVNotification = findViewById(R.id.tVNotification)
        TVNotificationChoosed = findViewById(R.id.TVNotificationChoosed)
        btnNotifications = findViewById(R.id.btnNotifications)
        rB_QCNotification = findViewById(R.id.rB_QCNotification)
        rB_QCAction = findViewById(R.id.rB_QCAction)

        rB_QCRelease = findViewById(R.id.rB_QCRelease)
    }
    private fun updateIssuesGrid() {
        containerSelectedIssues.removeAllViews()

        val selectedIssues = viewModel.selectedIssues

        if (selectedIssues.isEmpty()) {
            val layoutParams = scrollIssues.layoutParams
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            scrollIssues.layoutParams = layoutParams

            val emptyView = TextView(this).apply {
                text = "No issues selected..."
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#666666"))
                setPadding(8, 8, 8, 8)
            }
            containerSelectedIssues.addView(emptyView)
        } else {
            val maxColumns = 3
            var currentRow: LinearLayout? = null
            var columnCount = 0
            var rowCount = 0

            selectedIssues.forEachIndexed { index, issue ->
                if (columnCount == 0) {
                    currentRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            if (index > 0) setMargins(0, 8, 0, 0)
                        }
                    }
                    containerSelectedIssues.addView(currentRow)
                    rowCount++
                }

                val issueContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    background = resources.getDrawable(R.drawable.descriptionissue, null)
                    setPadding(12, 6, 8, 6)
                    gravity = android.view.Gravity.CENTER_VERTICAL

                    val params = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        weight = 1f
                        if (columnCount > 0) setMargins(8, 0, 0, 0)
                    }
                    layoutParams = params
                }

                val issueText = TextView(this).apply {
                    text = issue.descriptionIen
                    textSize = 11f
                    setTextColor(android.graphics.Color.parseColor("#000000"))
                    setPadding(0, 0, 6, 0)
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        weight = 1f
                    }
                }

                val removeButton = ImageButton(this).apply {
                    setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    background = null
                    setPadding(2, 2, 2, 2)
                    scaleType = ImageView.ScaleType.FIT_CENTER

                    layoutParams = LinearLayout.LayoutParams(
                        (16 * resources.displayMetrics.density).toInt(),
                        (16 * resources.displayMetrics.density).toInt()
                    )

                    setOnClickListener {
                        val currentIssues = viewModel.selectedIssues.toMutableList()
                        currentIssues.remove(issue)
                        viewModel.selectedIssues = currentIssues
                        updateIssuesGrid()
                    }
                }

                issueContainer.addView(issueText)
                issueContainer.addView(removeButton)
                currentRow?.addView(issueContainer)

                columnCount++

                if (columnCount >= maxColumns) {
                    columnCount = 0
                }
            }
            if (columnCount > 0 && currentRow != null) {
                for (i in columnCount until maxColumns) {
                    val spacer = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            1
                        ).apply {
                            weight = 1f
                            if (i > 0) setMargins(8, 0, 0, 0)
                        }
                    }
                    currentRow?.addView(spacer)
                }
            }

            val layoutParams = scrollIssues.layoutParams
            val maxHeightDp = 120
            val maxHeightPx = (maxHeightDp * resources.displayMetrics.density).toInt()

            if (rowCount <= 3) {
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            } else {
                layoutParams.height = maxHeightPx
            }
            scrollIssues.layoutParams = layoutParams
        }
    }
    private fun setupObservers() {
        viewModel.boxInfo.observe(this) { box ->
            progressBar.visibility = View.GONE
            tV_Grower.text = box.grower
            tV_flogra.text = box.floGrade
            tV_flocolor.text = box.floColor
            tV_boxType.text = box.boxType
            tV_description.text = box.boxDescription
            tV_customer.text = box.customer
            tv_AWBNumber.text = box.awbNo
            tv_TELEXNumber.text = "Telex: ${box.telexNum}"
            tv_OrdInfo.text = "Invoice: ${box.invoiceNum} | Order: ${box.numOrd}"
        }

        viewModel.savedBox.observe(this) { savedBox ->
            savedBox?.let { loadSavedBoxData(it) }
        }

        viewModel.boxComposition.observe(this) { compositions ->
            progressBar.visibility = View.GONE
            btnCompBoxes.isEnabled = true

            if (compositions.isNotEmpty()) {
                btnCompBXBadge.text = compositions.size.toString()
                hasAttemptedCompositionLoad = false
            } else if (hasAttemptedCompositionLoad) {
                Toast.makeText(this, "No composition data available", Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.compositionLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnCompBoxes.isEnabled = !isLoading
        }
    }
    private val compResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val compositionsJson = result.data?.getStringExtra("compBoxes")
            if (!compositionsJson.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<Entities.BoxCompositionResponse>>() {}.type
                    val updatedCompositions: List<Entities.BoxCompositionResponse> =
                        Gson().fromJson(compositionsJson, type)
                    compositionBoxes = updatedCompositions
                    viewModel.updateBoxComposition(updatedCompositions)
                    btnCompBXBadge.text = compositionBoxes.size.toString()

                } catch (e: Exception) {
                    Toast.makeText(this, "Error updating compositions", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun loadSavedBoxData(savedBox: Entities.SavedBoxResponse) {

        val issues = viewModel.qcIssues.value
        val actions = viewModel.qcActions.value

        if (issues.isNullOrEmpty() || actions.isNullOrEmpty()) {
            lifecycleScope.launch {
                kotlinx.coroutines.delay(200)
                if (!viewModel.qcIssues.value.isNullOrEmpty() &&
                    !viewModel.qcActions.value.isNullOrEmpty()) {
                    loadSavedBoxData(savedBox)
                }
            }
            return
        }

        rB_QCNotification.setOnClickListener(null)
        rB_QCAction.setOnClickListener(null)

        try {
            var qtyMedia = 0
            if (savedBox.imagesList.isNotEmpty()) {
                viewModel.photoPaths = savedBox.imagesList.toMutableList()
                qtyMedia += savedBox.imagesList.size
            }
            if (savedBox.videosList.isNotEmpty()) {
                viewModel.videoPaths = savedBox.videosList.toMutableList()
                qtyMedia += savedBox.videosList.size
            }
            cameraBadge.text = qtyMedia.toString()

            if (savedBox.boxIssue.isNotEmpty()) {
                val issueIds = savedBox.boxIssue.split(",")
                val selectedIssues = issues.filter { issue ->
                    issueIds.contains(issue.idIssue)
                }
                viewModel.selectedIssues = selectedIssues
                updateIssuesGrid()
            }
            if (savedBox.boxIssueDescript.isNotEmpty()) {
                descriptionIssue.setText(savedBox.boxIssueDescript)
                tvTotalDescript.text = ""
            }
            when (savedBox.qaInspectionStatus) {
                "1" -> {
                    rB_QCNotification.isChecked = true
                    rB_QCAction.isChecked = false

                    tVNotification.visibility = View.VISIBLE
                    TVNotificationChoosed.visibility = View.VISIBLE
                    btnNotifications.visibility = View.VISIBLE
                    tVAction.visibility = View.GONE
                    TVActionChoosed.visibility = View.GONE
                    btnActions.visibility = View.GONE

                    if (savedBox.boxAction.isNotEmpty()) {
                        val actionIds = savedBox.boxAction.split(",")
                        val selectedActions = actions.filter { action ->
                            actionIds.contains(action.idAction)
                        }
                        viewModel.selectedActions = selectedActions

                        TVNotificationChoosed.text = if (selectedActions.isNotEmpty())
                            selectedActions.joinToString(", ") { it.descriptionAen }
                        else
                            "0 Items."

                    }

                    btn_SaveReport.isEnabled = true
                    btn_SendReport.visibility = View.VISIBLE
                    btn_SendReport.isEnabled = true
                }
                "0" -> {
                    rB_QCAction.isChecked = true
                    rB_QCNotification.isChecked = false

                    tVAction.visibility = View.VISIBLE
                    TVActionChoosed.visibility = View.VISIBLE
                    btnActions.visibility = View.VISIBLE
                    tVNotification.visibility = View.GONE
                    TVNotificationChoosed.visibility = View.GONE
                    btnNotifications.visibility = View.GONE

                    if (savedBox.boxAction.isNotEmpty()) {
                        val actionIds = savedBox.boxAction.split(",")
                        val selectedActions = actions.filter { action ->
                            actionIds.contains(action.idAction)
                        }
                        viewModel.selectedActions = selectedActions

                        TVActionChoosed.text = if (selectedActions.isNotEmpty())
                            selectedActions.joinToString(", ") { it.descriptionAen }
                        else
                            "0 Items."

                    }

                    btn_SaveReport.isEnabled = true
                    btn_SendReport.visibility = View.VISIBLE
                    btn_SendReport.isEnabled = true
                }
                "2" -> {
                    rB_QCRelease.isChecked = true
                    rB_QCNotification.isChecked = false
                    rB_QCAction.isChecked = false

                    tVAction.visibility = View.GONE
                    TVActionChoosed.visibility = View.GONE
                    btnActions.visibility = View.GONE
                    tVNotification.visibility = View.GONE
                    TVNotificationChoosed.visibility = View.GONE
                    btnNotifications.visibility = View.GONE

                    btn_SaveReport.isEnabled = true
                    btn_SendReport.visibility = View.VISIBLE
                    btn_SendReport.isEnabled = true
                }
                else -> {
                    btn_SendReport.isEnabled = false
                    tVAction.visibility = View.GONE
                    TVActionChoosed.visibility = View.GONE
                    btnActions.visibility = View.GONE
                    tVNotification.visibility = View.GONE
                    TVNotificationChoosed.visibility = View.GONE
                    btnNotifications.visibility = View.GONE
                }
            }

        } catch (e: Exception) {
            Toast.makeText(
                this@QCInspectionActivity,
                "Error loading saved data",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            setupRadioButtons()
        }
    }

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    private fun setupListeners() {
        btnCamera.setOnClickListener { viewModel.onCameraButtonClicked() }
        btn_SendReport.setOnClickListener {
            val issueDescription = descriptionIssue.text.toString().trim()
            val isAccepted = rB_QCNotification.isChecked
            val isRejected = rB_QCAction.isChecked
            val isRelease = rB_QCRelease.isChecked
            val issues = viewModel.selectedIssues
            val actions = viewModel.selectedActions
            val photos = viewModel.photoPaths
            val videos = viewModel.videoPaths
            val selectedBoxes = viewModel.selectedBoxes

            val ordNum = viewModel.boxInfo.value?.numOrd?.toString()?.toIntOrNull() ?: 0
            val awbNum = viewModel.boxInfo.value?.awbNo ?: ""
            val telexNum = viewModel.boxInfo.value?.telexNum ?: ""
//            val num = viewModel.boxInfo.value?.num?.toIntOrNull() ?: 0

            val num = GlobalOrder.getBxNUM()?.toIntOrNull()
                ?: viewModel.boxInfo.value?.num?.toIntOrNull() ?: 0
            val qaInsp = UserSession.getUsername()

            val inspectStatus = when {
                isAccepted -> "1"
                isRejected -> "0"
                isRelease -> "2"
                else -> "0"
            }
//            val inspectStatus = if (isAccepted) "1" else "0"
            val issueC = issues.joinToString(",") { it.idIssue }
            val actionC = actions.joinToString(",") { it.idAction }
            val barcodesToI = selectedBoxes.joinToString(",") { it.barcode }
            val summary = buildString {
                appendLine("Barcode: $idBox")
                appendLine("Order Num: $ordNum")
                appendLine("Qty Boxes To Inspect: [${selectedBoxes.size}]")
                appendLine("AWB: $awbNum")
                appendLine("TELEX: $telexNum")
                appendLine("NUM: $num")
                appendLine("PHOTOS: ${photos.size}")
                appendLine("VIDEOS: ${videos.size}")
                appendLine("ISSUES: $issueC")
                appendLine("ACTIONS: $actionC")
                appendLine("DESCRIPTION: $issueDescription")
                appendLine("STATUS: ${when {
                    isAccepted -> "QC NOTIFICATION"
                    isRejected -> "QC LOCAL"
                    isRelease -> "RELEASE"
                    else -> "UNKNOWN"
                }}")
            }


            AlertDialog.Builder(this)
                .setTitle("Send to QC History")
                .setMessage(summary)
                .setPositiveButton("YES") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            progressBar.visibility = View.VISIBLE
                            val inspectId = viewModel.inspectId.value ?: idBox

                            val result = viewModel.sendBoxToQCHistory(
                                idBox = inspectId,
                                ordNum = ordNum,
                                awbNum = awbNum,
                                telexNum = telexNum,
                                num = num,
                                issueC = issueC,
                                actionC = actionC,
                                issueDes = issueDescription,
                                qaInsp = qaInsp,
                                listImages = photos,
                                listVideos = videos,
                                inspectStatus = inspectStatus,
                                barcodesToI = barcodesToI
                            )

                            progressBar.visibility = View.GONE
                            if (result.isSuccess) {
                                val compositionBoxes = viewModel.boxComposition.value
                                if (!compositionBoxes.isNullOrEmpty()) {
                                    val svc = com.example.qceqapp.data.network.Service()
                                    for (comp in compositionBoxes) {
                                        try {
                                            val idBoxInt = comp.inbNumBox.toIntOrNull()

                                            if (idBoxInt == null) {
                                                continue
                                            }
                                            if (comp.issues.isNullOrEmpty()) {
                                                continue
                                            }

                                            val issuesStr = comp.issues.joinToString(",")
                                            val updRes = svc.updateCompositionsIssues(
                                                idBox = idBoxInt,
                                                floGrade = comp.floGrade,
                                                floCod = comp.floCod,
                                                issues = issuesStr
                                            )

                                            updRes.onSuccess {
                                            }

                                            updRes.onFailure { ex ->
                                            }
                                        } catch (e: Exception) {
                                        }
                                    }

                                } else {
                                    Log.d(TAG, "No compositions to save")
                                }
                                Toast.makeText(
                                    this@QCInspectionActivity,
                                    "Box sent successfully.",
                                    Toast.LENGTH_LONG
                                ).show()

                                rB_QCNotification.isEnabled = true
                                rB_QCAction.isEnabled = true
                                finish()
                            } else {
                                Toast.makeText(
                                    this@QCInspectionActivity,
                                    "An error occurred, try again",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            progressBar.visibility = View.GONE
                            Log.e(TAG, "Error sending box: ${e.message}", e)
                            Toast.makeText(
                                this@QCInspectionActivity,
                                "Error: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .setNegativeButton("NO", null)
                .show()
        }
        viewModel.openCameraModule.observe(this) { open ->
            if (open == true) {
                val intent = Intent(this, QCMediaActivity::class.java).apply {
                    putExtra("idBox", idBox)
                    putExtra(
                        "boxIdToInspect",
                        if (!viewModel.boxIdToInspect.isNullOrEmpty()) viewModel.boxIdToInspect else idToInspect
                    )
                    putExtra("orderNum", viewModel.orderNum)
                    if (viewModel.photoPaths.isNotEmpty()) {
                        val gson = Gson()
                        putExtra("listImagesCaptured", gson.toJson(viewModel.photoPaths))
                    }
                    if (viewModel.videoPaths.isNotEmpty()) {
                        val gson = Gson()
                        putExtra("listVideosCaptured", gson.toJson(viewModel.videoPaths))
                    }
                }

                mediaActivityLauncher.launch(intent)
                viewModel.resetNavigationFlag()
            }
        }

        btnIssues.setOnClickListener {
            val issues = viewModel.qcIssues.value ?: emptyList()
            if (issues.isEmpty()) {
                Toast.makeText(this, "Loading issues, please wait...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            QCIssuesDialog(
                this,
                issues,
                preselected = viewModel.selectedIssues
            ) { selected ->
                viewModel.selectedIssues = selected
                updateIssuesGrid()  // ← SOLO CAMBIAR ESTA LÍNEA (antes era TVIssueChoosed.text = ...)
            }.show()
        }
        btnActions.setOnClickListener {
            val actions = viewModel.qcActions.value ?: emptyList()
            if (actions.isEmpty()) {
                Toast.makeText(this, "Loading actions, please wait...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            QCActionsDialog(
                this,
                actions,
                preselected = viewModel.selectedActions
            ) { selected ->
                viewModel.selectedActions = selected
                if (selected.isNotEmpty()) {
                    TVActionChoosed.text = selected.joinToString(", ") { it.descriptionAen }
                } else {
                    TVActionChoosed.text = "No selection"
                }
            }.show()
        }

        btnNotifications.setOnClickListener {
            val actions = viewModel.qcActions.value ?: emptyList()
            if (actions.isEmpty()) {
                Toast.makeText(this, "Loading actions, please wait...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            QCActionsDialog(
                this,
                actions,
                preselected = viewModel.selectedActions
            ) { selected ->
                viewModel.selectedActions = selected
                if (selected.isNotEmpty()) {
                    TVNotificationChoosed.text = selected.joinToString(", ") { it.descriptionAen }
                } else {
                    TVNotificationChoosed.text = "No selection"
                }
            }.show()
        }
        btnCompBoxes.
        setOnClickListener {
            val badgeCount = btnBoxesBadge.text.toString().toIntOrNull() ?: 0

            if (badgeCount <= 0) {
                Toast.makeText(
                    this@QCInspectionActivity,
                    "You must select at least one box before loading composition.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            btnCompBoxes.isEnabled = false
            progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    val service = com.example.qceqapp.data.network.Service()

                    val existingCompositions = viewModel.boxComposition.value

                    val compositions: MutableList<Entities.BoxCompositionResponse> = if (!existingCompositions.isNullOrEmpty()) {
                        existingCompositions.toMutableList()
                    } else {
                        val loadedCompositions = mutableListOf<Entities.BoxCompositionResponse>()

                        val boxesToInspect = if (!viewModel.selectedBoxes.isNullOrEmpty()) {
                            viewModel.selectedBoxes
                        } else {
                            listOf(Entities.BoxToInspect(barcode = idBox))
                        }

                        boxesToInspect.forEach {
                        }

                        for (box in boxesToInspect) {
                            try {
                                val result = service.getBoxComposition(box.barcode)
                                result.getOrNull()?.let { loadedCompositions.addAll(it) }
                            } catch (e: Exception) {
                            }
                        }
                        loadedCompositions
                    }

                    progressBar.visibility = View.GONE
                    btnCompBoxes.isEnabled = true

                    if (compositions.isEmpty()) {
                        Toast.makeText(
                            this@QCInspectionActivity,
                            "No composition data available for selected boxes",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    btnCompBXBadge.text = compositions.size.toString()

                    val serialized = Gson().toJson(compositions)
                    val intent = Intent(this@QCInspectionActivity, QCCompositionActivity::class.java).apply {
                        putExtra("fromActivityInfo", "true")
                        putExtra("selectedBXS", serialized)
                    }
                    compositionActivityLauncher.launch(intent)

                } catch (e: Exception) {
                    Log.e(TAG, "Error in composition logic: ${e.message}", e)
                    progressBar.visibility = View.GONE
                    btnCompBoxes.isEnabled = true
                    Toast.makeText(
                        this@QCInspectionActivity,
                        "Error loading compositions",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        val btnRejectBX: Button = findViewById(R.id.btn_Rejectbx)
        btnRejectBX.setOnClickListener {
            val totalBoxes = viewModel.selectedBoxes.size
            val msg = "Are you sure you don't want to inspect: [$totalBoxes] Boxes ?"

            AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("Yes") { _, _ ->
                    rejectScannedBoxes()
                }
                .setNegativeButton("No", null)
                .show()
        }


        btn_SaveReport.setOnClickListener {
            val issueDescription = descriptionIssue.text.toString().trim()
            val isAccepted = rB_QCNotification.isChecked
            val isRejected = rB_QCAction.isChecked
            val isRelease = rB_QCRelease.isChecked

            val issues = viewModel.selectedIssues
            val actions = viewModel.selectedActions
            val photos = viewModel.photoPaths
            val videos = viewModel.videoPaths
            val selectedBoxes = viewModel.selectedBoxes

            val ordNum = viewModel.boxInfo.value?.numOrd?.toString()?.toIntOrNull() ?: 0
//            val awbNum = viewModel.boxInfo.value?.awbNo?.toIntOrNull() ?: 0
//            val telexNum = viewModel.boxInfo.value?.telexNum?.toIntOrNull() ?: 0
            val awbNum = viewModel.boxInfo.value?.awbNo ?: ""
            val telexNum = viewModel.boxInfo.value?.telexNum ?: ""
//            val num = viewModel.boxInfo.value?.num?.toIntOrNull() ?: 0

            val num = GlobalOrder.getBxNUM()?.toIntOrNull()
                ?: viewModel.boxInfo.value?.num?.toIntOrNull() ?: 0

            val qaInsp = UserSession.getUsername()

//            when {
//                !isAccepted && !isRejected -> {
//                    Toast.makeText(this, "Select a QC status (Notification or Local).", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }
//                issues.isEmpty() -> {
//                    Toast.makeText(this, "You must choose at least one issue.", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }
//                issueDescription.isEmpty() -> {
//                    Toast.makeText(this, "You must write a description.", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }
//                actions.isEmpty() -> {
//                    val tipo = if (isAccepted) "QC NOTIFICATION" else "QC LOCAL"
//                    Toast.makeText(this, "$tipo requires at least one action.", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }
//                isRejected && photos.isEmpty() && videos.isEmpty() -> {
//                    Toast.makeText(this, "QC LOCAL requires at least one Photo or Video.", Toast.LENGTH_LONG).show()
//                    return@setOnClickListener
//                }
//            }

            val inspectStatus = when {
                isAccepted -> "1"
                isRejected -> "0"
                isRelease -> "2"
                else -> "0"
            }
            val issueC = issues.joinToString(",") { it.idIssue }
            val actionC = actions.joinToString(",") { it.idAction }
            val barcodesToI = selectedBoxes.joinToString(",") { it.barcode }

            val summary = buildString {
                appendLine("Barcode: $idBox")
                appendLine("Order Num: $ordNum")
                appendLine("Qty Boxes To Inspect: [${selectedBoxes.size}]")
                appendLine("AWB: $awbNum")
                appendLine("TELEX: $telexNum")
                appendLine("NUM: $num")
                appendLine("PHOTOS: ${photos.size}")
                appendLine("VIDEOS: ${videos.size}")
                appendLine("ISSUES: $issueC")
                appendLine("ACTIONS: $actionC")
                appendLine("DESCRIPTION: $issueDescription")
                appendLine("STATUS: ${if (isAccepted) "QC NOTIFICATION" else "QC LOCAL"}")
            }

            AlertDialog.Builder(this)
                .setTitle("Confirm QC Save")
                .setMessage(summary)
                .setPositiveButton("YES") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            progressBar.visibility = View.VISIBLE
                            val inspectId = viewModel.inspectId.value ?: idBox

                            val result = viewModel.saveQCBox(
                                idBox = inspectId,
                                ordNum = ordNum,
                                awbNum = awbNum,
                                telexNum = telexNum,
                                num = num,
                                issueC = issueC,
                                actionC = actionC,
                                issueDes = issueDescription,
                                qaInsp = qaInsp,
                                listImages = photos,
                                listVideos = videos,
                                inspectStatus = inspectStatus,
                                barcodesToI = barcodesToI
                            )

                            progressBar.visibility = View.GONE

                            if (result.isSuccess) {
                                val compositionBoxes = viewModel.boxComposition.value
                                if (!compositionBoxes.isNullOrEmpty()) {
                                    val svc = com.example.qceqapp.data.network.Service()
                                    for (comp in compositionBoxes) {
                                        try {
                                            val idBoxInt = comp.inbNumBox.toIntOrNull()

                                            if (idBoxInt == null) {
                                                continue
                                            }
                                            if (comp.issues.isNullOrEmpty()) {
                                                continue
                                            }

                                            val issuesStr = comp.issues.joinToString(",")
                                            val updRes = svc.updateCompositionsIssues(
                                                idBox = idBoxInt,
                                                floGrade = comp.floGrade,
                                                floCod = comp.floCod,
                                                issues = issuesStr
                                            )

                                            updRes.onSuccess {
                                            }

                                            updRes.onFailure { ex ->
                                            }
                                        } catch (e: Exception) {
                                        }
                                    }

                                } else {
                                    Log.d(TAG, "No compositions to save")
                                }

                                Toast.makeText(
                                    this@QCInspectionActivity,
                                    "Item saved successfully",
                                    Toast.LENGTH_LONG
                                ).show()

                                rB_QCNotification.isEnabled = true
                                rB_QCAction.isEnabled = true
                                finish()
                            } else {
                                Toast.makeText(
                                    this@QCInspectionActivity,
                                    "Error saving item.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            progressBar.visibility = View.GONE
                            Log.e(TAG, "Error saving box: ${e.message}", e)
                            Toast.makeText(
                                this@QCInspectionActivity,
                                "Error: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .setNegativeButton("NO", null)
                .show()
        }
        btnBoxesToInspect.setOnClickListener { openBoxesActivity() }

        descriptionIssue.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val count = countWords(s.toString())
              tvTotalDescript.text = ""
            }
        })
    }
//    private fun setupRadioButtons() {
//        rB_QCNotification.setOnClickListener {
//            viewModel.selectedActions = emptyList()
//            TVActionChoosed.text = "No selection"
//            TVNotificationChoosed.text = "No selection"
//            tVNotification.visibility = View.VISIBLE
//            TVNotificationChoosed.visibility = View.VISIBLE
//            btnNotifications.visibility = View.VISIBLE
//            tVAction.visibility = View.GONE
//            TVActionChoosed.visibility = View.GONE
//            btnActions.visibility = View.GONE
//            btn_SendReport.visibility = View.VISIBLE
//            btn_SendReport.isEnabled = true
//        }
//
//        rB_QCAction.setOnClickListener {
//            viewModel.selectedActions = emptyList()
//            TVActionChoosed.text = "No selection"
//            TVNotificationChoosed.text = "No selection"
//            tVAction.visibility = View.VISIBLE
//            TVActionChoosed.visibility = View.VISIBLE
//            btnActions.visibility = View.VISIBLE
//            tVNotification.visibility = View.GONE
//            TVNotificationChoosed.visibility = View.GONE
//            btnNotifications.visibility = View.GONE
//            btn_SendReport.visibility = View.VISIBLE
//            btn_SendReport.isEnabled = true
//        }
//    }
private fun setupRadioButtons() {
    rB_QCNotification.setOnClickListener {
        viewModel.selectedActions = emptyList()
        TVActionChoosed.text = "No selection"
        TVNotificationChoosed.text = "No selection"
        tVNotification.visibility = View.VISIBLE
        TVNotificationChoosed.visibility = View.VISIBLE
        btnNotifications.visibility = View.VISIBLE
        tVAction.visibility = View.GONE
        TVActionChoosed.visibility = View.GONE
        btnActions.visibility = View.GONE
        btn_SendReport.visibility = View.VISIBLE
        btn_SendReport.isEnabled = true
    }

    rB_QCAction.setOnClickListener {
        viewModel.selectedActions = emptyList()
        TVActionChoosed.text = "No selection"
        TVNotificationChoosed.text = "No selection"
        tVAction.visibility = View.VISIBLE
        TVActionChoosed.visibility = View.VISIBLE
        btnActions.visibility = View.VISIBLE
        tVNotification.visibility = View.GONE
        TVNotificationChoosed.visibility = View.GONE
        btnNotifications.visibility = View.GONE
        btn_SendReport.visibility = View.VISIBLE
        btn_SendReport.isEnabled = true
    }
    rB_QCRelease.setOnClickListener {
        AlertDialog.Builder(this)
            .setTitle("Release Confirmation")
            .setMessage("Selecting 'Release' will clear all selected issues and actions. The inspection will be marked as released without any issues. Do you want to continue?")
            .setPositiveButton("YES") { _, _ ->
                viewModel.selectedIssues = emptyList()
                viewModel.selectedActions = emptyList()
                updateIssuesGrid()
                TVActionChoosed.text = "No selection"
                TVNotificationChoosed.text = "No selection"

                tVNotification.visibility = View.GONE
                TVNotificationChoosed.visibility = View.GONE
                btnNotifications.visibility = View.GONE
                tVAction.visibility = View.GONE
                TVActionChoosed.visibility = View.GONE
                btnActions.visibility = View.GONE
                btn_SendReport.visibility = View.VISIBLE
                btn_SendReport.isEnabled = true
            }
            .setNegativeButton("NO") { _, _ ->
                rB_QCRelease.isChecked = false
            }
            .show()
    }
}

    private fun toggleLocalActionVisibility(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        tVAction.visibility = visibility
        TVActionChoosed.visibility = visibility
        btnActions.visibility = visibility
    }
    private val mediaActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val gson = Gson()

            data?.getStringExtra("listImagesCaptured")?.let { json ->
                val type = object : com.google.gson.reflect.TypeToken<MutableList<String>>() {}.type
                viewModel.photoPaths = gson.fromJson(json, type)
            }

            data?.getStringExtra("listVideosCaptured")?.let { json ->
                val type = object : com.google.gson.reflect.TypeToken<MutableList<String>>() {}.type
                viewModel.videoPaths = gson.fromJson(json, type)
            }
            val totalMedia = viewModel.photoPaths.size + viewModel.videoPaths.size
            cameraBadge.text = totalMedia.toString()
        }
    }
    private fun rejectScannedBoxes() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val inspectId = viewModel.inspectId.value ?: idBox
                val service = com.example.qceqapp.data.network.Service()
                val idBox = inspectId
                val qaInspector = UserSession.getUsername()
               // val qaReason = reason ?: "Not Inspected"
                val qaReason = GlobalReason.get()

                val result = withContext(Dispatchers.IO) {
                    service.rejectScannedBox(idBox, qaInspector, qaReason)
                }
                progressBar.visibility = View.GONE
                if (result.isSuccess) {
                    Toast.makeText(this@QCInspectionActivity, "Item deleted from list", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@QCInspectionActivity, "An error occurred, try again", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@QCInspectionActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openBoxesActivity() {
        if (idBox.isEmpty()) {
            Toast.makeText(this, "No box ID available", Toast.LENGTH_SHORT).show()
            return
        }

        btnBoxesToInspect.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                kotlinx.coroutines.delay(300)
                val serializedBoxes = viewModel.serializeSelectedBoxes()
                val intent = Intent(this@QCInspectionActivity, QCBoxesActivity::class.java).apply {
                    putExtra("codeReaded", idBox)
                    putExtra("fromActivityInfo", "true")
                    putExtra("selectedBXS", serializedBoxes)
                }
                boxesActivityLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error abriendo QCBoxesActivity: ${e.message}", e)
            } finally {
                progressBar.visibility = View.GONE
                btnBoxesToInspect.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        btnBoxesBadge.text = viewModel.selectedBoxes.size.toString()
        val comps = viewModel.boxComposition.value
        if (!comps.isNullOrEmpty()) {
            btnCompBXBadge.text = comps.size.toString()
        }

        val mediaCount = (viewModel.photoPaths.size + viewModel.videoPaths.size)
        if (mediaCount > 0) {
            cameraBadge.text = mediaCount.toString()
        }
    }
}