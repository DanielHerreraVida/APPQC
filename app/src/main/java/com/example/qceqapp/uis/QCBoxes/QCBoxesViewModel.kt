package com.example.qceqapp.uis.QCBoxes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.data.network.Service
import kotlinx.coroutines.launch

class QCBoxesViewModel(private val service: Service = Service()) : ViewModel() {

    private val TAG = "QCBoxesViewModel"

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
                _orderHeader = orderResult.getOrNull()

                if (_orderHeader != null) {
                    onOrderLoaded?.invoke(_orderHeader!!)
                } else {
                    Log.w(TAG, "No header found: $boxId")
                }
                val scanResult = service.scanToInspect(boxId)
                val boxesResponse = scanResult.getOrNull()

                if (boxesResponse == null) {
                    onError?.invoke("Error getting boxes")
                    return@launch
                }
                selectedBoxes = if (!previouslySelected.isNullOrEmpty()) {
                    previouslySelected.sortedBy { it.barcode ?: "" }
                } else {
                    (boxesResponse.lstSelectedBoxes ?: emptyList()).sortedBy { it.barcode ?: "" }
                }

                relatedBoxes = (boxesResponse.lstRelatedBoxes ?: emptyList()).sortedBy { it.barcode ?: "" }
                allBoxes = (boxesResponse.lstAllBoxes ?: emptyList()).sortedBy { it.barcode ?: "" }
                onDataLoaded?.invoke()
            } catch (e: Exception) {
                onError?.invoke("Error loading data: ${e.message}")
            }
        }
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