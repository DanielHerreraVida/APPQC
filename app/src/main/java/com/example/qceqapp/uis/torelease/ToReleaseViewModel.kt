package com.example.qceqapp.uis.torelease

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qceqapp.data.local.PendingReleaseEntity
import com.example.qceqapp.data.local.QCEQDatabase
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

    // DAO obtenido del singleton ya inicializado por QCEQApplication.
    // No requiere Context en el ViewModel — el constructor permanece sin cambios.
    private val dao = QCEQDatabase.getInstance().pendingReleaseDao()

    init {
        loadPendingItemsFromDb()
    }

    private val _releasedBoxes = MutableStateFlow<List<Entities.ReleaseBoxHistoryResponse>>(emptyList())
    val releasedBoxes: StateFlow<List<Entities.ReleaseBoxHistoryResponse>> = _releasedBoxes.asStateFlow()

    private val _filteredBoxes = MutableStateFlow<List<Entities.ReleaseBoxHistoryResponse>>(emptyList())
    val filteredBoxes: StateFlow<List<Entities.ReleaseBoxHistoryResponse>> = _filteredBoxes.asStateFlow()

    private val _allPendingItems = MutableStateFlow<List<PendingReleaseItem>>(emptyList())

    private val _pendingItems = MutableStateFlow<List<PendingReleaseItem>>(emptyList())
    val pendingItems: StateFlow<List<PendingReleaseItem>> = _pendingItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _warning = MutableStateFlow<String?>(null)
    val warning: StateFlow<String?> = _warning.asStateFlow()

    private val _playErrorSound = MutableStateFlow(false)
    val playErrorSound: StateFlow<Boolean> = _playErrorSound.asStateFlow()

    private val _releaseResult = MutableStateFlow<ReleaseResult?>(null)
    val releaseResult: StateFlow<ReleaseResult?> = _releaseResult.asStateFlow()

    private val _duplicateMessage = MutableStateFlow<String?>(null)
    val duplicateMessage: StateFlow<String?> = _duplicateMessage.asStateFlow()

    private var currentSearchQuery = ""
    private var currentFilters = ReleaseFilterDialog.FilterOptions()
    private var currentPendingFilters = ReleaseFilterDialog.FilterOptions()

    /**
     * Carga los pending items desde SQLite al iniciar el ViewModel.
     * Restaura [_allPendingItems] si la app fue cerrada antes de procesar los items.
     */
    private fun loadPendingItemsFromDb() {
        viewModelScope.launch {
            try {
                val entities = dao.getAll()
                if (entities.isNotEmpty()) {
                    val items = entities.map { PendingReleaseItem(box = it.box, scannedAt = it.scannedAt) }
                    _allPendingItems.value = items
                    applyPendingFiltersInternal(currentPendingFilters)
                    Log.d(TAG, "SQLite: loaded ${items.size} pending items")
                } else {
                    Log.d(TAG, "SQLite: no pending items in DB")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "SQLite ERROR: failed to load pending items: ${e.message}")
            }
        }
    }

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
                } else {
                    val error = result.exceptionOrNull()
                    _error.value = "Error loading released boxes: ${error?.message}"
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Failed to load released boxes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addPendingItem(boxCode: String) {
        val allItems = _allPendingItems.value.toMutableList()

        if (allItems.any { it.box.equals(boxCode, ignoreCase = true) }) {
            _duplicateMessage.value = "Esta caja ya está escaneada"
            _playErrorSound.value = true
            return
        }

        val alreadyReleased = _releasedBoxes.value.any {
            it.box.toString().equals(boxCode, ignoreCase = true)
        }

        if (alreadyReleased) {
            _duplicateMessage.value = "Esta caja ya fue escaneada anteriormente"
            _playErrorSound.value = true
            return
        }

        val newItem = PendingReleaseItem(box = boxCode)
        allItems.add(0, newItem)
        _allPendingItems.value = allItems

        applyPendingFiltersInternal(currentPendingFilters)

        // Persistir en SQLite — operación async que NO bloquea la UI
        viewModelScope.launch {
            try {
                dao.insert(PendingReleaseEntity(box = newItem.box, scannedAt = newItem.scannedAt))
                Log.d(TAG, "SQLite: insert pending item -> box=${newItem.box}, scannedAt=${newItem.scannedAt}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "SQLite ERROR: insert failed for box=${newItem.box}: ${e.message}")
            }
        }
    }

    fun clearDuplicateMessage() {
        _duplicateMessage.value = null
    }

    fun clearErrorSoundFlag() {
        _playErrorSound.value = false
    }

    fun removePendingItem(item: PendingReleaseItem) {
        val allItems = _allPendingItems.value.toMutableList()
        allItems.remove(item)
        _allPendingItems.value = allItems

        applyPendingFiltersInternal(currentPendingFilters)

        // Eliminar de SQLite — operación async que NO bloquea la UI
        viewModelScope.launch {
            try {
                dao.deleteByBox(item.box)
                Log.d(TAG, "SQLite: deleted pending item -> box=${item.box}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "SQLite ERROR: delete failed for box=${item.box}: ${e.message}")
            }
        }
    }

    fun clearPendingItems() {
        _allPendingItems.value = emptyList()
        _pendingItems.value = emptyList()

        // Limpiar SQLite — operación async que NO bloquea la UI
        viewModelScope.launch {
            try {
                dao.deleteAll()
                Log.d(TAG, "SQLite: cleared all pending items")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "SQLite ERROR: clearAll failed: ${e.message}")
            }
        }
    }

    fun deleteReleasedBox(box: Entities.ReleaseBoxHistoryResponse) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val boxNumber = box.box.toInt()
                val username = UserSession.getUsername()

                val result = service.deleteReleasedBox(boxNumber, username)

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response?.status == 1) {
                        loadReleasedBoxes()
                        _releaseResult.value = ReleaseResult.Success(
                            "Box ${box.box} deleted successfully"
                        )
                    } else {
                        _releaseResult.value = ReleaseResult.Error(
                            response?.message ?: "Failed to delete box"
                        )
                    }
                } else {
                    val error = result.exceptionOrNull()
                    _releaseResult.value = ReleaseResult.Error(
                        "Error deleting box: ${error?.message}"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _releaseResult.value = ReleaseResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun releaseAllPending() {
        viewModelScope.launch {
            val items = _allPendingItems.value

            if (items.isEmpty()) {
                _warning.value = "No pending items to release"
                return@launch
            }
            try {
                _isLoading.value = true
                val boxIds = items.mapNotNull { item ->
                    try {
                        item.box.toIntOrNull()
                    } catch (e: Exception) {
                        null
                    }
                }
                if (boxIds.isEmpty()) {
                    _releaseResult.value = ReleaseResult.Error("No valid box IDs to process")
                    return@launch
                }
                val qcUser = UserSession.getUsername()
                val result = service.releaseBoxesBatch(boxIds, qcUser)
                if (result.isSuccess) {
                    val response = result.getOrNull()

                    if (response != null) {
                        if (response.success > 0) {
                            val failedIds = response.failedBoxIds.map { it.toString() }
                            val newPendingList = _allPendingItems.value.filter { item ->
                                failedIds.contains(item.box)
                            }
                            _allPendingItems.value = newPendingList
                            _pendingItems.value = newPendingList

                            // Sincronizar SQLite: eliminar SOLO los items liberados exitosamente.
                            // Los fallidos permanecen en DB — se recuperarán si la app se reinicia.
                            // Ya estamos en viewModelScope.launch, podemos llamar suspend directo.
                            val successfulBoxes = items
                                .filter { !failedIds.contains(it.box) }
                                .map { it.box }
                            if (successfulBoxes.isNotEmpty()) {
                                try {
                                    dao.deleteByBoxes(successfulBoxes)
                                    Log.d(TAG, "SQLite: deleted ${successfulBoxes.size} released items, ${newPendingList.size} remaining")
                                } catch (e: Exception) {
                                    Log.e(TAG, "SQLite ERROR: failed to sync after release: ${e.message}")
                                }
                            }

                            loadReleasedBoxes()
                        }

                        when {
                            response.failed == 0 -> {
                                _releaseResult.value = ReleaseResult.Success(
                                    "Successfully released ${response.success} box(es)"
                                )
                            }
                            response.success == 0 -> {
                                _releaseResult.value = ReleaseResult.Error(
                                    "Failed to release ${response.failed} box(es)\nFailed IDs: ${response.failedBoxIds.joinToString(", ")}"
                                )
                            }
                            else -> {
                                _releaseResult.value = ReleaseResult.PartialSuccess(
                                    successCount = response.success,
                                    failedCount = response.failed,
                                    failedIds = response.failedBoxIds
                                )
                            }
                        }
                    } else {
                        _releaseResult.value = ReleaseResult.Error("Empty response from server")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    _releaseResult.value = ReleaseResult.Error(
                        "Error processing items: ${error?.message}"
                    )
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _releaseResult.value = ReleaseResult.Error("Error processing items: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchBoxes(query: String) {
        currentSearchQuery = query
        applyFiltersAndSearch()
        applyPendingFiltersInternal(currentPendingFilters)
    }

    fun applyHistoryFilters(filters: ReleaseFilterDialog.FilterOptions) {
        currentFilters = filters
        applyFiltersAndSearch()
    }

    fun applyPendingFilters(filters: ReleaseFilterDialog.FilterOptions) {
        currentPendingFilters = filters
        applyPendingFiltersInternal(filters)
    }

    private fun applyPendingFiltersInternal(filters: ReleaseFilterDialog.FilterOptions) {
        try {
            var filtered = _allPendingItems.value

            if (filters.scannedBoxes.isNotEmpty()) {
                filtered = filtered.filter { item ->
                    filters.scannedBoxes.any { filterBox ->
                        item.box.equals(filterBox, ignoreCase = true)
                    }
                }
            }
            if (currentSearchQuery.isNotBlank()) {
                filtered = filtered.filter { item ->
                    item.box.contains(currentSearchQuery, ignoreCase = true)
                }
            }

            _pendingItems.value = filtered

        } catch (e: Exception) {
            _error.value = "Error applying pending filters: ${e.message}"
        }
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

            if (currentFilters.selectedUsers.isNotEmpty()) {
                filtered = filtered.filter { box ->
                    currentFilters.selectedUsers.contains(box.user)
                }
            }

            if (currentFilters.startDate != null || currentFilters.endDate != null) {
                filtered = filtered.filter { box ->
                    val boxDate = parseDate(box.dtModify)
                    boxDate != null && isDateInRange(boxDate, currentFilters.startDate, currentFilters.endDate)
                }
            }

            if (currentFilters.scannedBoxes.isNotEmpty()) {
                filtered = filtered.filter { box ->
                    currentFilters.scannedBoxes.any { filterBox ->
                        box.box.toString().equals(filterBox, ignoreCase = true)
                    }
                }
            }

            if (currentSearchQuery.isNotBlank()) {
                filtered = filtered.filter { box ->
                    box.box.toString().contains(currentSearchQuery, ignoreCase = true) ||
                            box.numOrder.contains(currentSearchQuery, ignoreCase = true) ||
                            box.user.contains(currentSearchQuery, ignoreCase = true)
                }
            }

            _filteredBoxes.value = filtered

        } catch (e: Exception) {
            _error.value = "Error applying filters: ${e.message}"
        }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            format.parse(dateString)
        } catch (e: Exception) {
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

    fun clearWarning() {
        _warning.value = null
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
        data class PartialSuccess(
            val successCount: Int,
            val failedCount: Int,
            val failedIds: List<Int>
        ) : ReleaseResult()
    }
}

data class PendingReleaseItem(
    val box: String,
    val scannedAt: Long = System.currentTimeMillis()
)