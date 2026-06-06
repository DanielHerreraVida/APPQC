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
//                    Log.i(TAG, "SQLITE_DEBUG: initial load DISCARDED — user mutated state " +
//                            "while getAll() was in flight. staleDb=${entities.map { it.box }} " +
//                            "memory=${_allPendingItems.value.map { it.box }}")
                    return@launch
                }

                if (entities.isNotEmpty()) {
                    val items = entities.map { PendingReleaseItem(box = it.box, scannedAt = it.scannedAt) }
                    _allPendingItems.value = items
                    applyPendingFiltersInternal(currentPendingFilters)
//                    Log.i(TAG, "SQLITE_DEBUG: loaded ${items.size} pending items from DB: ${items.map { it.box }}")
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

        // Persistir en SQLite — dbWriteScope: sobrevive a onCleared() y preserva orden FIFO
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
        val removedFromMemory = allItems.remove(item)
        _allPendingItems.value = allItems

        Log.i(TAG, "SQLITE_DEBUG: removePendingItem box=${item.box} " +
                "removedFromMemory=$removedFromMemory " +
                "memoryBefore=${before.map { it.box }} memoryAfter=${allItems.map { it.box }}")

        applyPendingFiltersInternal(currentPendingFilters)

        dbWriteScope.launch {
            try {
                val rows = dao.deleteByBox(item.box)
                Log.i(TAG, "SQLITE_DEBUG: deleteByBox box=${item.box} rowsAffected=$rows")
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
        viewModelScope.launch {
            val items = _allPendingItems.value

            // INSTRUMENTACIÓN: estado completo inmediatamente antes del Process.
            // Si memory y sqlite difieren aquí, hay una escritura perdida o en vuelo.
            val dbSnapshot = try {
                dao.getAll().map { it.box }
            } catch (e: Exception) {
                listOf("<error: ${e.message}>")
            }
            Log.i(TAG, "SQLITE_DEBUG: PROCESS START " +
                    "memory=${items.map { it.box }} " +
                    "sqlite=$dbSnapshot " +
                    "uiFiltered=${_pendingItems.value.map { it.box }}")

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
                Log.i(TAG, "SQLITE_DEBUG: PROCESS sending boxIds=$boxIds to releaseBoxesBatch")
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
                            // NonCancellable: si el usuario navega fuera mientras esto corre,
                            // la limpieza DEBE completarse — si no, las cajas ya liberadas en el
                            // servidor quedarían en DB local y se reenviarían como duplicados.
                            val successfulBoxes = items
                                .filter { !failedIds.contains(it.box) }
                                .map { it.box }
                            if (successfulBoxes.isNotEmpty()) {
                                try {
                                    withContext(NonCancellable) {
                                        val rows = dao.deleteByBoxes(successfulBoxes)
                                        Log.i(TAG, "SQLITE_DEBUG: post-release sync " +
                                                "deleted=$successfulBoxes rowsAffected=$rows " +
                                                "remaining=${newPendingList.map { it.box }}")
                                    }
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

    /**
     * Total REAL de items que [releaseAllPending] enviará al API.
     * El Fragment debe usar esto en el diálogo de confirmación — NO pendingItems.value.size,
     * que es la lista filtrada por búsqueda/filtros y puede mostrar menos de lo que se envía.
     */
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