package com.example.qceqapp.uis.QCInspection

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class QCMediaViewModel : ViewModel() {
    val photoPaths = MutableLiveData<MutableList<String>>(mutableListOf())
    val videoPaths = MutableLiveData<MutableList<String>>(mutableListOf())
    var boxIdToInspect: String = ""
    var orderNum: String = ""
}

