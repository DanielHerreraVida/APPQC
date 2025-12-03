package com.example.qceqapp.uis.QCOrderSent

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
import com.example.qceqapp.uis.QCInspection.QCMediaActivity
import com.example.qceqapp.uis.QCInspection.QCIssuesDialog
import com.example.qceqapp.uis.QCInspection.QCActionsDialog

class QCOrderSentActivity : AppCompatActivity() {

    private val TAG = "QCOrderSentActivity"
    private val viewModel: QCOrderSentViewModel by viewModels()
    private lateinit var containerSelectedIssues: LinearLayout
    private lateinit var scrollIssues: ScrollView

    private lateinit var tV_Grower: TextView
    private lateinit var tV_flogra: TextView
    private lateinit var tV_flocolor: TextView
    private lateinit var tV_boxType: TextView
    private lateinit var tV_description: TextView
    private lateinit var tV_customer: TextView
    private lateinit var btnCamera: ImageButton
    private lateinit var btn_SaveReport: Button
    private lateinit var btnIssues: ImageButton
    private lateinit var tvTotalDescript: TextView
    private lateinit var btnBoxesToInspect: ImageButton
    private lateinit var btnBoxesBadge: TextView
    private lateinit var cameraBadge: TextView
    private lateinit var btnActions: ImageButton
    private lateinit var TVActionChoosed: TextView
    private lateinit var tVAction: TextView
    private lateinit var rB_QCNotification: RadioButton
    private lateinit var rB_QCAction: RadioButton
    private lateinit var btn_SendReport: Button
    private lateinit var descriptionIssue: EditText
    private lateinit var tVNotification: TextView
    private lateinit var TVNotificationChoosed: TextView
    private lateinit var btnNotifications: ImageButton
    private lateinit var tv_OrdBarcode: TextView
    private lateinit var tV_DateSent: TextView
    private lateinit var tv_OrdNumber: TextView
    private lateinit var tv_OrdRow: TextView

    private var idBox: String = ""
    private var reason: String? = null

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
                    val count = selectedList.size
                    btnBoxesBadge.text = count.toString()
                    tv_OrdBarcode.text = "Boxes: $count"
                    Toast.makeText(
                        this,
                        "Boxes updated: $count",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    Toast.makeText(this, "Error updating boxes", Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.selectedBoxes = emptyList()
                btnBoxesBadge.text = "0"
                tv_OrdBarcode.text = "Boxes: 0"
                Toast.makeText(this, "No boxes selected", Toast.LENGTH_SHORT).show()
            }
        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qc_order_send)

        initViews()
        setupObservers()
        setupListeners()
        setupRadioButtons()
        val barcodeBase = intent.getStringExtra("codeReaded") ?: ""
      //  val selectedBoxesJson = intent.getStringExtra("selectedBoxes")
        val infoBx = intent.getStringExtra("infobx") ?: ""
        val awb = intent.getStringExtra("AWB") ?: ""
        val ORDER = intent.getStringExtra("ORDER") ?: ""
        val dateSent = intent.getStringExtra("DATE_SENT") ?: ""
        tV_DateSent.text = if (dateSent.isNotEmpty()) dateSent else "-"
        tv_OrdNumber.text = if (awb.isNotEmpty()) awb else "AWB"
        tv_OrdRow.text = if (ORDER.isNotEmpty()) ORDER else "ORDER"

        idBox = intent.getStringExtra("idBox") ?: intent.getStringExtra("codeReaded") ?: ""
        if (idBox.isEmpty()) {
            Toast.makeText(this, "Box ID not received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            viewModel.loadInspectIdByBox(idBox)
        }

        lifecycleScope.launch {
            viewModel.loadInspectionData(idBox)
        }

        val selectedBoxesJson = intent.getStringExtra("selectedBoxes")
        if (!selectedBoxesJson.isNullOrEmpty()) {
            try {
                val boxes = viewModel.deserializeBoxes(selectedBoxesJson)
                viewModel.selectedBoxes = boxes
                btnBoxesBadge.text = boxes.size.toString()
                tv_OrdBarcode.text = "Boxes: ${boxes.size}"

                Toast.makeText(
                    this,
                    "Boxes loaded: ${boxes.size}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading boxes: ${e.message}")
            }
        } else {
            tv_OrdBarcode.text = "Boxes: 0"

        }

        reason = intent.getStringExtra("reason")

    }

    private fun initViews() {
        tV_Grower = findViewById(R.id.tV_Grower)
        tV_flogra = findViewById(R.id.tV_flogra)
        tV_flocolor = findViewById(R.id.tV_flocolor)
        tV_boxType = findViewById(R.id.tV_boxType)
        tV_description = findViewById(R.id.tV_description)
        tV_customer = findViewById(R.id.tV_customer)
        tv_OrdBarcode = findViewById(R.id.tv_OrdBarcode)
        tV_DateSent = findViewById(R.id.tV_DateSent)
        tv_OrdNumber = findViewById(R.id.tv_OrdNumber)
        tv_OrdRow = findViewById(R.id.tv_OrdRow)
        containerSelectedIssues = findViewById(R.id.containerSelectedIssues)
        scrollIssues = findViewById(R.id.scrollIssues)

        btnCamera = findViewById(R.id.btnCamera)
        btn_SaveReport = findViewById(R.id.btn_SaveReport)
        btnIssues = findViewById(R.id.btnIssues)
       // TVIssueChoosed = findViewById(R.id.TVIssueChoosed)
        tvTotalDescript = findViewById(R.id.tvTotalDescript)
        btnBoxesToInspect = findViewById(R.id.btnBoxesToInspect)
        btnBoxesBadge = findViewById(R.id.btnBoxesBadge)
        descriptionIssue = findViewById(R.id.descriptionIssue)
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
    }

    private fun setupObservers() {
        viewModel.boxInfo.observe(this) { box ->
            tV_Grower.text = box.grower
            tV_flogra.text = box.floGrade
            tV_flocolor.text = box.floColor
            tV_boxType.text = box.boxType
            tV_description.text = box.boxDescription
            tV_customer.text = box.customer
        }

        viewModel.savedBox.observe(this) { savedBox ->
            savedBox?.let { loadSavedBoxData(it) }
        }

        viewModel.openCameraModule.observe(this) { open ->
            if (open == true) {
                val intent = Intent(this, QCMediaActivity::class.java).apply {
                    putExtra("idBox", idBox)
                    putExtra("boxIdToInspect", viewModel.boxIdToInspect)
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

        try {
            var qtyMedia = 0

            // === FOTOS ===
            if (savedBox.imagesList.isNotEmpty()) {
                viewModel.photoPaths = savedBox.imagesList.toMutableList()
                qtyMedia += savedBox.imagesList.size
            }

            // === VIDEOS ===
            if (savedBox.videosList.isNotEmpty()) {
                viewModel.videoPaths = savedBox.videosList.toMutableList()
                qtyMedia += savedBox.videosList.size
            }

            cameraBadge.text = qtyMedia.toString()

            // === ISSUES ===
            if (savedBox.boxIssue.isNotEmpty()) {
                val issueIds = savedBox.boxIssue.split(",")
                val selectedIssues = issues.filter { issue ->
                    issueIds.contains(issue.idIssue)
                }
                viewModel.selectedIssues = selectedIssues
                updateIssuesGrid()
            } else {
                // Si no hay issues, limpia el contenedor
                viewModel.selectedIssues = emptyList()
                updateIssuesGrid()
            }

            // === DESCRIPCIÓN ===
            if (savedBox.boxIssueDescript.isNotEmpty()) {
                descriptionIssue.setText(savedBox.boxIssueDescript)
                val wordCount = countWords(savedBox.boxIssueDescript)
                tvTotalDescript.text = ""
            } else {
                descriptionIssue.setText("")
                tvTotalDescript.text = ""
            }

            // === STATUS QA ===
            when (savedBox.qaInspectionStatus) {
                "1" -> { // QC Notification
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

                "0" -> { // QC Action
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

                else -> { // Estado desconocido
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
            e.printStackTrace()
            Toast.makeText(
                this@QCOrderSentActivity,
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
            handleSendReport()
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
                updateIssuesGrid()  // ← CAMBIAR ESTAS LÍNEAS
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

        btn_SaveReport.setOnClickListener {
            handleSaveReport()
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
    }

    private fun handleSendReport() {
        val issueDescription = descriptionIssue.text.toString().trim()
        val isAccepted = rB_QCNotification.isChecked
        val isRejected = rB_QCAction.isChecked

        val issues = viewModel.selectedIssues
        val actions = viewModel.selectedActions
        val photos = viewModel.photoPaths
        val videos = viewModel.videoPaths
        val selectedBoxes = viewModel.selectedBoxes

        val ordNum = viewModel.boxInfo.value?.numOrd?.toString()?.toIntOrNull() ?: 0
        val awbNum = viewModel.boxInfo.value?.awbNo ?: ""
        val telexNum = viewModel.boxInfo.value?.telexNum ?: ""
//        val awbNum = viewModel.boxInfo.value?.awbNo?.toIntOrNull() ?: 0
//        val telexNum = viewModel.boxInfo.value?.telexNum?.toIntOrNull() ?: 0
        val num = viewModel.boxInfo.value?.num?.toIntOrNull() ?: 0
        val qaInsp = UserSession.getUsername()

        val inspectStatus = if (isAccepted) 1 else 0
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
                        val inspectId = viewModel.inspectId.value ?: idBox

                        val result = viewModel.sendQCHistorySent(
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
                                this@QCOrderSentActivity,
                                "Box sent successfully.",
                                Toast.LENGTH_LONG
                            ).show()

                            rB_QCNotification.isEnabled = true
                            rB_QCAction.isEnabled = true
                            finish()
                        } else {
                            Toast.makeText(
                                this@QCOrderSentActivity,
                                "Error saving item.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving box: ${e.message}", e)
                        Toast.makeText(
                            this@QCOrderSentActivity,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("NO", null)
            .show()
    }

    private fun handleSaveReport() {
        val issueDescription = descriptionIssue.text.toString().trim()
        val isAccepted = rB_QCNotification.isChecked
        val isRejected = rB_QCAction.isChecked

        val issues = viewModel.selectedIssues
        val actions = viewModel.selectedActions
        val photos = viewModel.photoPaths
        val videos = viewModel.videoPaths
        val selectedBoxes = viewModel.selectedBoxes

        val ordNum = viewModel.boxInfo.value?.numOrd?.toString()?.toIntOrNull() ?: 0
//        val awbNum = viewModel.boxInfo.value?.awbNo?.toIntOrNull() ?: 0
//        val telexNum = viewModel.boxInfo.value?.telexNum?.toIntOrNull() ?: 0
        val awbNum = viewModel.boxInfo.value?.awbNo ?: ""
        val telexNum = viewModel.boxInfo.value?.telexNum ?: ""
        val num = viewModel.boxInfo.value?.num?.toIntOrNull() ?: 0
        val qaInsp = UserSession.getUsername()

        val inspectStatus = if (isAccepted) 1 else 0
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
            appendLine("STATUS: ${if (inspectStatus == 1) "QC NOTIFICATION" else "QC LOCAL"}")
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm QC Save")
            .setMessage(summary)
            .setPositiveButton("YES") { _, _ ->
                lifecycleScope.launch {
                    try {
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
                                this@QCOrderSentActivity,
                                "Box sent successfully.",
                                Toast.LENGTH_LONG
                            ).show()

                            rB_QCNotification.isEnabled = true
                            rB_QCAction.isEnabled = true
                            finish()
                        } else {
                            Toast.makeText(
                                this@QCOrderSentActivity,
                                "Error saving item.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving box: ${e.message}", e)
                        Toast.makeText(
                            this@QCOrderSentActivity,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("NO", null)
            .show()
    }
    private fun openBoxesActivity() {
        if (idBox.isEmpty()) {
            Toast.makeText(this, "No box ID available", Toast.LENGTH_SHORT).show()
            return
        }

        btnBoxesToInspect.isEnabled = false

        lifecycleScope.launch {
            try {
                kotlinx.coroutines.delay(300)
                val serializedBoxes = viewModel.serializeSelectedBoxes()
                val intent = Intent(this@QCOrderSentActivity, QCBoxesActivity::class.java).apply {
                    putExtra("codeReaded", idBox)
                    putExtra("fromQCOrderSent", "true")
                    putExtra("selectedBXS", serializedBoxes)
                }
                boxesActivityLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error abriendo QCBoxesActivity: ${e.message}", e)
            } finally {
                btnBoxesToInspect.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        btnBoxesBadge.text = viewModel.selectedBoxes.size.toString()

        val mediaCount = (viewModel.photoPaths.size + viewModel.videoPaths.size)
        if (mediaCount > 0) {
            cameraBadge.text = mediaCount.toString()
        }
    }
}