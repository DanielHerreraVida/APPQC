package com.example.qceqapp.uis.QCInspection

import androidx.lifecycle.*
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.data.network.Service
import com.google.gson.Gson
import kotlinx.coroutines.launch

class QCInspectionViewModel : ViewModel() {

    private val service = Service()
    private val _boxInfo = MutableLiveData<Entities.BoxInfoResponse>()
    val boxInfo: LiveData<Entities.BoxInfoResponse> get() = _boxInfo
    private val _qcIssues = MutableLiveData<List<Entities.QCIssueResponse>>()
    val qcIssues: LiveData<List<Entities.QCIssueResponse>> get() = _qcIssues
    private val _qcActions = MutableLiveData<List<Entities.QCActionResponse>>()
    val qcActions: LiveData<List<Entities.QCActionResponse>> get() = _qcActions
    private val _savedBox = MutableLiveData<Entities.SavedBoxResponse?>()
    val savedBox: LiveData<Entities.SavedBoxResponse?> get() = _savedBox
    private val _openCameraModule = MutableLiveData<Boolean>()
    val openCameraModule: LiveData<Boolean> get() = _openCameraModule
    private val _boxComposition = MutableLiveData<List<Entities.BoxCompositionResponse>>()
    val boxComposition: LiveData<List<Entities.BoxCompositionResponse>> = _boxComposition
    private val _compositionLoading = MutableLiveData<Boolean>()
    val compositionLoading: LiveData<Boolean> get() = _compositionLoading
    var lastScannedCode: String? = null
    var selectedBoxes: List<Entities.BoxToInspect> = emptyList()

    var photoPaths: MutableList<String> = mutableListOf()
    var videoPaths: MutableList<String> = mutableListOf()

    var selectedIssues: List<Entities.QCIssueResponse> = emptyList()
    var selectedActions: List<Entities.QCActionResponse> = emptyList()
    var boxIdToInspect: String = ""
    var orderNum: String = ""

    fun serializeSelectedBoxes(): String {
        return Gson().toJson(selectedBoxes)
    }

    private val _inspectId = MutableLiveData<String>()
    val inspectId: LiveData<String> get() = _inspectId
    fun deserializeBoxes(json: String): List<Entities.BoxToInspect> {
        val type = object : com.google.gson.reflect.TypeToken<List<Entities.BoxToInspect>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun loadInspectIdByBox(idBox: String) {
        viewModelScope.launch {
            try {
                val result = service.getInspectIdByBox(idBox)
                result.getOrNull()?.let {
                    _inspectId.postValue(it)
                } ?: run {
                    _inspectId.postValue("")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _inspectId.postValue("")
            }
        }
    }
    fun loadInspectionData(idBox: String) {
        viewModelScope.launch {
            try {
                val boxInfoResult = service.getBoxInfo(idBox)
                val issuesResult = service.getQCIssues()
                val actionsResult = service.getQCActions()
                val savedBoxResult = service.getSavedBox(idBox)

                boxInfoResult.getOrNull()?.let { boxInfo ->
                    _boxInfo.postValue(boxInfo)
                    orderNum = boxInfo.numOrd
                }
                issuesResult.getOrNull()?.let { _qcIssues.postValue(it) }
                actionsResult.getOrNull()?.let { _qcActions.postValue(it) }
               // savedBoxResult.getOrNull()?.let { _savedBox.postValue(it) }
                savedBoxResult.getOrNull()?.let { savedBox ->
                    _savedBox.postValue(savedBox)
                    boxIdToInspect = savedBox.boxIdToInspect
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun sendBoxToQCHistory(
        idBox: String,
        ordNum: Int,
//        awbNum: Int,
        awbNum: String,
//        telexNum: Int,
        telexNum: String,
        num: Int,
        issueC: String,
        actionC: String,
        issueDes: String,
        qaInsp: String,
        listImages: List<String>,
        listVideos: List<String>,
        inspectStatus: Int,
        barcodesToI: String
    ): Result<String> {
        return try {
//            val result = service.sendBox(
                val result = service.sendQCHistorySent(

                    idBox = idBox,
                ordNum = ordNum,
                awbNum = awbNum,
                telexNum = telexNum,
                num = num,
                issueC = issueC,
                actionC = actionC,
                issueDes = issueDes,
                qaInsp = qaInsp,
                listImages = listImages,
                listVideos = listVideos,
                inspectStatus = inspectStatus,
                barcodesToI = barcodesToI
            )

            if (result.isSuccess) {
                photoPaths.clear()
                videoPaths.clear()
                selectedIssues = emptyList()
                selectedActions = emptyList()
                Result.success("Box sent successfully")
            } else {
                Result.failure(Exception("Server returned failure"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    fun updateBoxComposition(compositions: List<Entities.BoxCompositionResponse>) {
        _boxComposition.value = compositions
        compositions.forEach { comp ->
        }
    }
    fun loadBoxComposition(idBox: String) {
        viewModelScope.launch {
            _compositionLoading.postValue(true)
            try {
                val result = service.getBoxComposition(idBox)
                result.getOrNull()?.let {
                    _boxComposition.postValue(it)
                } ?: run {
                    _boxComposition.postValue(emptyList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _boxComposition.postValue(emptyList())
            } finally {
                _compositionLoading.postValue(false)
            }
        }
    }
    suspend fun saveQCBox(
        idBox: String,
        ordNum: Int,
//        awbNum: Int,
        awbNum: String,
//        telexNum: Int,
        telexNum: String,
        num: Int,
        issueC: String,
        actionC: String,
        issueDes: String,
        qaInsp: String,
        listImages: List<String>,
        listVideos: List<String>,
        inspectStatus: String,
        barcodesToI: String
    ): Result<Unit> {
        return try {
            val result = service.saveQCBox(
                idBox = idBox,
                ordNum = ordNum,
                awbNum = awbNum,
                telexNum = telexNum,
                num = num,
                issueC = issueC,
                actionC = actionC,
                issueDes = issueDes,
                qaInsp = qaInsp,
                listImages = listImages,
                listVideos = listVideos,
                inspectStatus = inspectStatus,
                barcodesToI = barcodesToI
            )

            if (result.isSuccess) {
                photoPaths.clear()
                videoPaths.clear()
                selectedIssues = emptyList()
                selectedActions = emptyList()

                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned failure"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    fun onCameraButtonClicked() {
        _openCameraModule.value = true
    }
    fun resetNavigationFlag() {
        _openCameraModule.value = false
    }
}