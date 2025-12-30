package com.example.qceqapp.uis.torelease

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.databinding.ItemReleaseBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReleaseAdapter(
    private var boxes: List<Entities.ReleaseBoxHistoryResponse>,
    private val onBoxClick: (Entities.ReleaseBoxHistoryResponse) -> Unit,
    private val onUserClick: (Entities.ReleaseBoxHistoryResponse) -> Unit,
    private val onDeleteClick: (Entities.ReleaseBoxHistoryResponse) -> Unit
) : RecyclerView.Adapter<ReleaseAdapter.ReleaseViewHolder>() {

    companion object {
        private const val TAG = "ReleaseAdapter"
    }

    inner class ReleaseViewHolder(private val binding: ItemReleaseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(box: Entities.ReleaseBoxHistoryResponse) {
            Log.d(TAG, "Binding box: ${box.box} - ${box.user} - ${box.numOrder}")

            binding.tvOrderCode.text = "Box: ${box.box}"
            binding.tvOrderNumber.text = if (box.numOrder.isNotEmpty()) {
                "Order: ${box.numOrder}"
            } else {
                "No order assigned"
            }
            binding.ivAuthor.setImageResource(R.drawable.ic_caja_release)

            val rawUser = box.user.uppercase().trim()
            val userName = formatUserName(rawUser)

            binding.btnQA.text = userName
            binding.btnQA.setBackgroundColor(
                binding.root.context.getColor(R.color.blue)
            )
            binding.btnQA.setTextColor(
                binding.root.context.getColor(android.R.color.white)
            )

            binding.ivIsSaved.isVisible = false

            binding.cardOrder.setOnClickListener {
                onBoxClick(box)
            }

            binding.btnQA.setOnClickListener {
                onUserClick(box)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(box)
            }
            binding.root.isVisible = true
        }

        private fun formatUserName(userName: String): String {
            return when {
                userName.length <= 7 -> userName
                userName.length <= 14 -> {
                    userName.chunked(7).joinToString("\n")
                }
                else -> {
                    userName.take(21).chunked(7).joinToString("\n")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReleaseViewHolder {
        val binding = ItemReleaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReleaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReleaseViewHolder, position: Int) {
        holder.bind(boxes[position])
    }

    override fun getItemCount(): Int {
        return boxes.size
    }

    fun updateData(newBoxes: List<Entities.ReleaseBoxHistoryResponse>) {
        newBoxes.forEachIndexed { index, box ->
        }

        boxes = newBoxes
        notifyDataSetChanged()

    }
}