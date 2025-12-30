package com.example.qceqapp.uis.QCBoxes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.data.network.Service
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QCBoxesViewModel(private val service: Service = Service()) : ViewModel() {

    private val TAG = "QCBoxesViewModel"
    private val MAX_RETRIES = 2
    private val RETRY_DELAY_MS = 1000L
    var idToInspect: String? = null


    var selectedBoxes: List<Entities.BoxToInspect> = emptyList()
    var relatedBoxes: List<Entities.BoxToInspect> = emptyList()
    var allBoxes: List<Entities.BoxToInspect> = emptyList()

    private var _orderHeader: Entities.OrderByBox? = null
    val orderHeader: Entities.OrderByBox?
        get() = _orderHeader

    var onDataLoaded: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onOrderLoaded: ((Entities.OrderByBox) -> Unit)? = null

    fun loadBoxData(boxId: String, previouslySelected: List<Entities.BoxToInspect>? = null) {

        viewModelScope.launch {
            try {
                val orderResult = service.getOrderByBox(boxId)

                if (!orderResult.isSuccess) {
                }

                _orderHeader = orderResult.getOrNull()

                if (_orderHeader != null) {
                    onOrderLoaded?.invoke(_orderHeader!!)
                } else {
                    Log.w(TAG, "No header found for boxId: $boxId")
                }

                val boxesResponse = loadBoxesWithRetry(boxId, MAX_RETRIES)

                if (boxesResponse == null) {
                    onError?.invoke("Error getting boxes. Please try again.")
                    return@launch
                }

                selectedBoxes = if (!previouslySelected.isNullOrEmpty()) {
                    previouslySelected.sortedBy { it.barcode ?: "" }
                } else {
                    val selected = boxesResponse.lstSelectedBoxes ?: emptyList()
                    selected.sortedBy { it.barcode ?: "" }
                }

                relatedBoxes = (boxesResponse.lstRelatedBoxes ?: emptyList()).sortedBy { it.barcode ?: "" }
                allBoxes = (boxesResponse.lstAllBoxes ?: emptyList()).sortedBy { it.barcode ?: "" }

                onDataLoaded?.invoke()

            } catch (e: Exception) {
                onError?.invoke("Error loading data: ${e.message}")
            }
        }
    }

    private suspend fun loadBoxesWithRetry(
        boxId: String,
        maxRetries: Int
    ): Entities.ScanToInspectResponse? {
        var attempt = 0
        var lastException: Exception? = null

        while (attempt <= maxRetries) {
            try {
                if (attempt > 0) {
                    delay(RETRY_DELAY_MS * attempt)
                }

                val scanResult = service.scanToInspect(boxId)

                if (scanResult.isSuccess) {
                    val response = scanResult.getOrNull()
                    if (response != null) {
                        idToInspect = response.idToInspect

                        return response
                    } else {
                        Log.w(TAG, "scanToInspect returned null response on attempt ${attempt + 1}")
                    }
                } else {
                    val error = scanResult.exceptionOrNull()
                    lastException = error as? Exception
                }

            } catch (e: Exception) {
                lastException = e
            }

            attempt++
        }

        return null
    }

    fun getInspectIdByBox(boxId: String, onResult: (String?) -> Unit) {

        viewModelScope.launch {
            try {
                val result = service.getInspectIdByBox(boxId)

                if (result.isSuccess) {
                    val id = result.getOrNull()
                    onResult(id)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
}