package com.example.qceqapp.data.network

import com.example.qceqapp.data.model.Entities
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Path
import retrofit2.http.Part
import okhttp3.MultipartBody

interface ApiService {
    @GET("api/health/check")
    suspend fun healthCheck(): Response<ResponseBody>

    @POST("api/auth/login")
    suspend fun getToken(
        @Body request: Entities.TokenRequest
    ): Response<Entities.TokenResponse>
    @POST("api/user/login")
    suspend fun userLogin(
        @Header("Authorization") authorization: String,
        @Body request: Entities.LoginRequest
    ): Response<Entities.LoginResponse>

    @GET("api/helper/qccustomer/{type}")
    suspend fun getQCCustomers(
        @Header("Authorization") authorization: String,
        @Path("type") type: Int
    ): Response<List<Entities.QCCustomerResponse>>

    @GET("api/helper/qcgrower/{type}")
    suspend fun getQCGrowers(
        @Header("Authorization") authorization: String,
        @Path("type") type: Int
    ): Response<List<Entities.QCGrowerResponse>>
    @GET("api/qcorder/get")
    suspend fun getQCOrders(
        @Header("Authorization") authorization: String
    ): Response<List<Entities.QCOrderResponse>>

    @POST("api/box/save/qchistory/sent")
    suspend fun saveQCHistorySent(
        @Header("Authorization") authorization: String,
        @Body request: Entities.SaveQCHistorySentRequest
    ): Response<Unit>
    @POST("api/box/sendToQCHistory")
    suspend fun sendQCHistorySent(
        @Header("Authorization") authorization: String,
        @Body request: Entities.SendQCHistorySentRequest
    ): Response<Unit>
    @POST("api/box/rejbox")
    suspend fun rejectBox(
        @Header("Authorization") authorization: String,
        @Body request: Entities.RejectBoxRequest
    ): Response<Unit>

    @GET("api/box/scantoinspect/{boxId}")
    suspend fun scanToInspect(
        @Header("Authorization") authorization: String,
        @Path("boxId") boxId: String
    ): Response<Entities.ScanToInspectResponse>

    @POST("api/qcorder/get/bybox/{boxid}")
    suspend fun getOrderByBox(
        @Header("Authorization") authorization: String,
        @Path("boxid") boxId: String,
    ): Response<Entities.OrderByBox>
    @GET("api/qcorder/get/history")
    suspend fun getQCHistory(
        @Header("Authorization") authorization: String
    ): Response<List<Entities.QCHistoryResponse>>
    @POST("api/box/setboxscan/{idbox}")
    suspend fun setBoxScan(
        @Header("Authorization") authorization: String,
        @Path("idbox") idBox: String
    ): Response<Unit>

    @GET("api/qcorder/check/bybox/{idbox}")
    suspend fun checkOrderByBox(
        @Header("Authorization") authorization: String,
        @Path("idbox") idBox: String
    ): Response<ResponseBody>

    @GET("api/qcorder/get/bybox/{idbox}")
    suspend fun getQCOrderByBox(
        @Header("Authorization") authorization: String,
        @Path("idbox") idBox: String
    ): Response<Entities.QCOrderResponse>

    @GET("api/box/inspectid/{idbox}")
    suspend fun getInspectIdByBox(
        @Header("Authorization") authorization: String,
        @Path("idbox") idBox: String
    ): Response<ResponseBody>

    @POST("api/box/rejscannedbox")
    suspend fun rejectScannedBox(
        @Header("Authorization") authorization: String,
        @Body request: Entities.RejectScannedBoxRequest
    ): Response<Unit>

    @GET("api/box/composition/{idbox}")
    suspend fun getBoxComposition(
        @Header("Authorization") authorization: String,
        @Path("idbox") idBox: String
    ): Response<List<Entities.BoxCompositionResponse>>

    @GET("api/box/get/{idbox}")
    suspend fun getBoxInfo(
        @Header("Authorization") authorization: String,
        @Path("idbox") idBox: String
    ): Response<Entities.BoxInfoResponse>

    @GET("api/helper/qcissues")
    suspend fun getQCIssues(
        @Header("Authorization") authorization: String
    ): Response<List<Entities.QCIssueResponse>>

    @GET("api/helper/qcactions")
    suspend fun getQCActions(
        @Header("Authorization") authorization: String
    ): Response<List<Entities.QCActionResponse>>

    @GET("api/box/get/saved/{idbox}")
    suspend fun getSavedBox(
        @Header("Authorization") authorization: String,
        @Path("idbox") idBox: String
    ): Response<Entities.SavedBoxResponse>

    @POST("api/box/saveqcBox")
    suspend fun saveQCBox(
        @Header("Authorization") authorization: String,
        @Body request: Entities.SaveQCBoxRequest
    ): Response<Unit>

    @POST("api/box/updateCompositionsIssuesBx")
    suspend fun updateCompositionsIssues(
        @Header("Authorization") authorization: String,
        @Body request: Entities.UpdateCompositionsRequest
    ): Response<Unit>

    @POST("api/qcorder/sendbox")
    suspend fun sendBox(
        @Header("Authorization") authorization: String,
        @Body request: Entities.SendBoxRequest
    ): Response<Unit>
    @POST("api/box/send/group")
    suspend fun sendGroupInspection(
        @Header("Authorization") authorization: String,
        @Body request: Entities.SendGroupInspectionRequest
    ): Response<ResponseBody>
    @GET("api/file/get/{guid}/{orderNum}/{boxIdToInspect}")
    suspend fun getFilesByBoxAndOrder(
        @Header("Authorization") authorization: String,
        @Path("guid") guid: String,
        @Path("orderNum") orderNum: String,
        @Path("boxIdToInspect") boxIdToInspect: String,
    ): Response<ResponseBody>
    @Multipart
    @POST("api/file/upload/{orderNum}/{boxIdToInspect}")
    suspend fun uploadFile(
        @Header("Authorization") authorization: String,
        @Path("orderNum") orderNum: String,
        @Path("boxIdToInspect") boxIdToInspect: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>
    @POST("api/box/updateCompositionsIssuesBx")
    suspend fun updateCompositionIssues(
        @Header("Authorization") authorization: String,
        @Body request: Entities.UpdateCompositionIssuesRequest
    ): Response<Entities.UpdateCompositionIssuesResponse>

    @POST("api/box/release/{idbox}/{QCUsername}")
    suspend fun releaseBox(
        @Header("Authorization") authorization: String,
        @Path("idbox") idBox: String,
        @Path("QCUsername") QCUsername: String
    ): Response<Entities.ReleaseBoxResponse>

    @GET("api/box/release")
    suspend fun getReleasedBoxes(
        @Header("Authorization") authorization: String
    ): Response<List<Entities.ReleaseBoxHistoryResponse>>

}
