package com.example.qceqapp.data.network

import com.example.qceqapp.data.model.Entities

class Service {

    private val restClient = RestClient()

    suspend fun healthCheck(): Result<String> {
        return try {
            restClient.healthCheck()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getToken(): Result<String> {
        return try {
            restClient.getToken()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun userLogin(username: String, password: String): Result<Entities.LoginResponse> {
        return try {
            restClient.userLogin(username, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getCustomers(type: Int): Result<List<Entities.QCCustomerResponse>> {
        return try {
            restClient.getQCCustomers(type)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGrowers(type: Int): Result<List<Entities.QCGrowerResponse>> {
        return try {
            restClient.getQCGrowers(type)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getQCOrders(): Result<List<Entities.QCOrderResponse>> {
        return try {
            restClient.getQCOrders()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun rejectBox(
        idBox: String,
        orderNum: String,
        rowNum: String,
        qaInspector: String,
        qaReason: String
    ): Result<Unit> {
        return try {
            restClient.rejectBox(idBox, orderNum, rowNum, qaInspector, qaReason)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanToInspect(boxId: String): Result<Entities.ScanToInspectResponse> {
        return try {
            restClient.scanToInspect(boxId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderByBox(
        boxId: String
    ): Result<Entities.OrderByBox> {
        return try {
            restClient.getOrderByBox(boxId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getQCHistory(): Result<List<Entities.QCHistoryResponse>> {
        return try {
            restClient.getQCHistory()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun setBoxScan(idBox: String): Result<Boolean> {
        return try {
            restClient.setBoxScan(idBox)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkOrderByBox(idBox: String): Result<String> {
        return try {
            restClient.checkOrderByBox(idBox)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getQCOrderByBox(idBox: String): Result<Entities.QCOrderResponse> {
        return try {
            restClient.getQCOrderByBox(idBox)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInspectIdByBox(idBox: String): Result<String> {
        return try {
            restClient.getInspectIdByBox(idBox)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun rejectScannedBox(
        idBox: String,
        qaInspector: String,
        qaReason: String
    ): Result<Unit> {
        return try {
            restClient.rejectScannedBox(idBox, qaInspector, qaReason)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBoxComposition(idBox: String): Result<List<Entities.BoxCompositionResponse>> {
        return try {
            restClient.getBoxComposition(idBox)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBoxInfo(idBox: String): Result<Entities.BoxInfoResponse> {
        return try {
            restClient.getBoxInfo(idBox)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQCIssues(): Result<List<Entities.QCIssueResponse>> {
        return try {
            restClient.getQCIssues()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

        suspend fun getQCActions(): Result<List<Entities.QCActionResponse>> {
            return try {
                restClient.getQCActions()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSavedBox(idBox: String): Result<Entities.SavedBoxResponse> {
        return try {
            restClient.getSavedBox(idBox)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun saveQCBox(
        idBox: String,
        ordNum: Int,
//        awbNum: Int,
//        telexNum: Int,
        awbNum: String,
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
            restClient.saveQCBox(
                idBox, ordNum, awbNum, telexNum, num,
                issueC, actionC, issueDes, qaInsp,
                listImages, listVideos, inspectStatus, barcodesToI
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun saveQCHistorySent(
        idBox: String,
        ordNum: Int,
        awbNum: String,
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
    ): Result<Unit> {
        return try {
            restClient.saveQCHistorySent(
                idBox, ordNum, awbNum, telexNum, num,
                issueC, actionC, issueDes, qaInsp,
                listImages, listVideos, inspectStatus, barcodesToI
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun sendQCHistorySent(
        idBox: String,
        ordNum: Int,
        awbNum: String,
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
    ): Result<Unit> {
        return try {
            restClient.sendQCHistorySent(
                idBox, ordNum, awbNum, telexNum, num,
                issueC, actionC, issueDes, qaInsp,
                listImages, listVideos, inspectStatus, barcodesToI
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun updateCompositionsIssues(
        idBox: Int,
        floGrade: String,
        floCod: String,
        issues: String
    ): Result<Unit> {
        return try {
            restClient.updateCompositionsIssues(idBox, floGrade, floCod, issues)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun sendBox(
        idBox: String,
        ordNum: Int,
        awbNum: String,
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
            restClient.sendBox(
                idBox, ordNum, awbNum, telexNum, num,
                issueC, actionC, issueDes, qaInsp,
                listImages, listVideos, inspectStatus, barcodesToI
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun sendGroupInspection(
        inspections: String
    ): Result<String> {
        return try {
            restClient.sendGroupInspection(inspections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getFilesByBoxAndOrder(
        guid: String,
        orderNum: String,
        boxIdToInspect: String
    ): Result<ByteArray> {
        return try {
            restClient.getFilesByBoxAndOrder(guid, orderNum, boxIdToInspect)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun uploadPhoto(orderNum: String, boxIdToInspect: String, imageBytes: ByteArray): Result<String> {
        return try {
            restClient.uploadPhoto(orderNum, boxIdToInspect, imageBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun uploadVideo(orderNum: String, boxIdToInspect: String, videoBytes: ByteArray): Result<String> {
        return try {
            restClient.uploadVideo(orderNum, boxIdToInspect, videoBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun updateCompositionIssues(
        idBox: String,
        floGrade: String,
        floCod: String,
        issues: List<String>
    ): Result<Entities.UpdateCompositionIssuesResponse> {
        return try {
            restClient.updateCompositionIssues(idBox, floGrade, floCod, issues)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun releaseBox(idBox: String, qcUser: String): Result<Entities.ReleaseBoxResponse> {
        return try {
            restClient.releaseBox(idBox, qcUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getReleasedBoxes(): Result<List<Entities.ReleaseBoxHistoryResponse>> {
        return try {
            restClient.getReleasedBoxes()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}