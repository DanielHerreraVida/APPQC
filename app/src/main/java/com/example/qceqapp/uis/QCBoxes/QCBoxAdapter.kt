package com.example.qceqapp.uis.QCBoxes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities

class QCBoxAdapter(
    private val selectedBoxes: MutableList<Entities.BoxToInspect>,
    private val onItemClick: ((Entities.BoxToInspect) -> Unit)? = null
) : RecyclerView.Adapter<QCBoxAdapter.QCBoxViewHolder>() {

    private var items: List<Entities.BoxToInspect> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QCBoxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.qcbox_list_row, parent, false)
        return QCBoxViewHolder(view)
    }

    override fun onBindViewHolder(holder: QCBoxViewHolder, position: Int) {
        val box = items[position]
        holder.bind(box, selectedBoxes)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(box)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<Entities.BoxToInspect>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun getItems(): List<Entities.BoxToInspect> = items

    class QCBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val farmTv: TextView = itemView.findViewById(R.id.FarmTV)
        private val barcodeTv: TextView = itemView.findViewById(R.id.BarcodeTV)
        private val typeTv: TextView = itemView.findViewById(R.id.TypeTV)
        private val varietyTv: TextView = itemView.findViewById(R.id.VarietyTV)
        private val colorTv: TextView = itemView.findViewById(R.id.ColorTV)
        private val gradeTv: TextView = itemView.findViewById(R.id.GradeTV)
        private val numTv: TextView = itemView.findViewById(R.id.NUMTV)
        private val upbTv: TextView = itemView.findViewById(R.id.UPBTV)

        fun bind(box: Entities.BoxToInspect, selectedBoxes: List<Entities.BoxToInspect>) {
            farmTv.text = box.farm ?: "-"
            barcodeTv.text = box.barcode ?: "-"
            typeTv.text = box.floType ?: "-"
            varietyTv.text = box.floVariety ?: "-"
            colorTv.text = box.floColor ?: "-"
            gradeTv.text = box.floGrade ?: "-"
            numTv.text = box.num ?: "-"
            upbTv.text = box.floUPB?.toString() ?: "-"

            val isSelected = selectedBoxes.any { it.barcode == box.barcode }
            itemView.setBackgroundColor(
                if (isSelected) Color.YELLOW else Color.WHITE
            )
        }
    }
}