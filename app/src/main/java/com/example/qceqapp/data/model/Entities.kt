package com.example.qceqapp.data.model
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
class Entities {
    data class TokenRequest(
        @SerializedName("user")
        val user: String,
        @SerializedName("password")
        val password: String
    )

    data class TokenResponse(
        @SerializedName("token")
        val token: String,
        @SerializedName("message")
        val message: String? = null,
        @SerializedName("success")
        val success: Boolean = true
    )

    data class LoginRequest(
        @SerializedName("login")
        val login: String,
        @SerializedName("password")
        val password: String
    )

    data class LoginResponse(
        @SerializedName("qcUsername")
        val qcUsername: String? = null,

        @SerializedName("qcuRole")
        val qcuRole: String? = null,

        @SerializedName("qcuName")
        val qcuName: String? = null,

        @SerializedName("qcuCompany")
        val qcuCompany: String? = null,

        @SerializedName("qcuEmail")
        val qcuEmail: String? = null
    )

    data class ApiResponse<T>(
        @SerializedName("success")
        val success: Boolean,
        @SerializedName("message")
        val message: String?,
        @SerializedName("data")
        val data: T?
    )

    @Parcelize
    data class QCCustomerResponse(
        @SerializedName("cod_customer")
        val codCustomer: String,
        @SerializedName("cust_company")
        val custCompany: String
    ): Parcelable
    @Parcelize
    data class QCGrowerResponse(
        @SerializedName("gro_cod")
        val groCod: String,
        @SerializedName("pro_vendor")
        val proVendor: String
    ): Parcelable
    data class QCOrderResponse(
        @SerializedName("orderNum") val orderNum: String,
        @SerializedName("rowNum") val rowNum: String,
        @SerializedName("observation") val observation: String?,
        @SerializedName("reason") val reason: String?,
        @SerializedName("bxAWB") val bxAWB: String?,
        @SerializedName("bxTELEX") val bxTELEX: String?,
        @SerializedName("bxNUM") val bxNUM: String?,
        @SerializedName("boxId") val boxId: String?,
        @SerializedName("boxIdToInspect") val boxIdToInspect: String?,
        @SerializedName("author") val author: String?,
        @SerializedName("isScanned") val isScanned: String?,
        @SerializedName("isPrinted") val isPrinted: String?,
        @SerializedName("isSaved") val isSaved: String?,
        @SerializedName("grower") val grower: String?,
        @SerializedName("customer") val customer: String?,
        @SerializedName("customerid") val customerid: String?,
        @SerializedName("printed") val printed: String? = null,
        @SerializedName("saved") val saved: String? = null
    ): Serializable
    @Parcelize
    data class FilterData(
        val author: String = "",
        val grower: String = "",
        val customer: String = "",
        val printed: String = "",
        val saved: String = "",
        val awb: String = "",
        val num: String = "",
        val barcodes: String = ""
    ) : Parcelable {
        fun isEmpty(): Boolean {
            return author.isEmpty() &&
                    grower.isEmpty() &&
                    customer.isEmpty() &&
                    printed.isEmpty() &&
                    saved.isEmpty() &&
                    awb.isEmpty() &&
                    num.isEmpty() &&
                    barcodes.isEmpty()
        }
    }
    data class OrderInspect(
        val orderCode: String,
        val orderNumber: String,
        val status: OrderStatus = OrderStatus.PENDING
    )

    enum class OrderStatus {
        PENDING,
        QA_APPROVED,
        REJECTED
    }
    //

    data class RejectBoxRequest(
        @SerializedName("IdBox")
        val idBox: String,
        @SerializedName("OrderNum")
        val orderNum: String,
        @SerializedName("RowNum")
        val rowNum: String,
        @SerializedName("QaInspector")
        val qaInspector: String,
        @SerializedName("QaReason")
        val qaReason: String
    )

    data class ScanToInspectResponse(
        @SerializedName("idToInspect")
        val idToInspect: String,
        @SerializedName("lstSelectedBoxes")
        val lstSelectedBoxes: List<BoxToInspect>? = emptyList(),
        @SerializedName("lstRelatedBoxes")
        val lstRelatedBoxes: List<BoxToInspect>? = emptyList(),
        @SerializedName("lstAllBoxes")
        val lstAllBoxes: List<BoxToInspect>? = emptyList()
    )


    @Parcelize

    data class lstSelectedBoxes(
        @SerializedName("farm")
        val farm: String,
        @SerializedName("barcode")
        val barcode: String,
        @SerializedName("num")
        val num: String,
        @SerializedName("floType")
        val floType: String,
        @SerializedName("floVariety")
        val floVariety: String,
        @SerializedName("floColor")
        val floColor: String,
        @SerializedName("floGrade")
        val floGrade: String,
        @SerializedName("floCT")
        val floCT: String,
        @SerializedName("floBox")
        val floBox: String,
        @SerializedName("floDescript")
        val floDescript: String,
        @SerializedName("floUPB")
        val floUPB: String
    ): Parcelable
    @Parcelize

    data class lstRelatedBoxes(
        @SerializedName("farm")
        val farm: String,
        @SerializedName("barcode")
        val barcode: String,
        @SerializedName("num")
        val num: String,
        @SerializedName("floType")
        val floType: String,
        @SerializedName("floVariety")
        val floVariety: String,
        @SerializedName("floColor")
        val floColor: String,
        @SerializedName("floGrade")
        val floGrade: String,
        @SerializedName("floCT")
        val floCT: String,
        @SerializedName("floBox")
        val floBox: String,
        @SerializedName("floDescript")
        val floDescript: String,
        @SerializedName("floUPB")
        val floUPB: String
    ): Parcelable
    @Parcelize

    data class lstAllBoxes(
        @SerializedName("farm")
        val farm: String,
        @SerializedName("barcode")
        val barcode: String,
        @SerializedName("num")
        val num: String,
        @SerializedName("floType")
        val floType: String,
        @SerializedName("floVariety")
        val floVariety: String,
        @SerializedName("floColor")
        val floColor: String,
        @SerializedName("floGrade")
        val floGrade: String,
        @SerializedName("floCT")
        val floCT: String,
        @SerializedName("floBox")
        val floBox: String,
        @SerializedName("floDescript")
        val floDescript: String,
        @SerializedName("floUPB")
        val floUPB: String
    ): Parcelable
    data class QCHistoryResponse(
        @SerializedName("orderNum") val orderNum: String? = null,
        @SerializedName("orderRow") val orderRow: String? = null,
        @SerializedName("boxId") val boxId: String? = null,
        @SerializedName("boxIdToInspect") val boxIdToInspect: String? = null,
        @SerializedName("author") val author: String? = null,
        @SerializedName("qaInspector") val qaInspector: String? = null,
        @SerializedName("qaReason") val qaReason: String? = null,
        @SerializedName("inspectionTime") val inspectionTime: String? = null,
        @SerializedName("inspectionStatus") val inspectionStatus: String? = null,
        @SerializedName("bxAWB") val bxAWB: String? = null,
        @SerializedName("bxTELEX") val bxTELEX: String? = null,
        @SerializedName("bxNUM") val bxNUM: String? = null,
        @SerializedName("grower") val grower: String? = null,
        @SerializedName("flowerGrade") val flowerGrade: String? = null,
        @SerializedName("flowerColor") val flowerColor: String? = null,
        @SerializedName("boxInfo") val boxInfo: String? = null,
        @SerializedName("ordDescription") val ordDescription: String? = null,
        @SerializedName("customerId") val customerId: String? = null,
        @SerializedName("bProduct") val bProduct: String? = null,
        @SerializedName("qcActions") val qcActions: String? = null
    ): Serializable
    data class GetOrderByBoxRequest(
        @SerializedName("IdBox")
        val idBox: String,
        @SerializedName("QaInspector")
        val qaInspector: String,
        @SerializedName("QaReason")
        val qaReason: String
    )
    @Parcelize
    data class BoxToInspect(
        @SerializedName("farm") val farm: String = "",
        @SerializedName("barcode") val barcode: String = "",
        @SerializedName("floType") val floType: String = "",
        @SerializedName("floVariety") val floVariety: String = "",
        @SerializedName("floColor") val floColor: String = "",
        @SerializedName("floGrade") val floGrade: String = "",
        @SerializedName("floCT") val floCT: String = "",
        @SerializedName("floBox") val floBox: String = "",
        @SerializedName("floDescript") val floDescript: String = "",
        @SerializedName("floUPB") val floUPB: String = "",
        @SerializedName("num") val num: String = ""
    ) : Parcelable
    @Parcelize
    data class BoxesToInspectResponse(
        @SerializedName("lstSelectedBoxes") val selectedBoxes: List<BoxToInspect> = emptyList(),
        @SerializedName("lstRelatedBoxes") val relatedBoxes: List<BoxToInspect> = emptyList(),
        @SerializedName("lstAllBoxes") val allBoxes: List<BoxToInspect> = emptyList()
    ) : Parcelable
    data class SelectedBox(
        val barcode: String
    )

    @Parcelize
    data class OrderByBox(
        @SerializedName("orderNum") val orderNum: String = "",
        @SerializedName("rowNum") val rowNum: String = "",
        @SerializedName("observation") val observation: String = "",
        @SerializedName("reason") val reason: String = "",
        @SerializedName("bxAWB") val bxAWB: String = "",
        @SerializedName("bxTELEX") val bxTELEX: String = "",
        @SerializedName("bxNUM") val bxNUM: String = "",
        @SerializedName("boxId") val boxId: String = "",
        @SerializedName("boxIdToInspect") val boxIdToInspect: String = "",
        @SerializedName("author") val author: String = "",
        @SerializedName("isScanned") val isScanned: String = "",
        @SerializedName("isPrinted") val isPrinted: String = "",
        @SerializedName("isSaved") val isSaved: String = "",
        @SerializedName("grower") val grower: String = "",
        @SerializedName("customer") val customer: String = "",
        @SerializedName("customerid") val customerid: String = ""
    ) : Parcelable
    data class RejectScannedBoxRequest(
        @SerializedName("IdBox")
        val idBox: String,
        @SerializedName("QaInspector")
        val qaInspector: String,
        @SerializedName("QaReason")
        val qaReason: String
    )
    data class BoxCompositionResponse(
        @SerializedName("inb_num_box")
        val inbNumBox: String,
        @SerializedName("flo_grade")
        val floGrade: String,
        @SerializedName("flo_cod")
        val floCod: String,
        @SerializedName("composition")
        val composition: String,
        @SerializedName("issues")
        var issues: List<String>
    )
    data class BoxInfoResponse(
        @SerializedName("grower")
        val grower: String,
        @SerializedName("floGrade")
        val floGrade: String,
        @SerializedName("floColor")
        val floColor: String,
        @SerializedName("boxType")
        val boxType: String,
        @SerializedName("boxDescription")
        val boxDescription: String,
        @SerializedName("customer")
        val customer: String,
        @SerializedName("awbNo")
        val awbNo: String,
        @SerializedName("invoiceNum")
        val invoiceNum: String,
        @SerializedName("telexNum")
        val telexNum: String,
        @SerializedName("num")
        val num: String,
        @SerializedName("numOrd")
        val numOrd: String,
        @SerializedName("bProduct")
        val bProduct: String
    )
    data class QCIssueResponse(
        @SerializedName("idIssue")
        val idIssue: String,
        @SerializedName("descriptionIes")
        val descriptionIes: String,
        @SerializedName("descriptionIen")
        val descriptionIen: String,
        @SerializedName("categoryI")
        val categoryI: String
    )
    data class QCActionResponse(
        @SerializedName("idAction")
        val idAction: String,
        @SerializedName("descriptionAes")
        val descriptionAes: String,
        @SerializedName("descriptionAen")
        val descriptionAen: String
    )
    data class SavedBoxResponse(
        @SerializedName("boxId")
        val boxId: String,
        @SerializedName("boxIdToInspect")
        val boxIdToInspect: String,
        @SerializedName("isSaved")
        val isSaved: String,
        @SerializedName("orderNum")
        val orderNum: String,
        @SerializedName("boxAWB")
        val boxAWB: String,
        @SerializedName("boxTELEX")
        val boxTELEX: String,
        @SerializedName("boxNUM")
        val boxNUM: String,
        @SerializedName("boxIssue")
        val boxIssue: String,
        @SerializedName("boxAction")
        val boxAction: String,
        @SerializedName("boxIssueDescript")
        val boxIssueDescript: String,
        @SerializedName("qaInspector")
        val qaInspector: String,
        @SerializedName("imagesList")
        val imagesList: List<String>,
        @SerializedName("videosList")
        val videosList: List<String>,
        @SerializedName("qaInspectionStatus")
        val qaInspectionStatus: String
    )
    data class SaveQCBoxRequest(
        @SerializedName("IdBox")
        val idBox: String,
        @SerializedName("OrdNum")
        val ordNum: Int,
        @SerializedName("AwbNum")
//        val awbNum: Int,
        val awbNum: String,
        @SerializedName("TelexNum")
//        val telexNum: Int,
        val telexNum: String,
        @SerializedName("Num")
        val num: Int,
        @SerializedName("IssueC")
        val issueC: String,
        @SerializedName("ActionC")
        val actionC: String,
        @SerializedName("IssueDes")
        val issueDes: String,
        @SerializedName("QaInsp")
        val qaInsp: String,
        @SerializedName("ListImages")
        val listImages: List<String>,
        @SerializedName("ListVideos")
        val listVideos: List<String>,
        @SerializedName("InspectStatus")
        val inspectStatus: String,
        @SerializedName("BarcodesToI")
        val barcodesToI: String
    )
    data class SaveQCHistorySentRequest(
        @SerializedName("idBox")
        val idBox: String,
        @SerializedName("ordNum")
        val ordNum: Int,
        @SerializedName("awbNum")
        val awbNum: String,
        @SerializedName("telexNum")
        val telexNum: String,
        @SerializedName("num")
        val num: Int,
        @SerializedName("issueC")
        val issueC: String,
        @SerializedName("actionC")
        val actionC: String,
        @SerializedName("issueDes")
        val issueDes: String,
        @SerializedName("qaInsp")
        val qaInsp: String,
        @SerializedName("listImages")
        val listImages: List<String>,
        @SerializedName("listVideos")
        val listVideos: List<String>,
        @SerializedName("inspectStatus")
        val inspectStatus: Int,
        @SerializedName("barcodesToI")
        val barcodesToI: String
    )
    data class SendQCHistorySentRequest(
        @SerializedName("idBox")
        val idBox: String,
        @SerializedName("ordNum")
        val ordNum: Int,
        @SerializedName("awbNum")
        val awbNum: String,
        @SerializedName("telexNum")
        val telexNum: String,
        @SerializedName("num")
        val num: Int,
        @SerializedName("issueC")
        val issueC: String,
        @SerializedName("actionC")
        val actionC: String,
        @SerializedName("issueDes")
        val issueDes: String,
        @SerializedName("qaInsp")
        val qaInsp: String,
        @SerializedName("listImages")
        val listImages: List<String>,
        @SerializedName("listVideos")
        val listVideos: List<String>,
        @SerializedName("inspectStatus")
        val inspectStatus: Int,
        @SerializedName("barcodesToI")
        val barcodesToI: String
    )
    data class UpdateCompositionsRequest(
        @SerializedName("idBox")
        val idBox: Int,
        @SerializedName("floGrade")
        val floGrade: String,
        @SerializedName("floCod")
        val floCod: String,
        @SerializedName("issues")
        val issues: String
    )

    data class SendBoxRequestToInpect(
        @SerializedName("idBox")
        val idBox: String,
        @SerializedName("ordNum")
        val ordNum: Int,
        @SerializedName("awbNum")
        val awbNum: String,
        @SerializedName("telexNum")
        val telexNum: String,
        @SerializedName("num")
        val num: Int,
        @SerializedName("issueC")
        val issueC: String,
        @SerializedName("actionC")
        val actionC: String,
        @SerializedName("issueDes")
        val issueDes: String,
        @SerializedName("qaInsp")
        val qaInsp: String,
        @SerializedName("listImages")
        val listImages: List<String>,
        @SerializedName("listVideos")
        val listVideos: List<String>,
        @SerializedName("inspectStatus")
        val inspectStatus: Int,
        @SerializedName("barcodesToI")
        val barcodesToI: String
    )

    data class SendBoxRequest(
        @SerializedName("IdBox")
        val idBox: String,
        @SerializedName("OrdNum")
        val ordNum: Int,
        @SerializedName("AwbNum")
        val awbNum: String,
        @SerializedName("TelexNum")
        val telexNum: String,
        @SerializedName("Num")
        val num: Int,
        @SerializedName("IssueC")
        val issueC: String,
        @SerializedName("ActionC")
        val actionC: String,
        @SerializedName("IssueDes")
        val issueDes: String,
        @SerializedName("QaInsp")
        val qaInsp: String,
        @SerializedName("ListImages")
        val listImages: List<String>,
        @SerializedName("ListVideos")
        val listVideos: List<String>,
        @SerializedName("InspectStatus")
        val inspectStatus: String,
        @SerializedName("BarcodesToI")
        val barcodesToI: String
    )

    data class QCHistoryItem(
        @SerializedName("orderNum")
        val orderNum: String = "",

        @SerializedName("orderRow")
        val orderRow: String = "",

        @SerializedName("BoxId")
        val boxId: String = "",

        @SerializedName("BoxIdToInspect")
        val boxIdToInspect: String = "",

        @SerializedName("Author")
        val author: String = "",

        @SerializedName("QAInspector")
        val qaInspector: String = "",

        @SerializedName("QAReason")
        val qaReason: String = "",

        @SerializedName("InspectionTime")
        val inspectionTime: String = "",

        @SerializedName("InspectionStatus")
        val inspectionStatus: String = "",

        @SerializedName("bxAWB")
        val bxAWB: String = "",

        @SerializedName("bxTELEX")
        val bxTELEX: String = "",

        @SerializedName("bxNUM")
        val bxNUM: String = "",

        @SerializedName("Grower")
        val grower: String = "",

        @SerializedName("FlowerGrade")
        val flowerGrade: String = "",

        @SerializedName("FlowerColor")
        val flowerColor: String = "",

        @SerializedName("BoxInfo")
        val boxInfo: String = "",

        @SerializedName("OrdDescription")
        val ordDescription: String = "",

        @SerializedName("CustomerId")
        val customerId: String = "",

        @SerializedName("BProduct")
        val bProduct: String = ""
    )
    data class SendGroupInspectionRequest(
        @SerializedName("Inspections")
        val inspections: String
    )
    data class UpdateCompositionIssuesRequest(
        val idBox: String,
        val floGrade: String,
        val floCod: String,
        val issues: String
    )

    data class UpdateCompositionIssuesResponse(
        val message: String,
        val success: Boolean
    )
    data class ReleaseBoxResponse(
        val message: String,
        val status: Int
    )
    data class ReleaseBoxHistoryResponse(
        val box: Long,
        val numOrder: String,
        val dtModify: String,
        val user: String
    )
    data class PendingReleaseItem(
        val box: String,
        val scannedAt: Long = System.currentTimeMillis()
    )

    data class ReleaseBoxesRequest(
        val boxIds: List<Int>,
        val user: String
    )
    data class SimpleReleaseResponse(
        val success: Int,
        val failed: Int,
        val failedBoxIds: List<Int>
    )
}