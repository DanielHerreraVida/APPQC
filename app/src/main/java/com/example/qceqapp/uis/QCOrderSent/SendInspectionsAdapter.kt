package com.example.qceqapp.uis.QCOrderSent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.data.model.Entities.QCHistoryResponse
import com.example.qceqapp.R

class SendInspectionsAdapter(
    private val items: List<QCHistoryResponse>,
    private val onItemClick: (QCHistoryResponse, Int) -> Unit
) : RecyclerView.Adapter<SendInspectionsAdapter.QCHistoryViewHolder>() {

    inner class QCHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView1: ImageView = itemView.findViewById(R.id.imageView1)
        val tvOrder: TextView = itemView.findViewById(R.id.tV_order)
        val tvFirstData: TextView = itemView.findViewById(R.id.tV_FirstData)
        val tvSecondData: TextView = itemView.findViewById(R.id.tV_SecondData)
        val tvThirdData: TextView = itemView.findViewById(R.id.tV_ThirdData)
        val ivAuthor: ImageView = itemView.findViewById(R.id.iV_QCHAuthor)
        val ivInspectionStatus: ImageView = itemView.findViewById(R.id.iV_QCInspection)

        fun bind(item: QCHistoryResponse, position: Int) {
            val orderLine = "${item.grower ?: ""}-${item.bxAWB ?: ""}-${item.bxTELEX ?: ""}-${item.bxNUM ?: ""}"
            tvOrder.text = orderLine
            val barcodeLine = if (item.boxId.isNullOrEmpty()) {
                "ORDER: ${item.orderNum ?: ""}-${item.orderRow ?: ""}"
            } else {
                val boxCount = item.boxId.split(',').size
                "ORDER: ${item.orderNum ?: ""}-${item.orderRow ?: ""} BOXES: $boxCount"
            }
            tvFirstData.text = barcodeLine
            val authorLine = "Author: [${item.author ?: ""}] QAInspector:[${item.qaInspector ?: ""}] QAReason:${item.qaReason ?: ""}"
            tvSecondData.text = authorLine
            tvThirdData.text = item.inspectionTime ?: ""

            if (item.author.equals("QA", ignoreCase = false)) {
                ivAuthor.setImageResource(R.drawable.letter_qa)
            } else {
                ivAuthor.setImageResource(R.drawable.letter_s)
            }

            when (item.inspectionStatus) {
                "1" -> ivInspectionStatus.setImageResource(R.drawable.interface_check)
                "0" -> ivInspectionStatus.setImageResource(R.drawable.interface_uncheck)
                else -> ivInspectionStatus.setImageResource(R.drawable.icissaved)
            }

            when (item.qaReason?.trim()) {
                "Saved" -> imageView1.setImageResource(R.drawable.ic_inspsaved)
                "Sent" -> imageView1.setImageResource(R.drawable.ic_inspsent)
                "Modified" -> imageView1.setImageResource(R.drawable.icissaved)
                else -> imageView1.setImageResource(R.drawable.ic_flower)
            }

            itemView.setOnClickListener {
                onItemClick(item, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QCHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_qc_history_row, parent, false)
        return QCHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: QCHistoryViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): QCHistoryResponse = items[position]

    fun getQCHistoryItem(position: Int): QCHistoryResponse = items[position]
}