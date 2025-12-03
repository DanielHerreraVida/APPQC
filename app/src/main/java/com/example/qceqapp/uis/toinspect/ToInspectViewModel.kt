package com.example.qceqapp.uis.toinspect

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.data.model.session.UserSession
import com.example.qceqapp.data.network.Service
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ToInspectViewModel : ViewModel() {

    companion object {
        private const val TAG = "ToInspectViewModel"
        private const val DEFAULT_QA_REASON = "Rejected from mobile"
    }

    private val service = Service()

    private val _customers = MutableStateFlow<List<Entities.QCCustomerResponse>>(emptyList())
    val customers: StateFlow<List<Entities.QCCustomerResponse>> = _customers.asStateFlow()

    private val _growers = MutableStateFlow<List<Entities.QCGrowerResponse>>(emptyList())
    val growers: StateFlow<List<Entities.QCGrowerResponse>> = _growers.asStateFlow()

    private val _orders = MutableStateFlow<List<Entities.QCOrderResponse>>(emptyList())
    val orders: StateFlow<List<Entities.QCOrderResponse>> = _orders.asStateFlow()

    private val _filteredOrders = MutableStateFlow<List<Entities.QCOrderResponse>>(emptyList())
    val filteredOrders: StateFlow<List<Entities.QCOrderResponse>> = _filteredOrders.asStateFlow()

    private val _currentFilter = MutableStateFlow(Entities.FilterData())
    val currentFilter: StateFlow<Entities.FilterData> = _currentFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadDataForToInspect() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val customersDeferred = async { loadCustomers() }
                val growersDeferred = async { loadGrowers() }
                val ordersDeferred = async { loadOrders() }
                val customersSuccess = customersDeferred.await()
                val growersSuccess = growersDeferred.await()
                val ordersSuccess = ordersDeferred.await()

                if (!ordersSuccess) {
                    _error.value = "Failed to load orders. Please try again."
                }

                if (!customersSuccess) {
                }

                if (!growersSuccess) {
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadCustomers(): Boolean {
        return try {
            val result = service.getCustomers(0)
            if (result.isSuccess) {
                val customersList = result.getOrDefault(emptyList())
                _customers.value = customersList
                true
            } else {
                val error = result.exceptionOrNull()
                _error.value = "Error loading customers: ${error?.message}"
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "Failed to load customers: ${e.message}"
            false
        }
    }

    private suspend fun loadGrowers(): Boolean {
        return try {
            val result = service.getGrowers(0)
            if (result.isSuccess) {
                val growersList = result.getOrDefault(emptyList())
                _growers.value = growersList
                true
            } else {
                val error = result.exceptionOrNull()
                _error.value = "Error loading growers: ${error?.message}"
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "Failed to load growers: ${e.message}"
            false
        }
    }

    private suspend fun loadOrders(): Boolean {
        return try {
            val result = service.getQCOrders()
            if (result.isSuccess) {
                val ordersList = result.getOrDefault(emptyList())
                _orders.value = ordersList
                _filteredOrders.value = ordersList
                Log.d(TAG, "Orders loaded: ${ordersList.size}")
                true
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Error loading orders", error)
                _error.value = "Error loading orders: ${error?.message}"
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading orders", e)
            _error.value = "Failed to load orders: ${e.message}"
            false
        }
    }

    fun applyFilters(filterData: Entities.FilterData) {
        try {
            _currentFilter.value = filterData
            if (filterData.isEmpty()) {
                _filteredOrders.value = _orders.value
                return
            }

            val filtered = _orders.value.filter { order ->
                matchesFilter(order, filterData)
            }
            _filteredOrders.value = filtered
        } catch (e: Exception) {
            _error.value = "Error applying filters: ${e.message}"
            _filteredOrders.value = _orders.value
        }
    }
//    private fun matchesFilter(order: Entities.QCOrderResponse, filterData: Entities.FilterData): Boolean {
//        try {
//            if (filterData.author.isNotEmpty()) {
//                val orderAuthor = order.author ?: ""
//                if (!orderAuthor.equals(filterData.author, ignoreCase = true)) {
//                    return false
//                }
//            }
//            if (filterData.grower.isNotEmpty()) {
//                val selectedGrowers = filterData.grower.split(",").map { it.trim() }
//                val orderGrower = order.grower ?: ""
//                val growerMatches = selectedGrowers.any { g ->
//                    orderGrower.equals(g, ignoreCase = true) ||  // si grower trae nombre
//                            orderGrower.contains(g, ignoreCase = true)   // o parte del código
//                }
//                if (!growerMatches) return false
//            }
//
//            if (filterData.customer.isNotEmpty()) {
//                val selectedCustomers = filterData.customer.split(",").map { it.trim() }
//                val orderCustomerName = order.customer ?: ""
//                val orderCustomerId = order.customerid ?: ""
//
//                val customerMatches = selectedCustomers.any { c ->
//                    orderCustomerId.equals(c, ignoreCase = true) ||
//                            orderCustomerName.equals(c, ignoreCase = true) ||
//                            orderCustomerName.contains(c, ignoreCase = true)
//                }
//                if (!customerMatches) return false
//            }
//            if (filterData.saved.isNotEmpty()) {
//                val orderSaved = order.isSaved ?: order.saved ?: ""
//                if (!orderSaved.equals(filterData.saved, ignoreCase = true)) {
//                    return false
//                }
//            }
//
//            if (filterData.barcodes.isNotEmpty()) {
//                val scannedBarcodes = filterData.barcodes.split(",").map { it.trim() }
//                val orderBoxId = order.boxId ?: ""
//                val orderBoxIdToInspect = order.boxIdToInspect ?: ""
//
//                val barcodeMatches = scannedBarcodes.any { barcode ->
//                    orderBoxId.contains(barcode, ignoreCase = true) ||
//                            orderBoxIdToInspect.contains(barcode, ignoreCase = true)
//                }
//                if (!barcodeMatches) return false
//            }
//
//            return true
//        } catch (e: Exception) {
//            Log.e("ToInspectViewModel", "Filter error", e)
//            return false
//        }
//    }
private fun matchesFilter(order: Entities.QCOrderResponse, filterData: Entities.FilterData): Boolean {
    try {
        if (filterData.author.isNotEmpty()) {
            val orderAuthor = order.author ?: ""
            if (!orderAuthor.equals(filterData.author, ignoreCase = true)) {
                return false
            }
        }

        if (filterData.grower.isNotEmpty()) {
            val selectedGrowers = filterData.grower.split(",").map { it.trim() }
            val orderGrower = order.grower ?: ""
            val growerMatches = selectedGrowers.any { g ->
                orderGrower.equals(g, ignoreCase = true) ||
                        orderGrower.contains(g, ignoreCase = true)
            }
            if (!growerMatches) return false
        }

        if (filterData.customer.isNotEmpty()) {
            val selectedCustomers = filterData.customer.split(",").map { it.trim() }
            val orderCustomerName = order.customer ?: ""
            val orderCustomerId = order.customerid ?: ""

            val customerMatches = selectedCustomers.any { c ->
                orderCustomerId.equals(c, ignoreCase = true) ||
                        orderCustomerName.equals(c, ignoreCase = true) ||
                        orderCustomerName.contains(c, ignoreCase = true)
            }
            if (!customerMatches) return false
        }

        if (filterData.saved.isNotEmpty()) {
            val orderSaved = order.isSaved ?: order.saved ?: ""
            if (!orderSaved.equals(filterData.saved, ignoreCase = true)) {
                return false
            }
        }

        // ✅ CORRECCIÓN: Dividir los boxIds y comparar exactamente
        if (filterData.barcodes.isNotEmpty()) {
            val scannedBarcodes = filterData.barcodes.split(",").map { it.trim() }

            // Dividir los boxIds en una lista individual
            val orderBoxIds = order.boxId?.split(",")?.map { it.trim() } ?: emptyList()
            val orderBoxIdsToInspect = order.boxIdToInspect?.split(",")?.map { it.trim() } ?: emptyList()

            // Comparar exactamente cada código escaneado con cada boxId individual
            val barcodeMatches = scannedBarcodes.any { scannedBarcode ->
                orderBoxIds.any { it.equals(scannedBarcode, ignoreCase = true) } ||
                        orderBoxIdsToInspect.any { it.equals(scannedBarcode, ignoreCase = true) }
            }

            if (!barcodeMatches) return false
        }

        return true
    } catch (e: Exception) {
        Log.e("ToInspectViewModel", "Filter error", e)
        return false
    }
}
    fun scanBoxToInspect(
        boxId: String,
        isSaved: Boolean,
        onResult: (Entities.ScanToInspectResponse?) -> Unit
    ) {
        if (boxId.isBlank()) {
            _error.value = "Box ID cannot be empty"
            onResult(null)
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val result = service.scanToInspect(boxId)

                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null) {
                        onResult(data)
                    } else {
                        _error.value = "No data found for box"
                        onResult(null)
                    }
                } else {
                    val error = result.exceptionOrNull()
                    _error.value = "Error scanning box: ${error?.message ?: "Unknown error"}"
                    onResult(null)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message ?: "Unknown error"}"
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun rejectOrderInspection(
        idBox: String,
        orderNum: String,
        rowNum: String,
        qaReason: String = DEFAULT_QA_REASON,
        onResult: (Boolean) -> Unit
    ) {
        if (orderNum.isBlank()) {
            _error.value = "Order number is required"
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val qaInspector = getUsernameOrDefault()
                val result = service.rejectBox(
                    idBox = idBox,
                    orderNum = orderNum,
                    rowNum = rowNum,
                    qaInspector = qaInspector,
                    qaReason = qaReason
                )
                if (result.isSuccess) {
                    onResult(true)
                } else {
                    val error = result.exceptionOrNull()
                    _error.value = "Error rejecting box: ${error?.message ?: "Unknown error"}"
                    onResult(false)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message ?: "Unknown error"}"
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getUsernameOrDefault(): String {
        return try {
            UserSession.getUsername() ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting username from session", e)
            "Unknown"
        }
    }

    fun clearFilters() {
        try {
            _currentFilter.value = Entities.FilterData()
            _filteredOrders.value = _orders.value
        } catch (e: Exception) {
            _error.value = "Error clearing filters"
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        try {
            super.onCleared()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCleared", e)
        }
    }
}