package com.example.qceqapp.uis.torelease

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.R
import com.example.qceqapp.databinding.ItemReleaseBinding

class PendingReleaseAdapter(
    private var pendingItems: List<PendingReleaseItem>,
    private val onItemClick: (PendingReleaseItem) -> Unit,
    private val onDeleteClick: (PendingReleaseItem) -> Unit
) : RecyclerView.Adapter<PendingReleaseAdapter.PendingViewHolder>() {

    inner class PendingViewHolder(private val binding: ItemReleaseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PendingReleaseItem) {
            binding.tvOrderCode.text = "Box: ${item.box}"
            binding.tvOrderNumber.text = "Pending release"
            binding.ivAuthor.setImageResource(R.drawable.ic_caja)

            binding.btnQA.text = "PENDING"
            binding.btnQA.setBackgroundColor(
                binding.root.context.getColor(R.color.green)
            )
            binding.btnQA.setTextColor(
                binding.root.context.getColor(android.R.color.white)
            )

            binding.ivIsSaved.isVisible = false

            binding.cardOrder.setOnClickListener {
                onItemClick(item)
            }

            binding.btnQA.setOnClickListener {
                onItemClick(item)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingViewHolder {
        val binding = ItemReleaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingViewHolder, position: Int) {
        holder.bind(pendingItems[position])
    }

    override fun getItemCount(): Int = pendingItems.size

    fun updateData(newItems: List<PendingReleaseItem>) {
        // DiffUtil: solo actualiza/anima las filas que cambiaron en vez de re-renderizar TODA
        // la lista (notifyDataSetChanged). Identidad por `box` (clave natural) — así borrar un
        // item solo anima esa fila y no rebindea las demás. Evita el lag al borrar pendings.
        val old = pendingItems
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                old[oldItemPosition].box == newItems[newItemPosition].box
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                old[oldItemPosition] == newItems[newItemPosition]
        })
        pendingItems = newItems
        diff.dispatchUpdatesTo(this)
    }

    fun addItem(item: PendingReleaseItem) {
        val mutableList = pendingItems.toMutableList()
        mutableList.add(0, item)
        pendingItems = mutableList
        notifyItemInserted(0)
    }

    fun removeItem(item: PendingReleaseItem) {
        val position = pendingItems.indexOf(item)
        if (position != -1) {
            val mutableList = pendingItems.toMutableList()
            mutableList.removeAt(position)
            pendingItems = mutableList
            notifyItemRemoved(position)
        }
    }

    fun clearAll() {
        val size = pendingItems.size
        pendingItems = emptyList()
        notifyItemRangeRemoved(0, size)
    }
}