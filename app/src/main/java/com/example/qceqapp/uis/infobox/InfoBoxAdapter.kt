package com.example.qceqapp.uis.infobox

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.databinding.ItemBoxBinding

class InfoBoxAdapter(private val boxes: List<Entities.BoxToInspect>) :
    RecyclerView.Adapter<InfoBoxAdapter.BoxViewHolder>() {

    inner class BoxViewHolder(private val binding: ItemBoxBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(box: Entities.BoxToInspect) {
            binding.tvBarcode.text = box.barcode
            binding.tvVariety.text = "${box.floVariety} - ${box.floColor}"
            binding.tvGrade.text = "Grade: ${box.floGrade}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        val binding = ItemBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BoxViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        holder.bind(boxes[position])
    }

    override fun getItemCount(): Int = boxes.size
}
