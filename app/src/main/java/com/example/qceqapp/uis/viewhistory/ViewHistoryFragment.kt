package com.example.qceqapp.uis.viewhistory

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qceqapp.R
import com.google.gson.Gson
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.databinding.FragmentViewHistoryBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.qceqapp.uis.QCOrderSent.QCOrderSentActivity
import com.example.qceqapp.uis.QCOrderSent.SendInspectionActivity

class ViewHistoryFragment : Fragment() {

    private var _binding: FragmentViewHistoryBinding? = null
    private val binding get() = _binding!!
    private var isProcessingClick = false

    private val viewModel: ViewHistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter
    private val inspectionSelectedList = mutableListOf<String>()
    private val filterDictionary = mutableMapOf<String, String>()

    private var baseHistoryItem: Entities.QCHistoryResponse? = null
    private var isSelectionMode = false
    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("Filters")?.let { filters ->
                android.util.Log.d("ViewHistoryFragment", "Filters received: $filters")

                val newFilters = HistoryAdapter.deserializeFilterDictionary(filters)
                filterDictionary.clear()
                loadFilters()
                filterDictionary.putAll(newFilters)

                val allFiltersEmpty = filterDictionary.values.all { it.isEmpty() }
                if (allFiltersEmpty) {
                    historyAdapter.filter.filter("")
                    Snackbar.make(binding.root, "All filters cleared", Snackbar.LENGTH_LONG).show()
                } else {
                    applyFilters()
                    val activeFilters = filterDictionary.count { it.value.isNotEmpty() }
                    Snackbar.make(
                        binding.root,
                        "Filters applied ($activeFilters active)",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            android.util.Log.d("ViewHistoryFragment", "Filter cancelled")
        }
    }

    private val inspectionDetailsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadHistoryData()
        }
    }

    private val sendInspectionsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("InspectionsSent")?.let { status ->
                if (status == "OK") {
                    inspectionSelectedList.clear()
                    baseHistoryItem = null
                    updateInspectionBadge()
                    viewModel.loadHistoryData()
                    Toast.makeText(requireContext(), "Inspections sent successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        loadFilters()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFloatingActionButton()
        observeViewModel()
        viewModel.loadHistoryData()
        setupBackPressHandler()
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

    private fun loadFilters() {
        filterDictionary["Author"] = ""
        filterDictionary["Fechai"] = ""
        filterDictionary["Fechaf"] = ""
        filterDictionary["Grower"] = ""
        filterDictionary["Customer"] = ""
        filterDictionary["InspectionState"] = ""
        filterDictionary["AWB"] = ""
        filterDictionary["OrderNum"] = ""
        filterDictionary["Barcodes"] = ""
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            originalList = emptyList(),
            inspectionSelected = inspectionSelectedList,
            onItemClick = { item ->
                if (isSelectionMode) {
                    handleItemLongClick(item)
                } else {
                    handleItemClick(item)
                }
            }
        )

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFloatingActionButton() {
        binding.RLFloatingBtn.visibility = View.GONE

        binding.fab.setOnClickListener {
            if (inspectionSelectedList.isNotEmpty()) {
                sendInspections()
            } else {
                Toast.makeText(
                    requireContext(),
                    "You must select at least one inspection to send",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.isVisible = isLoading
                binding.recyclerViewHistory.isVisible = !isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.history.collectLatest { history ->
                if (history.isNotEmpty()) {
                    android.util.Log.d("ViewHistoryFragment", "Received ${history.size} items from ViewModel")
                    historyAdapter.updateData(history)
                    applyFiltersIfNeeded()
                }
            }
        }
    }

    private fun handleItemClick(item: Entities.QCHistoryResponse) {
        if (isProcessingClick) {
            Toast.makeText(requireContext(), "Processing, please wait...", Toast.LENGTH_SHORT).show()
            return
        }
        isProcessingClick = true
        binding.progressBar.isVisible = true
        binding.recyclerViewHistory.isEnabled = false

        if (item.inspectionStatus.isNullOrEmpty()) {
            val msg = "The order ${item.orderNum} was Not Inspected"
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            resetClickState()
            return
        }

        val boxIds = item.boxId?.split(',')?.map { it.trim() } ?: emptyList()
        if (boxIds.isEmpty()) {
            Toast.makeText(requireContext(), "No associated boxes were found", Toast.LENGTH_SHORT).show()
            resetClickState()
            return
        }

        val barcodeBase = boxIds[0]
        if (barcodeBase.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid cashier code", Toast.LENGTH_SHORT).show()
            resetClickState()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = com.example.qceqapp.data.network.Service()
                val result = service.scanToInspect(barcodeBase)

                result.onSuccess { model ->
                    val boxes = model.lstSelectedBoxes

                    if (!boxes.isNullOrEmpty()) {
                        val intent = Intent(requireContext(), QCOrderSentActivity::class.java).apply {
                            putExtra("codeReaded", barcodeBase)
                            putExtra("selectedBoxes", Gson().toJson(boxes))
                            putExtra("infobx", item.boxIdToInspect)
                            val awbFormatted = "${item.bxAWB?.takeLast(4)}-${item.bxTELEX}"
                            putExtra("AWB", awbFormatted)
                            putExtra("ORDER", item.orderNum)
                            putExtra("DATE_SENT", item.inspectionTime)
                        }
                        inspectionDetailsLauncher.launch(intent)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error trying to get Selected Boxes, please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.onFailure { e ->
                    Snackbar.make(
                        binding.root,
                        "Error loading boxes: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error: ${e.localizedMessage}",
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                resetClickState()
            }
        }
    }

    private fun resetClickState() {
        binding.progressBar.isVisible = false
        binding.recyclerViewHistory.isEnabled = true
        isProcessingClick = false
    }


    private fun handleItemLongClick(item: Entities.QCHistoryResponse) {
        if (!isSelectionMode) return
        if (inspectionSelectedList.isEmpty()) {
            baseHistoryItem = item
        }
        val base = baseHistoryItem
        if (base != null) {
            when {
                item.grower != base.grower -> {
                    Toast.makeText(
                        requireContext(),
                        "Only inspections from the same property should be selected",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                item.bxAWB != base.bxAWB -> {
                    Toast.makeText(
                        requireContext(),
                        "Only inspections from the same GUIDE should be selected",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                item.bxTELEX != base.bxTELEX -> {
                    Toast.makeText(
                        requireContext(),
                        "Only inspections from the same GUIDE Date should be selected",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }
        }
        item.boxIdToInspect?.let { id ->
            if (inspectionSelectedList.contains(id)) {
                inspectionSelectedList.remove(id)
            } else {
                inspectionSelectedList.add(id)
            }
            updateInspectionBadge()
            historyAdapter.notifyDataSetChanged()
        }
    }

    private fun updateInspectionBadge() {
        val count = inspectionSelectedList.size
        binding.TVInspBadge.text = count.toString()
    }

    private fun sendInspections() {
        val currentList = viewModel.history.value

        val inspectionsToSend = currentList.filter { item ->
            inspectionSelectedList.contains(item.boxIdToInspect)
        }

        if (inspectionsToSend.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "There are no inspections to send",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val gson = Gson()
        val inspectionsJson = gson.toJson(inspectionsToSend)

        val intent = Intent(requireContext(), SendInspectionActivity::class.java).apply {
            putExtra("InspectionsS", inspectionsJson)
        }

        sendInspectionsLauncher.launch(intent)
    }

    private fun applyFiltersIfNeeded() {
        if (filterDictionary.values.any { it.isNotEmpty() }) {
            applyFilters()
        }
    }

    private fun applyFilters() {
        val filterJson = HistoryAdapter.serializeFilterDictionary(filterDictionary)
        historyAdapter.filter.filter(filterJson)
    }

    fun applyFilter(filterMap: Map<String, String>) {
        filterDictionary.putAll(filterMap)
        applyFilters()
    }

    fun clearFilters() {
        filterDictionary.clear()
        loadFilters()
        historyAdapter.filter.filter("")
        Snackbar.make(binding.root, "All filters cleared", Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.qch_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_filter -> {
                openFilterActivity()
                true
            }
            R.id.menu_select -> {
                toggleSelectionMode(item)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFilterActivity() {
        val authors = viewModel.history.value
            .mapNotNull { it.author }
            .distinct()
            .sorted()

        val intent = Intent(requireContext(), FilterQCHistoryActivity::class.java).apply {
            putExtra("Filters", HistoryAdapter.serializeFilterDictionary(filterDictionary))
            putExtra("customers", Gson().toJson(viewModel.customers.value))
            putExtra("growers", Gson().toJson(viewModel.growers.value))
            putExtra("authors", Gson().toJson(authors))
            putExtra("historyList", Gson().toJson(viewModel.history.value))
        }
        filterLauncher.launch(intent)
    }

    private fun toggleSelectionMode(menuItem: MenuItem) {
        isSelectionMode = !isSelectionMode

        if (isSelectionMode) {
            binding.RLFloatingBtn.visibility = View.VISIBLE
            menuItem.setIcon(R.drawable.completed_task)
            Toast.makeText(requireContext(), "Selection mode ON", Toast.LENGTH_SHORT).show()
        } else {
            binding.RLFloatingBtn.visibility = View.GONE
            menuItem.setIcon(R.drawable.completed_task_off)
            inspectionSelectedList.clear()
            baseHistoryItem = null
            updateInspectionBadge()
            historyAdapter.notifyDataSetChanged()
            Toast.makeText(requireContext(), "Selection mode OFF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        isProcessingClick = false
        binding.recyclerViewHistory.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}