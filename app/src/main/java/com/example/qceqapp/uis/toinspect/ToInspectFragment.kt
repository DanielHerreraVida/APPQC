package com.example.qceqapp.uis.toinspect

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.databinding.FragmentToInspectBinding
import com.example.qceqapp.uis.QCInspection.QCInspectionActivity
import com.example.qceqapp.uis.scanner.BarcodeScannerActivity
import com.example.qceqapp.utils.GlobalReason
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.qceqapp.utils.GlobalOrder
class ToInspectFragment : Fragment() {
    companion object {
        private const val TAG = "ToInspectFragment"
        private const val NAVIGATION_DELAY = 1000L
    }
    private var _binding: FragmentToInspectBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ToInspectViewModel by viewModels()
    private lateinit var ordersAdapter: OrdersAdapter
    private var isNavigatingToQCBoxes = false
    private var skipNextResume = false
    private lateinit var scannerLauncher: ActivityResultLauncher<Intent>
    private lateinit var filtersLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToInspectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFragment()
    }

    override fun onResume() {
        super.onResume()
        isNavigatingToQCBoxes = false

        handleResume()

        binding.etSearch.postDelayed({
            binding.etSearch.requestFocus()
        }, 100)
    }
    override fun onDestroyView() {
        cleanupFragment()
        super.onDestroyView()
    }
    private fun initializeFragment() {
        runCatching {
            setupActivityResultLaunchers()
            setupRecyclerView()
            setupButtons()
            setupBackPressHandler()
            observeViewModel()
            viewModel.loadDataForToInspect()
            safeRequestFocus()
        }.onFailure { e ->
            showError("Error initializing view: ${e.localizedMessage}")
        }
    }

    private fun setupActivityResultLaunchers() {
        scannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            skipNextResume = true
            handleScanResult(result.resultCode, result.data?.getStringExtra("SCANNED_CODE"))

            binding.etSearch.setText("")
            safeRequestFocus()
        }

        filtersLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "Filters result: ${result.resultCode}")
            skipNextResume = true
            if (result.resultCode == Activity.RESULT_OK) {
                handleFilterResult(result.data)
            }
            safeRequestFocus()
        }

    }

    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter(
            orders = emptyList(),
            growers = emptyList(),
            customers = emptyList(),
            onOrderClick = ::handleOrderClick,
            onQAClick = ::showQAInfo,
            onDeleteClick = ::rejectOrderInspection
        )
        binding.recyclerOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ordersAdapter
            setHasFixedSize(true)
        }
    }
    private fun setupButtons() {
        binding.apply {
            btnFilter.setOnClickListener { openFiltersActivity() }
            btnScan.setOnClickListener { openScanner() }
            etSearch.setOnEditorActionListener { v, actionId, event ->
                val isEnterKey = event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER
                val isImeAction = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                        actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                        actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO

                if (isEnterKey || isImeAction) {
                    val searchText = v.text.toString().trim()
                    if (searchText.isNotEmpty()) {
                        handleManualScan(searchText)
                        v.setText("")
                        true
                    } else {
                        showMessage("Please enter a code")
                        false
                    }
                } else {
                    false
                }
                etSearch.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: android.text.Editable?) {
                        val text = s.toString().trim()
                        if (text.endsWith("\n") || text.endsWith("\r")) {
                            val cleanText = text.replace("\n", "").replace("\r", "").trim()
                            if (cleanText.isNotEmpty()) {
                                etSearch.setText("")
                                handleManualScan(cleanText)
                            }
                        }
                    }
                })

                etSearch.requestFocus()
                etSearch.post {
                    etSearch.requestFocus()
                    val imm =
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.showSoftInput(
                        etSearch,
                        android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
                    )
                }
            }
        }
    }
    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                }
            }
        )
    }
    private fun cleanupFragment() {
        runCatching {
            isNavigatingToQCBoxes = false
            _binding = null
        }.onFailure { e ->
            Log.e(TAG, "Error cleaning up fragment", e)
        }
    }
    private fun observeViewModel() {
        observeFlow(viewModel.isLoading) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.recyclerOrders.isVisible = !isLoading
        }
        observeFlow(viewModel.error) { error ->
            error?.let { showError(it) }
        }
        observeCombinedData()
    }

    private fun observeCombinedData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                kotlinx.coroutines.flow.combine(
                    viewModel.filteredOrders,
                    viewModel.growers,
                    viewModel.customers
                ) { orders, growers, customers ->
                    Triple(orders, growers, customers)
                }.collectLatest { (orders, growers, customers) ->
                    updateAdapter(orders, growers, customers)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error observing combined flows", e)
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

    private fun updateAdapter(
        orders: List<Entities.QCOrderResponse>,
        growers: List<Entities.QCGrowerResponse>,
        customers: List<Entities.QCCustomerResponse>
    ) {
        ordersAdapter.updateData(orders, growers, customers)
        if (orders.isEmpty() && viewModel.orders.value.isNotEmpty()) {
            showMessage("No orders match the filters")
        }
    }
//    private fun handleScanResult(resultCode: Int, scannedCode: String?) {
//        when (resultCode) {
//            Activity.RESULT_OK -> {
//                if (!scannedCode.isNullOrEmpty()) {
//                    binding.etSearch.setText(scannedCode)
//                    handleManualScan(scannedCode)
//                } else {
//                    showError("No code detected. Please try again.")
//                }
//            }
//            Activity.RESULT_CANCELED -> {
//                Log.d(TAG, "Scan was canceled by user")
//            }
//            else -> {
//                showError("Scan canceled.")
//            }
//        }
//    }
private fun handleScanResult(resultCode: Int, scannedCode: String?) {
    when (resultCode) {
        Activity.RESULT_OK -> {
            if (!scannedCode.isNullOrEmpty()) {
                Log.d(TAG, "Scan successful: $scannedCode")
                handleManualScan(scannedCode)
            } else {
                Log.w(TAG, "Scan returned OK but no code")
            }
        }
        Activity.RESULT_CANCELED -> {
            Log.d(TAG, "Scan was canceled by user")
        }
        else -> {
            Log.w(TAG, "Unexpected scan result code: $resultCode")
        }
    }
}

    private fun handleFilterResult(data: Intent?) {
        val filterData = data?.getParcelableExtraCompat<Entities.FilterData>(
            FiltersActivity.EXTRA_FILTER_DATA
        )

        filterData?.let { filter ->
            Log.d(TAG, "Applying filters: $filter")
            viewModel.applyFilters(filter)

            val activeCount = countActiveFilters(filter)
            val message = if (activeCount > 0) {
                "$activeCount filter(s) applied"
            } else {
                "All filters cleared"
            }
            showMessage(message)
        } ?: run {
            Log.w(TAG, "Filter data is null")
        }
    }

    private fun countActiveFilters(filterData: Entities.FilterData): Int {
        return listOf(
            filterData.author,
            filterData.grower,
            filterData.customer,
            filterData.saved,
            filterData.barcodes
        ).count { it.isNotEmpty() }
    }private fun handleOrderClick(order: Entities.QCOrderResponse) {
        runCatching {
            GlobalOrder.clear()
            GlobalOrder.set(order)

            GlobalReason.clear()
            GlobalReason.set(order.reason)

            val barcodeBase = extractBarcodeBase(order.boxId)

            if (barcodeBase == null) {
                navigateToScanOrderForBox(order)
                return
            }

            val isSaved = order.isSaved == "1"
            setLoading(true)

            viewModel.scanBoxToInspect(barcodeBase, isSaved) { scanResult ->
                setLoading(false)
                processScanResult(scanResult, order, isSaved)
            }
        }.onFailure { e ->
            Log.e(TAG, "Error in handleOrderClick", e)
            setLoading(false)
            showError("Error: ${e.localizedMessage}")
        }
    }

    private fun openScannerForOrder(order: Entities.QCOrderResponse) {
        val intent = Intent(requireContext(), BarcodeScannerActivity::class.java).apply {
            putExtra("ORDER_CODE", order.customer ?: "")
            putExtra("ORDER_NUMBER", order.orderNum ?: "")
            putExtra("TYPE", "order_scan")
        }
        scannerLauncher.launch(intent)
    }
    private fun navigateToScanOrderForBox(order: Entities.QCOrderResponse) {
        val intent = Intent(requireContext(), ScanOrder::class.java).apply {
            putExtra("ORDER_CODE", order.customer ?: "")
            putExtra("ORDER_NUMBER", order.orderNum ?: "")
            putExtra("BOX_ID", "")
            putExtra("BOX_ID_TO_INSPECT", "")
        }
        startActivity(intent)
    }


    private fun extractBarcodeBase(boxId: String?): String? {
        if (boxId.isNullOrEmpty()) {
            return null
        }

        return boxId.split(",")
            .firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun processScanResult(
        scanResult: Entities.ScanToInspectResponse?,
        order: Entities.QCOrderResponse,
        isSaved: Boolean
    ) {
        when {
            isSaved && scanResult?.lstSelectedBoxes?.isNotEmpty() == true -> {
                openQCInspection(scanResult, order)
            }
            isSaved && scanResult?.lstSelectedBoxes?.isEmpty() == true -> {
                showMessage("Scan again")
            }
            else -> {
                navigateToScanOrder(order)
            }
        }
    }
    private fun openQCInspection(
        scanResult: Entities.ScanToInspectResponse,
        order: Entities.QCOrderResponse
    ) {
        val codeReaded = scanResult.lstSelectedBoxes?.firstOrNull()?.barcode
            ?: order.boxIdToInspect
            ?: run {
                showError("Invalid box data.")
                return
            }

        showMessage("Opening QC Inspection...")

        val intent = Intent(requireContext(), QCInspectionActivity::class.java).apply {
            putExtra("codeReaded", codeReaded)
            putExtra("idBox", codeReaded)
            putExtra("selectedBoxes", Gson().toJson(scanResult.lstSelectedBoxes))
        }

        startActivity(intent)
    }
    private fun handleManualScan(scannedCode: String) {
        val idBox = scannedCode.trim()

        Log.d(TAG, "handleManualScan - idBox: $idBox, isNavigating: $isNavigatingToQCBoxes")

        if (idBox.isEmpty()) {
            showError("Invalid code")
            return
        }

        navigateToQCBoxesSafely(idBox)
        binding.etSearch.setText("")
    }

    //    private fun handleManualScan(scannedCode: String) {
//        if (isNavigatingToQCBoxes) {
//            return
//        }
//
//        val idBox = scannedCode.trim().takeIf { it.isNotEmpty() } ?: run {
//            showError("Invalid code.")
//            return
//        }
//
//        val matchingOrder = findOrderByBoxId(idBox)
//
//        if (matchingOrder == null) {
//            showError("Box not found in current inventory.")
//            binding.etSearch.setText("")
//            return
//        }
//
//        GlobalOrder.clear()
//        GlobalOrder.set(matchingOrder)
//        GlobalReason.clear()
//        GlobalReason.set(matchingOrder.reason)
//
//        setLoading(true)
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                val service = com.example.qceqapp.data.network.Service()
//
//                if (!performBoxScan(service, idBox)) return@launch
//                if (!checkBoxExistence(service, idBox)) return@launch
//
//                navigateToQCBoxesSafely(idBox)
//
//            } catch (e: CancellationException) {
//                throw e
//            } catch (e: Exception) {
//                showError("Error: ${e.localizedMessage ?: "Unknown error"}")
//            } finally {
//                setLoading(false)
//                resetNavigationFlag()
//                binding.etSearch.setText("")
//            }
//        }
//    }
///
private fun findOrderByBoxId(scannedCode: String): Entities.QCOrderResponse? {
    val currentOrders = viewModel.filteredOrders.value.ifEmpty { viewModel.orders.value }

    return currentOrders.find { order ->
        val boxIds = order.boxId?.split(",")?.map { it.trim() } ?: emptyList()
        val boxIdsToInspect = order.boxIdToInspect?.split(",")?.map { it.trim() } ?: emptyList()

        boxIds.any { it.equals(scannedCode, ignoreCase = true) } ||
                boxIdsToInspect.any { it.equals(scannedCode, ignoreCase = true) }
    }
}
    private suspend fun performBoxScan(service: com.example.qceqapp.data.network.Service, idBox: String): Boolean {
        val result = service.setBoxScan(idBox)
        if (!result.isSuccess || result.getOrNull() != true) {
            showError("Scan failed. Try again.")
            return false
        }
        return true
    }
    private suspend fun checkBoxExistence(service: com.example.qceqapp.data.network.Service, idBox: String): Boolean {
        val result = service.checkOrderByBox(idBox)
        if (!result.isSuccess) {
            showError("Box not registered.")
            return false
        }

        val body = result.getOrNull()?.trim()
        if (body.isNullOrEmpty() || body.contains("Error", ignoreCase = true)) {
            showError("Box not registered.")
            return false
        }

        return true
    }
    private fun navigateToQCBoxesSafely(idBox: String) {

        if (isNavigatingToQCBoxes) {
            Log.w(TAG, "Already navigating, skipping")
            return
        }

        isNavigatingToQCBoxes = true

        val intent = com.example.qceqapp.uis.QCBoxes.QCBoxesActivity.newIntent(
            requireContext(),
            codeReaded = idBox
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
    private fun resetNavigationFlag() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                delay(NAVIGATION_DELAY)
                isNavigatingToQCBoxes = false
            } catch (e: CancellationException) {
                Log.d(TAG, "Reset navigation flag cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting navigation flag", e)
                isNavigatingToQCBoxes = false
            }
        }
    }
    private fun navigateToScanOrder(order: Entities.QCOrderResponse) {
        val intent = Intent(requireContext(), ScanOrder::class.java).apply {
            putExtra("ORDER_CODE", order.customer ?: "")
            putExtra("ORDER_NUMBER", order.orderNum ?: "")
            putExtra("BOX_ID", order.boxId ?: "")
            putExtra("BOX_ID_TO_INSPECT", order.boxIdToInspect ?: "")
        }
        startActivity(intent)
    }
    private fun openFiltersActivity() {
        val currentOrders = viewModel.filteredOrders.value.ifEmpty { viewModel.orders.value }
        val validGrowerCodes = currentOrders.mapNotNull { it.grower }.toSet()
        val validCustomerIds = currentOrders.mapNotNull { it.customerid }.toSet()

        val validAuthors = currentOrders.mapNotNull { it.author }.distinct().sorted()
        val validBoxIds = currentOrders
            .mapNotNull { it.boxId }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val filteredGrowers = viewModel.growers.value.filter { it.groCod in validGrowerCodes }
        val filteredCustomers = viewModel.customers.value.filter { it.codCustomer in validCustomerIds }

        val intent = Intent(requireContext(), FiltersActivity::class.java).apply {
            putExtra(FiltersActivity.EXTRA_FILTER_DATA, viewModel.currentFilter.value)
            putExtra(FiltersActivity.EXTRA_GROWERS, ArrayList(filteredGrowers))
            putExtra(FiltersActivity.EXTRA_CUSTOMERS, ArrayList(filteredCustomers))
            putStringArrayListExtra(FiltersActivity.EXTRA_AUTHORS, ArrayList(validAuthors))
            putStringArrayListExtra(FiltersActivity.EXTRA_BOX_IDS, ArrayList(validBoxIds))
        }

        filtersLauncher.launch(intent)
    }

    private fun openScanner() {
        val intent = Intent(requireContext(), BarcodeScannerActivity::class.java).apply {
            putExtra("ORDER_CODE", "General Scan")
            putExtra("ORDER_NUMBER", "")
            putExtra("TYPE", "general")
        }
        scannerLauncher.launch(intent)
    }
    private fun rejectOrderInspection(order: Entities.QCOrderResponse) {
        val message = if (order.boxIdToInspect.isNullOrEmpty()) {
            "Are you sure you don't want to inspect order: ${order.orderNum}?"
        } else {
            "Are you sure you don't want to inspect the boxes for order: ${order.orderNum}?"
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Reject Inspection")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> performRejectInspection(order) }
            .setNegativeButton("No", null)
            .show()
    }
    private fun performRejectInspection(order: Entities.QCOrderResponse) {
        viewModel.rejectOrderInspection(
            idBox = order.boxIdToInspect ?: "",
            orderNum = order.orderNum ?: "",
            rowNum = order.rowNum ?: ""
        ) { success ->
            if (success) {
                showMessage("Inspection rejected")
                viewModel.loadDataForToInspect()
            } else {
                showError("Error rejecting inspection")
            }
        }
    }
    private fun showQAInfo(order: Entities.QCOrderResponse) {
        val growerName = viewModel.growers.value
            .find { it.groCod == order.grower }
            ?.proVendor ?: "N/A"

        val customerName = viewModel.customers.value
            .find { it.codCustomer == order.customerid }
            ?.custCompany ?: order.customerid ?: "-"

        val info = """
            Order: ${order.orderNum ?: "N/A"}
            Author: ${order.author ?: "N/A"}
            Customer: $customerName
            Grower: $growerName
            Reason: ${order.reason ?: "N/A"}
            Observation: ${order.observation ?: "N/A"}
        """.trimIndent()

        showMessage(info, Toast.LENGTH_LONG)
    }
    private fun setLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.isVisible = isLoading
            recyclerOrders.isEnabled = !isLoading
            btnFilter.isEnabled = !isLoading
            btnScan.isEnabled = !isLoading
        }
    }

    private fun showMessage(message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (isAdded && !isDetached) {
            Toast.makeText(requireContext(), message, duration).show()
        }
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
    private fun handleResume() {
        if (skipNextResume) {
            skipNextResume = false
        } else {
            viewModel.loadDataForToInspect()
        }
    }

    private fun safeRequestFocus() {
        if (isAdded && !isDetached && _binding != null) {
            binding.etSearch.post {
                binding.etSearch.requestFocus()
            }
        }
    }
    private inline fun <reified T : android.os.Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key)
        }
    }
}