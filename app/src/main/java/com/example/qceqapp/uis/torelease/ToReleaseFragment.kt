package com.example.qceqapp.uis.torelease

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.databinding.FragmentToReleaseBinding
import com.example.qceqapp.uis.scanner.BarcodeScannerActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ToReleaseFragment : Fragment() {

    companion object {
        private const val TAG = "ToReleaseFragment"
        private const val VIEW_PENDING = 0
        private const val VIEW_HISTORY = 1
    }

    private var _binding: FragmentToReleaseBinding? = null
    private val binding get() = _binding!!
    private var currentFilterDialog: ReleaseFilterDialog? = null

    private val viewModel: ToReleaseViewModel by viewModels()
    private lateinit var historyAdapter: ReleaseAdapter
    private lateinit var pendingAdapter: PendingReleaseAdapter
    private var skipNextResume = false
    private var currentView = VIEW_HISTORY

    private var historyFilters = ReleaseFilterDialog.FilterOptions()
    private var pendingFilters = ReleaseFilterDialog.FilterOptions()

    private lateinit var scannerLauncher: ActivityResultLauncher<Intent>

    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToReleaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPressHandler()
        initializeFragment()
    }

    override fun onResume() {
        super.onResume()
        handleResume()
        binding.etSearch.postDelayed({
            requestSearchFocus()
        }, 100)
    }

    override fun onDestroyView() {
        cleanupFragment()
        super.onDestroyView()
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressCallback
        )
    }

    private fun initializeFragment() {
        runCatching {
            setupActivityResultLaunchers()
            setupViewSelector()
            setupRecyclerViews()
            setupSearchListener()
            setupButtons()
            observeViewModel()
            viewModel.loadReleasedBoxes()

            binding.etSearch.postDelayed({
                requestSearchFocus()
            }, 300)
        }.onFailure { e ->
            showError("Error initializing view: ${e.localizedMessage}")
        }
    }

    private fun setupViewSelector() {
        val viewOptions = arrayOf("Pending Items", "Release History")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            viewOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerViewSelector.adapter = adapter
        binding.spinnerViewSelector.setSelection(VIEW_HISTORY)

        binding.spinnerViewSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentView = position
                switchView(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun switchView(viewType: Int) {
        when (viewType) {
            VIEW_PENDING -> {
                binding.recyclerPending.isVisible = true
                binding.recyclerHistory.isVisible = false
                viewModel.applyFilters(pendingFilters)

                updateEmptyState(viewModel.pendingItems.value.isEmpty(), "No pending items")
                updateItemCount(viewModel.pendingItems.value.size)

                binding.btnProcess.isEnabled = viewModel.pendingItems.value.isNotEmpty()
                binding.btnProcess.alpha = if (viewModel.pendingItems.value.isNotEmpty()) 1.0f else 0.5f
            }
            VIEW_HISTORY -> {
                binding.recyclerPending.isVisible = false
                binding.recyclerHistory.isVisible = true
                viewModel.applyFilters(historyFilters)

                updateEmptyState(viewModel.filteredBoxes.value.isEmpty(), "No release history")
                updateItemCount(viewModel.filteredBoxes.value.size)

                binding.btnProcess.isEnabled = false
                binding.btnProcess.alpha = 0.5f
            }
        }
    }

    private fun updateItemCount(count: Int) {
        binding.tvItemCount.text = "($count)"
    }

    private fun updateEmptyState(isEmpty: Boolean, message: String) {
        binding.layoutEmptyState.isVisible = isEmpty
        binding.tvEmptyMessage.text = message
    }

    private fun setupActivityResultLaunchers() {
        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            skipNextResume = true

            val scanType = result.data?.getStringExtra("TYPE")

            if (scanType == "filter") {
                if (result.resultCode == Activity.RESULT_OK) {
                    val scannedCode = result.data?.getStringExtra("SCANNED_CODE")
                    scannedCode?.let { code ->
                        currentFilterDialog?.addScannedBox(code)
                    }
                }
            } else {
                handleScanResult(result.resultCode, result.data?.getStringExtra("SCANNED_CODE"))
            }
        }
    }

    private fun setupRecyclerViews() {
        pendingAdapter = PendingReleaseAdapter(
            pendingItems = emptyList(),
            onItemClick = ::showPendingItemInfo,
            onDeleteClick = ::handleDeletePendingItem
        )
        binding.recyclerPending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pendingAdapter
            setHasFixedSize(true)
        }

        historyAdapter = ReleaseAdapter(
            boxes = emptyList(),
            onBoxClick = ::handleBoxClick,
            onUserClick = ::showBoxInfo,
            onDeleteClick = ::handleDeleteBox
        )
        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString()
            viewModel.searchBoxes(query)
        }

        binding.etSearch.setOnEditorActionListener { v, actionId, event ->
            val isEnterPressed = event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.action == android.view.KeyEvent.ACTION_DOWN
            val isDoneAction = actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_GO

            if (isEnterPressed || isDoneAction) {
                val code = v.text.toString().trim()
                if (code.isNotEmpty()) {
                    handleManualScan(code)
                    return@setOnEditorActionListener true
                }
            }
            false
        }
    }

    private fun setupButtons() {
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnScan.setOnClickListener {
            openScanner()
        }

        binding.btnProcess.setOnClickListener {
            handleProcessPending()
        }
    }
    private fun showFilterDialog() {
        when (currentView) {
            VIEW_PENDING -> {
                val pendingBoxes = viewModel.pendingItems.value.map { it.box }.distinct().sorted()

                if (pendingBoxes.isEmpty()) {
                    showMessage("No pending items to filter")
                    return
                }

                currentFilterDialog = ReleaseFilterDialog(
                    context = requireContext(),
                    allUsers = emptyList(),
                    allBoxes = pendingBoxes,
                    currentFilters = pendingFilters,
                    isPendingView = true,
                    onApplyFilters = { filters ->
                        pendingFilters = filters
                        viewModel.applyPendingFilters(filters)

                        val filterCount = if (filters.scannedBoxes.isNotEmpty()) 1 else 0
                        if (filterCount > 0) {
                            showMessage("$filterCount filter(s) applied to Pending")
                        } else {
                            showMessage("All Pending filters cleared")
                        }

                        currentFilterDialog = null
                    },
                    onScanRequested = {
                        openFilterScanner()
                    }
                )
            }
            VIEW_HISTORY -> {
                val allUsers = viewModel.getAllUsers()
                val historyBoxes = viewModel.releasedBoxes.value.map { it.box.toString() }.distinct().sorted()

                if (allUsers.isEmpty() && historyBoxes.isEmpty()) {
                    showMessage("No history data to filter")
                    return
                }

                currentFilterDialog = ReleaseFilterDialog(
                    context = requireContext(),
                    allUsers = allUsers,
                    allBoxes = historyBoxes,
                    currentFilters = historyFilters,
                    isPendingView = false,
                    onApplyFilters = { filters ->
                        historyFilters = filters
                        viewModel.applyHistoryFilters(filters)

                        var filterCount = 0
                        if (filters.selectedUsers.isNotEmpty()) filterCount++
                        if (filters.startDate != null || filters.endDate != null) filterCount++
                        if (filters.scannedBoxes.isNotEmpty()) filterCount++

                        if (filterCount > 0) {
                            showMessage("$filterCount filter(s) applied to History")
                        } else {
                            showMessage("All History filters cleared")
                        }

                        currentFilterDialog = null
                    },
                    onScanRequested = {
                        openFilterScanner()
                    }
                )
            }
        }

        currentFilterDialog?.show()
    }
    private fun openFilterScanner() {
        val intent = Intent(requireContext(), BarcodeScannerActivity::class.java).apply {
            putExtra("ORDER_CODE", "Filter Scan")
            putExtra("ORDER_NUMBER", "")
            putExtra("TYPE", "filter")
        }
        scannerLauncher.launch(intent)
    }

    private fun showDuplicateAlert(message: String) {
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        )

        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(
            android.graphics.Color.parseColor("#D32F2F")
        )

        val textView = snackbarView.findViewById<android.widget.TextView>(
            com.google.android.material.R.id.snackbar_text
        )
        textView.setTextColor(android.graphics.Color.WHITE)
        textView.textSize = 16f
        textView.maxLines = 3

        snackbar.show()
    }

    private fun observeViewModel() {
        observeFlow(viewModel.isLoading) { isLoading ->
            binding.progressBar.isVisible = isLoading
            if (!isLoading) {
                setProcessButtonLoading(false)
            }
        }

        observeFlow(viewModel.error) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
                setProcessButtonLoading(false)
            }
        }

        observeFlow(viewModel.duplicateMessage) { message ->
            message?.let {
                showDuplicateAlert(it)
                viewModel.clearDuplicateMessage()
            }
        }

        observeFlow(viewModel.warning) { warning ->
            warning?.let {
                showMessage(it)
                viewModel.clearWarning()
            }
        }

        observeFlow(viewModel.playErrorSound) { shouldPlay ->
            if (shouldPlay) {
                playErrorSound()
                viewModel.clearErrorSoundFlag()
            }
        }

        observeFlow(viewModel.filteredBoxes) { boxes ->
            updateHistoryAdapter(boxes)
            if (currentView == VIEW_HISTORY) {
                updateItemCount(boxes.size)
                updateEmptyState(boxes.isEmpty(), "No release history")
            }
        }

        observeFlow(viewModel.pendingItems) { items ->
            updatePendingAdapter(items)
            if (currentView == VIEW_PENDING) {
                updateItemCount(items.size)
                updateEmptyState(items.isEmpty(), "No pending items")
                binding.btnProcess.isEnabled = items.isNotEmpty()
                binding.btnProcess.alpha = if (items.isNotEmpty()) 1.0f else 0.5f
            }
        }

        observeFlow(viewModel.releaseResult) { result ->
            result?.let {
                setProcessButtonLoading(false)

                when (it) {
                    is ToReleaseViewModel.ReleaseResult.Success -> {
                        showSuccessMessage(it.message)
                        binding.spinnerViewSelector.setSelection(VIEW_HISTORY)
                        binding.etSearch.text?.clear()
                        binding.etSearch.postDelayed({
                            requestSearchFocus()
                        }, 100)
                    }
                    is ToReleaseViewModel.ReleaseResult.Error -> {
                        showError(it.message)
                        binding.etSearch.text?.clear()
                        binding.etSearch.postDelayed({
                            requestSearchFocus()
                        }, 100)
                    }
                    is ToReleaseViewModel.ReleaseResult.PartialSuccess -> {
                        val message = buildString {
                            append("Released: ${it.successCount}\n")
                            append("Failed: ${it.failedCount}\n")
                            append("Failed IDs: ${it.failedIds.joinToString(", ")}")
                        }
                        showPartialSuccessDialog(message, it.successCount)
                        binding.etSearch.text?.clear()
                        binding.etSearch.postDelayed({
                            requestSearchFocus()
                        }, 100)
                    }
                }
                viewModel.clearReleaseResult()
            }
        }
    }

    private fun showPartialSuccessDialog(message: String, successCount: Int) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Partial Success")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                if (successCount > 0) {
                    showMessage("$successCount box(es) were released successfully")
                }
            }
            .show()
    }

    private fun playErrorSound() {
        try {
            val toneGen = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION,
                100
            )
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, 200)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing error sound", e)
        }
    }

    private fun handleManualScan(scannedCode: String) {
        val code = scannedCode.trim()

        if (code.isEmpty()) {
            return
        }

        viewModel.addPendingItem(code)

        binding.etSearch.setText("")
        binding.etSearch.postDelayed({
            requestSearchFocus()
        }, 100)
    }

    private fun <T> observeFlow(
        flow: kotlinx.coroutines.flow.StateFlow<T>,
        action: (T) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                flow.collectLatest { value ->
                    runCatching { action(value) }
                        .onFailure { e -> Log.e(TAG, "Error in flow observer", e) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting flow", e)
            }
        }
    }

    private fun updateHistoryAdapter(boxes: List<Entities.ReleaseBoxHistoryResponse>) {
        historyAdapter.updateData(boxes)
        binding.recyclerHistory.post {
            binding.recyclerHistory.requestLayout()
        }
    }

    private fun updatePendingAdapter(items: List<PendingReleaseItem>) {
        pendingAdapter.updateData(items)
    }

    private fun handleScanResult(resultCode: Int, scannedCode: String?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                if (!scannedCode.isNullOrEmpty()) {
                    handleManualScan(scannedCode)
                } else {
                    showMessage("No code detected")
                    binding.etSearch.postDelayed({
                        requestSearchFocus()
                    }, 100)
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Scan was canceled")
                binding.etSearch.postDelayed({
                    requestSearchFocus()
                }, 100)
            }
            else -> {
                showMessage("Scan canceled")
                binding.etSearch.postDelayed({
                    requestSearchFocus()
                }, 100)
            }
        }
    }

    private fun handleProcessPending() {
        val itemCount = viewModel.pendingItems.value.size

        if (itemCount == 0) {
            showMessage("No items to process")
            return
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Process Items")
            .setMessage("Release $itemCount pending item(s)?")
            .setPositiveButton("Yes") { _, _ ->
                setProcessButtonLoading(true)
                viewModel.releaseAllPending()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setProcessButtonLoading(isLoading: Boolean) {
        binding.btnProcess.isEnabled = !isLoading
        binding.btnProcess.text = if (isLoading) "" else "Process"
        binding.progressBarProcess.isVisible = isLoading

        binding.btnScan.isEnabled = !isLoading
        binding.btnFilter.isEnabled = !isLoading
        binding.etSearch.isEnabled = !isLoading
    }

    private fun handleBoxClick(box: Entities.ReleaseBoxHistoryResponse) {
        showBoxInfo(box)
    }

    private fun showBoxInfo(box: Entities.ReleaseBoxHistoryResponse) {
        val formattedDate = formatDate(box.dtModify)

        val info = """
            Box ID: ${box.box}
            Order Number: ${if (box.numOrder.isNotEmpty()) box.numOrder else "Not assigned"}
            Released by: ${box.user}
            Date: $formattedDate
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Release Information")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPendingItemInfo(item: PendingReleaseItem) {
        val info = """
            Box ID: ${item.box}
            Status: Pending release
            Scanned at: ${formatTimestamp(item.scannedAt)}
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Pending Item")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .setNegativeButton("Remove") { _, _ ->
                handleDeletePendingItem(item)
            }
            .show()
    }

    private fun handleDeletePendingItem(item: PendingReleaseItem) {
        viewModel.removePendingItem(item)
        showMessage("Item removed")
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    private fun handleDeleteBox(box: Entities.ReleaseBoxHistoryResponse) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Box")
            .setMessage("Are you sure you want to delete box ${box.box}?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteReleasedBox(box)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openScanner() {
        val intent = Intent(requireContext(), BarcodeScannerActivity::class.java).apply {
            putExtra("ORDER_CODE", "Release Scan")
            putExtra("ORDER_NUMBER", "")
            putExtra("TYPE", "release")
        }
        scannerLauncher.launch(intent)
    }

    private fun handleResume() {
        if (skipNextResume) {
            skipNextResume = false
        } else {
            viewModel.refresh()
        }
    }

    private fun requestSearchFocus() {
        if (!isAdded || isDetached || _binding == null) return

        binding.etSearch.apply {
            requestFocus()
            val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(this.windowToken, 0)
        }
    }

    private fun cleanupFragment() {
        runCatching {
            backPressCallback.remove()

            val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

            _binding = null
        }.onFailure { e ->
            Log.e(TAG, "Error cleaning up fragment", e)
        }
    }

    private fun showMessage(message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (isAdded && !isDetached) {
            Toast.makeText(requireContext(), message, duration).show()
        }
    }

    private fun showSuccessMessage(message: String) {
        showMessage(message)
    }

    private fun showError(message: String) {
        if (!isAdded || isDetached) {
            return
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}