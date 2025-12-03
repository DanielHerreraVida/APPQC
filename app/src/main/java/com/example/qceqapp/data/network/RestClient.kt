package com.example.qceqapp.data.network

import android.util.Log
import com.example.qceqapp.data.model.Entities
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.example.qceqapp.data.model.session.UserSession
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RestClient {

    private val apiService: ApiService

    init {
        val okHttpClient = createOkHttpClient()
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("API_LOG", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .sslSocketFactory(createSSLSocketFactory(), createTrustManager())
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private suspend fun ensureToken(): Result<Unit> {
        return try {
            if (Constants.token.isEmpty()) {
                val tokenResult = getToken()
                if (tokenResult.isFailure) {
                    return Result.failure(tokenResult.exceptionOrNull()
                        ?: Exception("Failed to obtain authentication token"))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Token validation error: ${e.message}"))
        }
    }

    private fun getAuthorizationHeader(): String = "Bearer ${Constants.token}"

    private fun handleHttpError(code: Int, rawError: String?): Exception {
        val cleanMessage = rawError
            ?.replace(Regex("<[^>]*>"), "") // Remove HTML tags
            ?.replace(Regex("\\s+"), " ")   // Clean extra spaces
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return when (code) {
            400 -> Exception("Bad request. Please verify the data sent.")
            401 -> Exception("Unauthorized. Your session has expired or credentials are incorrect.")
            403 -> Exception("Access denied. You don't have permission to perform this action.")
            404 -> Exception("Resource not found.")
            408 -> Exception("Request timeout. Please try again.")
            500 -> Exception("Internal server error. Please try again later.")
            502 -> Exception("Bad gateway. Server is temporarily unavailable.")
            503 -> Exception("Service unavailable. Please try again later.")
            504 -> Exception("Gateway timeout. Please try again.")
            in 500..599 -> Exception("Server error (${code}). Please try again later.")
            else -> Exception(cleanMessage ?: "Unexpected error (${code}).")
        }
    }

    private fun <T> handleResponse(
        response: Response<T>,
        emptyErrorMessage: String = "Empty response from server"
    ): Result<T> {
        return try {
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(emptyErrorMessage))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error processing response: ${e.message}"))
        }
    }

    private fun <T> handleListResponse(
        response: Response<List<T>>,
        emptyErrorMessage: String = "No records found"
    ): Result<List<T>> {
        return try {
            if (response.isSuccessful) {
                val body = response.body()
                if (!body.isNullOrEmpty()) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(emptyErrorMessage))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error processing list response: ${e.message}"))
        }
    }

    private fun createSSLSocketFactory(): javax.net.ssl.SSLSocketFactory {
        return try {
            val trustManager = createTrustManager()
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), java.security.SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw Exception("Failed to create SSL socket factory: ${e.message}")
        }
    }

    private fun createTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    // ==================== API METHODS ====================

    suspend fun healthCheck(): Result<String> {
        return try {
            val response = apiService.healthCheck()

            if (response.isSuccessful) {
                val raw = response.body()?.string() ?: ""
                Result.success(raw)
            }
            else
            {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Health check error: ${e.message}"))
        }
    }

    suspend fun getToken(): Result<String> {
        return try {
            val request = Entities.TokenRequest(
                user = Constants.API_USER,
                password = Constants.API_PASSWORD
            )

            val response = apiService.getToken(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Constants.token = body.token
                    Result.success(body.token)
                } else {
                    Result.failure(Exception("Empty token response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Token request failed: ${e.message}"))
        }
    }

    suspend fun userLogin(username: String, password: String): Result<Entities.LoginResponse> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val request = Entities.LoginRequest(
                login = username,
                password = password
            )

            val response = apiService.userLogin(getAuthorizationHeader(), request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && !body.qcUsername.isNullOrBlank()) {
                    UserSession.clearSession()
                    UserSession.saveUser(body)
                    Result.success(body)
                } else {
                    Result.failure(Exception("Invalid credentials or empty user data"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }

    suspend fun getQCCustomers(type: Int): Result<List<Entities.QCCustomerResponse>> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getQCCustomers(getAuthorizationHeader(), type)
            handleListResponse(response, "No customers found")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch customers: ${e.message}"))
        }
    }

    suspend fun getQCGrowers(type: Int): Result<List<Entities.QCGrowerResponse>> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getQCGrowers(getAuthorizationHeader(), type)
            handleListResponse(response, "No growers found")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch growers: ${e.message}"))
        }
    }

    suspend fun getQCOrders(): Result<List<Entities.QCOrderResponse>> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getQCOrders(getAuthorizationHeader())
            handleListResponse(response, "No orders found for inspection")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch QC orders: ${e.message}"))
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
            ensureToken().onFailure { return Result.failure(it) }

            val request = Entities.RejectBoxRequest(
                idBox = idBox,
                orderNum = orderNum,
                rowNum = rowNum,
                qaInspector = qaInspector,
                qaReason = qaReason
            )

            val response = apiService.rejectBox(getAuthorizationHeader(), request)
            handleResponse(response, "Failed to reject box")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to reject box: ${e.message}"))
        }
    }

    suspend fun scanToInspect(boxId: String): Result<Entities.ScanToInspectResponse> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.scanToInspect(getAuthorizationHeader(), boxId)
            handleResponse(response, "Box not found or invalid scan")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to scan box: ${e.message}"))
        }
    }

    suspend fun getOrderByBox(boxId: String): Result<Entities.OrderByBox> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getOrderByBox(getAuthorizationHeader(), boxId)
            handleResponse(response, "Order not found for this box")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch order by box: ${e.message}"))
        }
    }

    suspend fun getQCHistory(): Result<List<Entities.QCHistoryResponse>> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getQCHistory(getAuthorizationHeader())
            handleListResponse(response, "No history records found")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch QC history: ${e.message}"))
        }
    }

    suspend fun setBoxScan(idBox: String): Result<Boolean> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val response = apiService.setBoxScan(getAuthorizationHeader(), idBox)

            when {
                response.isSuccessful -> Result.success(true)
                response.code() == 400 -> Result.failure(Exception("Box does not match"))
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(handleHttpError(response.code(), errorBody))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to set box scan: ${e.message}"))
        }
    }

    suspend fun checkOrderByBox(idBox: String): Result<String> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val response = apiService.checkOrderByBox(getAuthorizationHeader(), idBox)

            val code = response.code()
            val rawBody = response.body()?.string()?.trim()
            val errorBody = response.errorBody()?.string()?.trim()

            when {
                response.isSuccessful && !rawBody.isNullOrEmpty() && !rawBody.startsWith("{") -> {
                    Result.success(rawBody)
                }
                response.isSuccessful && rawBody.isNullOrEmpty() -> {
                    Result.failure(Exception("Server returned empty response"))
                }
                !response.isSuccessful && !errorBody.isNullOrEmpty() && errorBody.startsWith("{") -> {
                    Result.failure(handleHttpError(code, errorBody))
                }
                else -> {
                    Result.failure(Exception("Unexpected response from server (${code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to check order by box: ${e.message}"))
        }
    }

    suspend fun getQCOrderByBox(idBox: String): Result<Entities.QCOrderResponse> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getQCOrderByBox(getAuthorizationHeader(), idBox)
            handleResponse(response, "No order found for this box")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch QC order by box: ${e.message}"))
        }
    }

    suspend fun getInspectIdByBox(idBox: String): Result<String> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val response = apiService.getInspectIdByBox(getAuthorizationHeader(), idBox)

            if (response.isSuccessful) {
                val rawBody = response.body()?.string()?.trim()
                if (!rawBody.isNullOrEmpty()) {
                    Result.success(rawBody)
                } else {
                    Result.failure(Exception("Empty inspect ID response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch inspect ID: ${e.message}"))
        }
    }

    suspend fun rejectScannedBox(
        idBox: String,
        qaInspector: String,
        qaReason: String
    ): Result<Unit> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val request = Entities.RejectScannedBoxRequest(
                idBox = idBox,
                qaInspector = qaInspector,
                qaReason = qaReason
            )

            val response = apiService.rejectScannedBox(getAuthorizationHeader(), request)
            handleResponse(response, "Failed to reject scanned box")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to reject scanned box: ${e.message}"))
        }
    }

    suspend fun getBoxComposition(idBox: String): Result<List<Entities.BoxCompositionResponse>> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getBoxComposition(getAuthorizationHeader(), idBox)

            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch box composition: ${e.message}"))
        }
    }

    suspend fun getBoxInfo(idBox: String): Result<Entities.BoxInfoResponse> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getBoxInfo(getAuthorizationHeader(), idBox)
            handleResponse(response, "Box information not found")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch box info: ${e.message}"))
        }
    }

    suspend fun getQCIssues(): Result<List<Entities.QCIssueResponse>> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getQCIssues(getAuthorizationHeader())

            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch QC issues: ${e.message}"))
        }
    }

    suspend fun getQCActions(): Result<List<Entities.QCActionResponse>> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getQCActions(getAuthorizationHeader())

            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch QC actions: ${e.message}"))
        }
    }

    suspend fun getSavedBox(idBox: String): Result<Entities.SavedBoxResponse> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }
            val response = apiService.getSavedBox(getAuthorizationHeader(), idBox)
            handleResponse(response, "Saved box not found")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch saved box: ${e.message}"))
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
            ensureToken().onFailure { return Result.failure(it) }

            val request = Entities.SaveQCBoxRequest(
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

            val response = apiService.saveQCBox(getAuthorizationHeader(), request)
            handleResponse(response, "Failed to save QC box")
        }
        catch (e: Exception) {
            Result.failure(Exception("Failed to save QC box: ${e.message}"))
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
            ensureToken().onFailure { return Result.failure(it) }

            val request = Entities.SaveQCHistorySentRequest(
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

            val response = apiService.saveQCHistorySent(getAuthorizationHeader(), request)
            handleResponse(response, "Failed to save history")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save QC history: ${e.message}"))
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
            ensureToken().onFailure { return Result.failure(it) }

            val request = Entities.SendQCHistorySentRequest(
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

            val response = apiService.sendQCHistorySent(getAuthorizationHeader(), request)
            handleResponse(response, "Failed to send history")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to send QC history: ${e.message}"))
        }
    }

    suspend fun updateCompositionsIssues(
        idBox: Int,
        floGrade: String,
        floCod: String,
        issues: String
    ): Result<Unit> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val request = Entities.UpdateCompositionsRequest(
                idBox = idBox,
                floGrade = floGrade,
                floCod = floCod,
                issues = issues
            )

            val response = apiService.updateCompositionsIssues(getAuthorizationHeader(), request)
            handleResponse(response, "Failed to update compositions")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update compositions: ${e.message}"))
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
            ensureToken().onFailure { return Result.failure(it) }

            val request = Entities.SendBoxRequest(
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

            val response = apiService.sendBox(getAuthorizationHeader(), request)
            handleResponse(response, "Failed to send box")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to send box: ${e.message}"))
        }
    }

    suspend fun sendGroupInspection(inspections: String): Result<String> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val request = Entities.SendGroupInspectionRequest(inspections)
            val response = apiService.sendGroupInspection(getAuthorizationHeader(), request)

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                if (responseBody != null) {
                    Result.success(responseBody)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(handleHttpError(response.code(), errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to send group inspection: ${e.message}"))
        }
    }
    suspend fun getFilesByBoxAndOrder(
        guid: String,
        orderNum: String,
        boxIdToInspect: String
    ): Result<ByteArray> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val response = apiService.getFilesByBoxAndOrder(
                getAuthorizationHeader(),
                guid,
                orderNum,
                boxIdToInspect
            )

            if (response.isSuccessful && response.body() != null) {
                val bytes = response.body()!!.bytes()
                Result.success(bytes)
            } else {
                Result.failure(Exception("Image not found"))
            }

        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch image: ${e.message}"))
        }
    }
    suspend fun uploadPhoto(orderNum: String, boxIdToInspect: String, imageBytes: ByteArray): Result<String> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val requestBody = MultipartBody.Part.createFormData(
                "file",
                "photo_${System.currentTimeMillis()}.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )

            val response = apiService.uploadFile(
                getAuthorizationHeader(),
                orderNum,
                boxIdToInspect,
                requestBody
            )

            if (response.isSuccessful && response.body() != null) {
                val json = response.body()?.string()
                val jsonObject = JSONObject(json ?: "")
                val guidFile = jsonObject.getString("guidFile")
                Result.success(guidFile)
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error uploading photo: ${e.message}"))
        }
    }
    suspend fun uploadVideo(orderNum: String, boxIdToInspect: String, videoBytes: ByteArray): Result<String> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val requestBody = MultipartBody.Part.createFormData(
                "file",
                "video_${System.currentTimeMillis()}.mp4",
                videoBytes.toRequestBody("video/mp4".toMediaTypeOrNull())
            )

            val response = apiService.uploadFile(
                getAuthorizationHeader(),
                orderNum,
                boxIdToInspect,
                requestBody
            )

            if (response.isSuccessful && response.body() != null) {
                val json = response.body()?.string()
                val jsonObject = JSONObject(json ?: "")
                val guidFile = jsonObject.getString("guidFile")
                Result.success(guidFile)
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error uploading video: ${e.message}"))
        }
    }
    suspend fun updateCompositionIssues(
        idBox: String,
        floGrade: String,
        floCod: String,
        issues: List<String>
    ): Result<Entities.UpdateCompositionIssuesResponse> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val issuesString = issues.joinToString(",")

            val request = Entities.UpdateCompositionIssuesRequest(
                idBox = idBox,
                floGrade = floGrade,
                floCod = floCod,
                issues = issuesString
            )

            val response = apiService.updateCompositionIssues(
                getAuthorizationHeader(),
                request
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Update failed: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error updating composition issues: ${e.message}"))
        }
    }
    suspend fun releaseBox(idBox: String, qcUser: String): Result<Entities.ReleaseBoxResponse> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val response = apiService.releaseBox(
                getAuthorizationHeader(),
                idBox,
                qcUser
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Release failed: ${response.code()}"))
            }

        } catch (e: Exception) {
            Result.failure(Exception("Error releasing box: ${e.message}"))
        }
    }
    suspend fun getReleasedBoxes(): Result<List<Entities.ReleaseBoxHistoryResponse>> {
        return try {
            ensureToken().onFailure { return Result.failure(it) }

            val response = apiService.getReleasedBoxes(getAuthorizationHeader())

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get failed: ${response.code()}"))
            }

        } catch (e: Exception) {
            Result.failure(Exception("Error getting release boxes: ${e.message}"))
        }
    }


}