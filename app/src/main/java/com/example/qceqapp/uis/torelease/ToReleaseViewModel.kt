package com.example.qceqapp.uis.torelease

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.data.model.session.UserSession
import com.example.qceqapp.data.network.Service
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ToReleaseViewModel : ViewModel() {

    companion object {
        private const val TAG = "ToReleaseViewModel"
    }

    private val service = Service()

    private val _releasedBoxes = MutableStateFlow<List<Entities.ReleaseBoxHistoryResponse>>(emptyList())
    val releasedBoxes: StateFlow<List<Entities.ReleaseBoxHistoryResponse>> = _releasedBoxes.asStateFlow()

    private val _filteredBoxes = MutableStateFlow<List<Entities.ReleaseBoxHistoryResponse>>(emptyList())
    val filteredBoxes: StateFlow<List<Entities.ReleaseBoxHistoryResponse>> = _filteredBoxes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _releaseResult = MutableStateFlow<ReleaseResult?>(null)
    val releaseResult: StateFlow<ReleaseResult?> = _releaseResult.asStateFlow()

    private var currentSearchQuery = ""
    private var currentFilters = ReleaseFilterDialog.FilterOptions()

    fun loadReleasedBoxes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val result = service.getReleasedBoxes()

                if (result.isSuccess) {
                    val boxesList = result.getOrDefault(emptyList())
                    _releasedBoxes.value = boxesList
                    applyFiltersAndSearch()
                    Log.d(TAG, "Released boxes loaded: ${boxesList.size}")
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Error loading released boxes", error)
                    _error.value = "Error loading released boxes: ${error?.message}"
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading released boxes", e)
                _error.value = "Failed to load released boxes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun releaseBox(idBox: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _releaseResult.value = null

                val qcUser = UserSession.getUsername()
                Log.d(TAG, "Releasing box: $idBox by user: $qcUser")

                val result = service.releaseBox(idBox, qcUser)

                if (result.isSuccess) {
                    val response = result.getOrNull()

                    when (response?.status) {
                        1 -> {
                            _releaseResult.value = ReleaseResult.Success(
                                response.message ?: "Box $idBox released successfully"
                            )
                            Log.d(TAG, "Box released successfully: $idBox")
                            loadReleasedBoxes()
                        }
                        0 -> {
                            _releaseResult.value = ReleaseResult.Error(
                                response.message ?: "Failed to release box $idBox"
                            )
                            Log.w(TAG, "Box release failed: $idBox")
                        }
                        else -> {
                            _releaseResult.value = ReleaseResult.Error(
                                "Unknown response from server"
                            )
                        }
                    }
                } else {
                    val error = result.exceptionOrNull()
                    _releaseResult.value = ReleaseResult.Error(
                        error?.message ?: "Error releasing box $idBox"
                    )
                    Log.e(TAG, "Error releasing box", error)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exception releasing box", e)
                _releaseResult.value = ReleaseResult.Error(
                    e.message ?: "Unknown error releasing box"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchBoxes(query: String) {
        currentSearchQuery = query
        applyFiltersAndSearch()
    }

    fun applyFilters(filters: ReleaseFilterDialog.FilterOptions) {
        currentFilters = filters
        applyFiltersAndSearch()
    }

    fun getAllUsers(): List<String> {
        return _releasedBoxes.value
            .map { it.user }
            .distinct()
            .sorted()
    }

    fun getCurrentFilters(): ReleaseFilterDialog.FilterOptions {
        return currentFilters
    }

    private fun applyFiltersAndSearch() {
        try {
            var filtered = _releasedBoxes.value

            // Aplicar filtro de usuarios
            if (currentFilters.selectedUsers.isNotEmpty()) {
                filtered = filtered.filter { box ->
                    currentFilters.selectedUsers.contains(box.user)
                }
            }

            // Aplicar filtro de fechas
            if (currentFilters.startDate != null || currentFilters.endDate != null) {
                filtered = filtered.filter { box ->
                    val boxDate = parseDate(box.dtModify)
                    boxDate != null && isDateInRange(boxDate, currentFilters.startDate, currentFilters.endDate)
                }
            }
            if (currentFilters.scannedBoxes.isNotEmpty()) {
                filtered = filtered.filter { box ->
                    currentFilters.scannedBoxes.contains(box.box.toString())
                }
            }
            // Aplicar búsqueda de texto
            if (currentSearchQuery.isNotBlank()) {
                filtered = filtered.filter { box ->
                    box.box.toString().contains(currentSearchQuery, ignoreCase = true) ||
                            box.numOrder.contains(currentSearchQuery, ignoreCase = true) ||
                            box.user.contains(currentSearchQuery, ignoreCase = true)
                }
            }

            _filteredBoxes.value = filtered

        } catch (e: Exception) {
            Log.e(TAG, "Error applying filters and search", e)
            _error.value = "Error applying filters: ${e.message}"
        }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            format.parse(dateString)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            null
        }
    }

    private fun isDateInRange(date: Date, startDate: Date?, endDate: Date?): Boolean {
        val dateOnly = getDateOnly(date)
        val start = startDate?.let { getDateOnly(it) }
        val end = endDate?.let { getDateOnly(it) }

        return when {
            start != null && end != null -> dateOnly in start..end
            start != null -> dateOnly >= start
            end != null -> dateOnly <= end
            else -> true
        }
    }

    private fun getDateOnly(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    fun refresh() {
        loadReleasedBoxes()
    }

    fun clearError() {
        _error.value = null
    }

    fun clearReleaseResult() {
        _releaseResult.value = null
    }

    override fun onCleared() {
        try {
            super.onCleared()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCleared", e)
        }
    }

    sealed class ReleaseResult {
        data class Success(val message: String) : ReleaseResult()
        data class Error(val message: String) : ReleaseResult()
    }
}