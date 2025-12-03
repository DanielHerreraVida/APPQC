package com.example.qceqapp.uis.viewhistory

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import com.example.qceqapp.data.model.Entities.QCHistoryResponse

class HistoryAdapter(
    private var originalList: List<QCHistoryResponse>,
    private val inspectionSelected: MutableList<String>,
    private val onItemClick: (QCHistoryResponse) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>(), Filterable {

    private var filteredList: List<QCHistoryResponse> = originalList
    private val historyFilter = HistoryFilter()

    private val clickDebouncer = ClickDebouncer(800L)

    companion object {
        private const val COLOR_VERDE = "#4CAF50"
        private const val COLOR_BLANCO = "#FFFFFF"

        fun serializeFilterDictionary(filterMap: Map<String, String>): String {
            return Gson().toJson(filterMap)
        }

        fun deserializeFilterDictionary(json: String): Map<String, String> {
            val type = object : TypeToken<Map<String, String>>() {}.type
            return Gson().fromJson(json, type)
        }

        fun serializeFilterStringList(list: List<String>): String {
            return Gson().toJson(list)
        }

        fun deserializeFilterStringList(json: String): List<String> {
            if (json.isEmpty()) return emptyList()
            val type = object : TypeToken<List<String>>() {}.type
            return Gson().fromJson(json, type)
        }
    }

    private class ClickDebouncer(private val delayMillis: Long = 800L) {
        private var lastClickTime = 0L

        fun canClick(): Boolean {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < delayMillis) {
                return false
            }
            lastClickTime = currentTime
            return true
        }

        fun reset() {
            lastClickTime = 0L
        }
    }

    inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val llQCHistoryRow: LinearLayout = view.findViewById(R.id.LLQCHistoryRow)
        val imageView1: ImageView = view.findViewById(R.id.imageView1)
        val tvOrder: TextView = view.findViewById(R.id.tV_order)
        val tvFirstData: TextView = view.findViewById(R.id.tV_FirstData)
        val tvSecondData: TextView = view.findViewById(R.id.tV_SecondData)
        val tvThirdData: TextView = view.findViewById(R.id.tV_ThirdData)
        val ivQCHAuthor: ImageView = view.findViewById(R.id.iV_QCHAuthor)
        val ivQCInspection: ImageView = view.findViewById(R.id.iV_QCInspection)

        fun bind(item: QCHistoryResponse) {
            if (inspectionSelected.contains(item.boxIdToInspect)) {
                llQCHistoryRow.setBackgroundColor(Color.parseColor(COLOR_VERDE))
            } else {
                llQCHistoryRow.setBackgroundColor(Color.parseColor(COLOR_BLANCO))
            }

            val awbX = "0000${item.bxAWB ?: ""}"
            val bAWB = awbX.substring(awbX.length - 4)
            val orderLine = "${item.grower}-$bAWB-${item.bxTELEX}-${item.bxNUM}"

            val barcodeLine = if (item.boxId.isNullOrEmpty()) {
                "ORDER: ${item.orderNum}-${item.orderRow}"
            } else {
                val boxCount = item.boxId?.split(',')?.size ?: 0
                "ORDER: ${item.orderNum}-${item.orderRow} BOXES: $boxCount"
            }

            val authorLine = "Author: [${item.author}] QAInspector:[${item.qaInspector}] QAReason:${item.qaReason}"

            val dateLine = item.inspectionTime ?: ""

            tvOrder.text = orderLine
            tvFirstData.text = barcodeLine
            tvSecondData.text = authorLine
            tvThirdData.text = dateLine

            if (item.author?.equals("QA", ignoreCase = true) == true) {
                ivQCHAuthor.setImageResource(R.drawable.letter_qa)
            } else {
                ivQCHAuthor.setImageResource(R.drawable.letter_s)
            }

            when (item.inspectionStatus) {
                "1" -> ivQCInspection.setImageResource(R.drawable.interface_check)
                "0" -> ivQCInspection.setImageResource(R.drawable.interface_uncheck)
                else -> ivQCInspection.setImageResource(R.drawable.interfaceuninspected)
            }

            when (item.qaReason?.trim()) {
                "Saved" -> imageView1.setImageResource(R.drawable.ic_inspsaved)
                "Sent" -> imageView1.setImageResource(R.drawable.ic_inspsent)
                "Modified" -> imageView1.setImageResource(R.drawable.icissaved)
                else -> imageView1.setImageResource(R.drawable.ic_flower)
            }

            itemView.setOnClickListener {
                if (clickDebouncer.canClick()) {
                    onItemClick(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_qc_history_row, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size

    fun updateData(newList: List<QCHistoryResponse>) {
        originalList = newList
        filteredList = newList
        notifyDataSetChanged()
    }

    fun resetDebouncer() {
        clickDebouncer.reset()
    }

    override fun getFilter(): Filter {
        return historyFilter
    }

    inner class HistoryFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()

            if (constraint.isNullOrEmpty()) {
                results.values = originalList
                results.count = originalList.size
                return results
            }

            val filterDictionary = deserializeFilterDictionary(constraint.toString())
            var filteredResults = originalList.toList()

            filterDictionary["Grower"]?.let { growerJson ->
                if (growerJson.isNotEmpty()) {
                    val growerCodes = deserializeFilterStringList(growerJson)
                    if (growerCodes.isNotEmpty()) {
                        filteredResults = filteredResults.filter { item ->
                            growerCodes.contains(item.grower)
                        }
                    }
                }
            }

            filterDictionary["Customer"]?.let { customerJson ->
                if (customerJson.isNotEmpty()) {
                    val customerCodes = deserializeFilterStringList(customerJson)
                    if (customerCodes.isNotEmpty()) {
                        filteredResults = filteredResults.filter { item ->
                            customerCodes.contains(item.customerId)
                        }
                    }
                }
            }
            filterDictionary["AWB"]?.let { awbJson ->
                if (awbJson.isNotEmpty()) {
                    val awbList = deserializeFilterStringList(awbJson)
                    if (awbList.isNotEmpty()) {
                        filteredResults = filteredResults.filter { item ->
                            awbList.contains(item.bxAWB)
                        }
                    }
                }
            }

            filterDictionary["OrderNum"]?.let { orderNumJson ->
                if (orderNumJson.isNotEmpty()) {
                    val orderNumList = deserializeFilterStringList(orderNumJson)
                    if (orderNumList.isNotEmpty()) {
                        filteredResults = filteredResults.filter { item ->
                            orderNumList.contains(item.orderNum)
                        }
                    }
                }
            }

            filterDictionary["Barcodes"]?.let { barcodesValue ->
                if (barcodesValue.isNotEmpty()) {
                    val barcodeList = barcodesValue.split(",").filter { it.isNotBlank() }
                    if (barcodeList.isNotEmpty()) {
                        filteredResults = filteredResults.filter { item ->
                            val boxIds = item.boxId?.split(',')?.map { it.trim() } ?: emptyList()
                            boxIds.any { boxId ->
                                barcodeList.any { barcode ->
                                    boxId.contains(barcode, ignoreCase = true)
                                }
                            }
                        }
                    }
                }
            }
            filterDictionary["InspectionState"]?.let { inspectionState ->
                if (inspectionState.isNotEmpty()) {
                    filteredResults = filteredResults.filter {
                        it.inspectionStatus == inspectionState
                    }
                }
            }

            filterDictionary["Author"]?.let { author ->
                if (author.isNotEmpty()) {
                    filteredResults = filteredResults.filter {
                        it.author?.equals(author, ignoreCase = true) == true
                    }
                }
            }

            val fechaInicial = filterDictionary["Fechai"]
            val fechaFinal = filterDictionary["Fechaf"]

            val userDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val itemDateFormat = SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.US)

            try {
                val dateInicio = fechaInicial?.takeIf { it.isNotEmpty() }?.let { userDateFormat.parse(it) }
                val dateFin = fechaFinal?.takeIf { it.isNotEmpty() }?.let { userDateFormat.parse(it) }

                val calendar = Calendar.getInstance()
                val startDate = dateInicio ?: Date(Long.MIN_VALUE)
                val endDate = if (dateFin != null) {
                    calendar.time = dateFin
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.time
                } else {
                    Date(Long.MAX_VALUE)
                }

                filteredResults = filteredResults.filter { item ->
                    try {
                        val itemDate = item.inspectionTime?.let { itemDateFormat.parse(it) }
                        itemDate != null &&
                                !itemDate.before(startDate) &&
                                !itemDate.after(endDate)
                    } catch (e: Exception) {
                        false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            results.values = filteredResults
            results.count = filteredResults.size
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            filteredList = (results?.values as? List<QCHistoryResponse>) ?: emptyList()
            notifyDataSetChanged()
        }
    }
}