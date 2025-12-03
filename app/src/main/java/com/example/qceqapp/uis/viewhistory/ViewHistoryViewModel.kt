package com.example.qceqapp.uis.viewhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.data.network.Service
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class ViewHistoryViewModel : ViewModel() {

    private val service = Service()

    private val _customers = MutableStateFlow<List<Entities.QCCustomerResponse>>(emptyList())
    val customers: StateFlow<List<Entities.QCCustomerResponse>> = _customers.asStateFlow()

    private val _growers = MutableStateFlow<List<Entities.QCGrowerResponse>>(emptyList())
    val growers: StateFlow<List<Entities.QCGrowerResponse>> = _growers.asStateFlow()

    private val _history = MutableStateFlow<List<Entities.QCHistoryResponse>>(emptyList())
    val history: StateFlow<List<Entities.QCHistoryResponse>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var lastLoadTime: Long = 0
    private val CACHE_DURATION = 30_000L

    fun loadHistoryData(forceRefresh: Boolean = false) {
        if (_isLoading.value) return

        val now = System.currentTimeMillis()
        if (!forceRefresh && (now - lastLoadTime) < CACHE_DURATION && _history.value.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                supervisorScope {
                    val customersDeferred = async {
                        try {
                            service.getCustomers(1)
                        } catch (e: Exception) {
                            Result.failure<List<Entities.QCCustomerResponse>>(e)
                        }
                    }

                    val growersDeferred = async {
                        try {
                            service.getGrowers(1)
                        } catch (e: Exception) {
                            Result.failure<List<Entities.QCGrowerResponse>>(e)
                        }
                    }

                    val historyDeferred = async {
                        try {
                            service.getQCHistory()
                        } catch (e: Exception) {
                            Result.failure<List<Entities.QCHistoryResponse>>(e)
                        }
                    }
                    val customersResult = customersDeferred.await()
                    val growersResult = growersDeferred.await()
                    val historyResult = historyDeferred.await()

                    customersResult.onSuccess { customers ->
                        _customers.value = customers
                    }.onFailure { error ->
                        android.util.Log.e("ViewHistoryVM", "Error loading customers: ${error.message}")
                    }

                    growersResult.onSuccess { growers ->
                        _growers.value = growers
                    }.onFailure { error ->
                        android.util.Log.e("ViewHistoryVM", "Error loading growers: ${error.message}")
                    }

                    historyResult.onSuccess { history ->
                        _history.value = history
                        lastLoadTime = System.currentTimeMillis()
                    }.onFailure { error ->
                        _error.value = "Error loading history: ${error.message}"
                        android.util.Log.e("ViewHistoryVM", "Error loading history: ${error.message}")
                    }
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error loading history"
                android.util.Log.e("ViewHistoryVM", "Critical error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }
}