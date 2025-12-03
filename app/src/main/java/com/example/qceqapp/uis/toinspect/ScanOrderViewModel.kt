package com.example.qceqapp.uis.toinspect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.qceqapp.data.network.Service

class ScanOrderViewModel : ViewModel() {

    private val service = Service()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success

    fun processScan(idBox: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _success.value = null

            try {
                val scanResult = service.setBoxScan(idBox)
           if (scanResult.isSuccess && scanResult.getOrNull() == true)

                {

                    val existenceResult = service.checkOrderByBox(idBox)

                    if (existenceResult.isSuccess) {
                        val body = existenceResult.getOrNull()?.trim()

                        if (!body.isNullOrEmpty() && !body.contains("Error", ignoreCase = true)) {
                           // _success.value = body
                            _success.value =idBox
                        } else {
                            _error.value = "Box not registered."
                        }
                    }
                    else
                    {
                        _error.value = "Box not registered."
                    }

                }
                else
                {
                    _error.value = "Scan failed. Try again."
                }

            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage}"
            }
            finally
            {
                _loading.value = false
            }
        }
    }

}
