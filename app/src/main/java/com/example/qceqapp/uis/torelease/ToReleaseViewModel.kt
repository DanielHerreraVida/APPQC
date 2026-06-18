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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ToReleaseViewModel : ViewModel() {

    companion object {
        private const val TAG = "ToReleaseViewModel"
    }

    private val service = Service()


    private val dao = QCEQDatabase.getInstance().pendingReleaseDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dbWriteScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    @Volatile
    private var memoryMutated = false

    // Guard contra doble disparo del Process (doble click): evita mandar el mismo batch 2 veces.
    @Volatile
    private var isProcessing = false

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

    private fun loadPendingItemsFromDb() {
        viewModelScope.launch {
            try {
                val entities = dao.getAll()

                if (memoryMutated) {

                    return@launch
                }

                if (entities.isNotEmpty()) {
                    val items = entities.map { PendingReleaseItem(box = it.box, scannedAt = it.scannedAt) }
                    _allPendingItems.value = items
                    applyPendingFiltersInternal(currentPendingFilters)
                } else {
                    Log.i(TAG, "SQLITE_DEBUG: no pending items in DB")
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
        memoryMutated = true
        _allPendingItems.value = allItems

        applyPendingFiltersInternal(currentPendingFilters)

        dbWriteScope.launch {
            try {
                val rowId = dao.insert(PendingReleaseEntity(box = newItem.box, scannedAt = newItem.scannedAt))
                Log.i(TAG, "SQLITE_DEBUG: insert box=${newItem.box} rowId=$rowId" +
                        if (rowId == -1L) " (IGNORED — ya existía en DB)" else "")
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
        memoryMutated = true
        val before = _allPendingItems.value
        val allItems = before.toMutableList()
        val removedFromMemory = allItems.removeAll { it.box == item.box }
        _allPendingItems.value = allItems

        applyPendingFiltersInternal(currentPendingFilters)

        dbWriteScope.launch {
            try {
                val rows = dao.deleteByBox(item.box)
                if (rows == 0) {
                    Log.e(TAG, "SQLITE_DEBUG: deleteByBox affected 0 ROWS for box=${item.box} " +
                            "— la fila no existía en DB al momento del delete")
                }
            } catch (e: Exception) {
                Log.e(TAG, "SQLite ERROR: delete failed for box=${item.box}: ${e.message}")
            }
        }
    }

    fun clearPendingItems() {
        memoryMutated = true
        _allPendingItems.value = emptyList()
        _pendingItems.value = emptyList()

        dbWriteScope.launch {
            try {
                val rows = dao.deleteAll()
                Log.i(TAG, "SQLITE_DEBUG: deleteAll rowsAffected=$rows")
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
        // Re-entrada: si ya hay un Process en curso, ignorar clicks adicionales.
        if (isProcessing) {
            Log.i(TAG, "PROCESS ignored: already processing")
            return
        }
        isProcessing = true
        viewModelScope.launch {
            val items = _allPendingItems.value

            if (items.isEmpty()) {
                _warning.value = "No pending items to release"
                isProcessing = false
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
                        // status: 1 = liberada, -1 = ya liberada, 0 = ignorada (ya tiene acción QC), otro = error
                        val releasedGroup = response.groups.firstOrNull { it.status == 1 }
                        val alreadyGroup = response.groups.firstOrNull { it.status == -1 }
                        val ignoredGroup = response.groups.firstOrNull { it.status == 0 }
                        val errorGroups = response.groups.filter { it.status != 1 && it.status != -1 && it.status != 0 }
                        val releasedIds = (releasedGroup?.idBoxes ?: emptyList()).map { it.toString() }
                        val alreadyIds = (alreadyGroup?.idBoxes ?: emptyList()).map { it.toString() }
                        val ignoredIds = (ignoredGroup?.idBoxes ?: emptyList()).map { it.toString() }
                        val errorIds = errorGroups.flatMap { it.idBoxes }

                        val releasedCount = releasedGroup?.count ?: releasedIds.size
                        val alreadyCount = alreadyGroup?.count ?: alreadyIds.size
                        val ignoredCount = ignoredGroup?.count ?: ignoredIds.size
                        val errorCount = errorGroups.sumOf { if (it.count > 0) it.count else it.idBoxes.size }
                        val releasedTotal = releasedCount + alreadyCount

                        // No procesadas (ya tienen acción QC -> status 0, o error) PERMANECEN en Pending
                        // para que el usuario las revise. Solo se quitan las liberadas (1) y ya liberadas (-1).
                        val notProcessedIds = (ignoredGroup?.idBoxes ?: emptyList()) + errorIds
                        val notProcessedCount = ignoredCount + errorCount

                        val resolvedIds = (releasedIds + alreadyIds).toSet()
                        if (resolvedIds.isNotEmpty()) {
                            val newPendingList = _allPendingItems.value.filter { item ->
                                !resolvedIds.contains(item.box)
                            }
                            _allPendingItems.value = newPendingList
                            applyPendingFiltersInternal(currentPendingFilters)
                            val resolvedBoxes = items.map { it.box }.filter { resolvedIds.contains(it) }
                            if (resolvedBoxes.isNotEmpty()) {
                                try {
                                    withContext(NonCancellable) {
                                        dao.deleteByBoxes(resolvedBoxes)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "SQLite ERROR: failed to sync after release: ${e.message}")
                                }
                            }
                            loadReleasedBoxes()
                        }

                        if (notProcessedCount == 0) {
                            _releaseResult.value = ReleaseResult.Success(
                                "Released $releasedTotal box(es)"
                            )
                        } else {
                            _releaseResult.value = ReleaseResult.PartialSuccess(
                                successCount = releasedTotal,
                                failedCount = notProcessedCount,
                                failedIds = notProcessedIds
                            )
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
                isProcessing = false
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

    fun totalPendingCount(): Int = _allPendingItems.value.size

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