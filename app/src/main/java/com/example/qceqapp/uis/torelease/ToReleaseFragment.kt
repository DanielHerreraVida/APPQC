package com.example.qceqapp.uis.torelease

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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

class ToReleaseFragment : Fragment() {

    companion object {
        private const val TAG = "ToReleaseFragment"
    }

    private var _binding: FragmentToReleaseBinding? = null
    private val binding get() = _binding!!
    private var currentFilterDialog: ReleaseFilterDialog? = null

    private val viewModel: ToReleaseViewModel by viewModels()
    private lateinit var adapter: ReleaseAdapter
    private var skipNextResume = false

    private lateinit var scannerLauncher: ActivityResultLauncher<Intent>

    // ✅ Callback para bloquear el botón de retroceso
    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // ✅ No hacer nada - bloquear completamente el botón back
            // Opcionalmente puedes mostrar un mensaje
            // showMessage("Back button is disabled in this screen")
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

        // ✅ Registrar el callback del botón back
        setupBackPressHandler()

        initializeFragment()
    }

    override fun onResume() {
        super.onResume()
        handleResume()
        // Solicitar foco cuando el fragment vuelve a estar activo
        requestSearchFocus()
    }

    override fun onDestroyView() {
        cleanupFragment()
        super.onDestroyView()
    }

    // ✅ Método para bloquear el botón de retroceso
    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressCallback
        )
    }

    private fun initializeFragment() {
        runCatching {
            setupActivityResultLaunchers()
            setupRecyclerView()
            setupSearchListener()
            setupButtons()
            observeViewModel()
            viewModel.loadReleasedBoxes()

            // Solicitar foco inicial después de un pequeño delay
            binding.etSearch.postDelayed({
                requestSearchFocus()
            }, 300)
        }.onFailure { e ->
            showError("Error initializing view: ${e.localizedMessage}")
        }
    }

    private fun setupActivityResultLaunchers() {
        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            skipNextResume = true

            // Verificar si el escaneo viene de los filtros
            val scanType = result.data?.getStringExtra("TYPE")

            if (scanType == "filter") {
                // Escaneo desde filtros
                if (result.resultCode == Activity.RESULT_OK) {
                    val scannedCode = result.data?.getStringExtra("SCANNED_CODE")
                    scannedCode?.let { code ->
                        currentFilterDialog?.addScannedBox(code)
                    }
                }
            } else {
                // Escaneo normal para release
                handleScanResult(result.resultCode, result.data?.getStringExtra("SCANNED_CODE"))
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ReleaseAdapter(
            boxes = emptyList(),
            onBoxClick = ::handleBoxClick,
            onUserClick = ::showBoxInfo,
            onDeleteClick = ::handleDeleteBox
        )

        binding.recyclerOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ToReleaseFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchListener() {
        // Búsqueda en tiempo real mientras se escribe
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString()
            if (query.isEmpty()) {
                viewModel.searchBoxes("")
            } else {
                viewModel.searchBoxes(query)
            }
        }

        // Manejo del botón Enter/Done del teclado y pistolas de escaneo
        binding.etSearch.setOnEditorActionListener { v, actionId, event ->
            // Detectar Enter del teclado físico, pistola de escaneo o botón Done
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
    }

    private fun showFilterDialog() {
        val allUsers = viewModel.getAllUsers()

        if (allUsers.isEmpty()) {
            showMessage("No data available to filter")
            return
        }

        currentFilterDialog = ReleaseFilterDialog(
            context = requireContext(),
            allUsers = allUsers,
            currentFilters = viewModel.getCurrentFilters(),
            onApplyFilters = { filters ->
                viewModel.applyFilters(filters)

                // Contar filtros activos
                var filterCount = 0
                if (filters.selectedUsers.isNotEmpty()) filterCount++
                if (filters.startDate != null || filters.endDate != null) filterCount++
                if (filters.scannedBoxes.isNotEmpty()) filterCount++

                if (filterCount > 0) {
                    showMessage("$filterCount filter(s) applied")
                } else {
                    showMessage("All filters cleared")
                }

                currentFilterDialog = null
            },
            onScanRequested = {
                openFilterScanner()
            }
        )

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

    private fun observeViewModel() {
        observeFlow(viewModel.isLoading) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.recyclerOrders.isVisible = !isLoading
        }

        observeFlow(viewModel.error) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        observeFlow(viewModel.filteredBoxes) { boxes ->
            updateAdapter(boxes)
        }

        // Observar resultado del release
        observeFlow(viewModel.releaseResult) { result ->
            result?.let {
                when (it) {
                    is ToReleaseViewModel.ReleaseResult.Success -> {
                        showSuccessMessage(it.message)
                        binding.etSearch.text?.clear()
                        // Volver a solicitar el foco después de limpiar
                        requestSearchFocus()
                    }
                    is ToReleaseViewModel.ReleaseResult.Error -> {
                        showError(it.message)
                        binding.etSearch.text?.clear()
                        requestSearchFocus()
                    }
                }
                viewModel.clearReleaseResult()
            }
        }
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

    private fun updateAdapter(boxes: List<Entities.ReleaseBoxHistoryResponse>) {
        adapter.updateData(boxes)
        if (boxes.isEmpty() && viewModel.releasedBoxes.value.isNotEmpty()) {
            showMessage("No boxes match the search")
        }
    }

    private fun handleScanResult(resultCode: Int, scannedCode: String?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                if (!scannedCode.isNullOrEmpty()) {
                    binding.etSearch.setText(scannedCode)
                    handleManualScan(scannedCode)
                } else {
                    showError("No code detected. Please try again.")
                    requestSearchFocus()
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Scan was canceled by user")
                requestSearchFocus()
            }
            else -> {
                showError("Scan canceled.")
                requestSearchFocus()
            }
        }
    }

    private fun handleManualScan(scannedCode: String) {
        val code = scannedCode.trim()
        if (code.isEmpty()) {
            showError("Invalid code.")
            return
        }

        // Mostrar diálogo de confirmación antes de liberar el box
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Release Box")
            .setMessage("Do you want to release box: $code?")
            .setPositiveButton("Yes") { _, _ ->
                // Llamar al release
                viewModel.releaseBox(code)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                binding.etSearch.text?.clear()
                requestSearchFocus()
            }
            .show()
    }

    private fun handleBoxClick(box: Entities.ReleaseBoxHistoryResponse) {
        // Mostrar información del box al hacer clic
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

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun handleDeleteBox(box: Entities.ReleaseBoxHistoryResponse) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Release Record")
            .setMessage("Are you sure you want to delete the release record for box: ${box.box}?")
            .setPositiveButton("Yes") { _, _ ->
                showMessage("Delete functionality not implemented yet")
            }
            .setNegativeButton("No", null)
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

    /**
     * Solicita el foco en el campo de búsqueda y muestra el teclado
     */
    private fun requestSearchFocus() {
        if (!isAdded || isDetached || _binding == null) return

        binding.etSearch.apply {
            requestFocus()
            // Mostrar el teclado virtual
            val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun cleanupFragment() {
        runCatching {
            // ✅ Remover el callback del botón back
            backPressCallback.remove()

            // Ocultar el teclado antes de destruir
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
        if (!isAdded || isDetached) return

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Success")
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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