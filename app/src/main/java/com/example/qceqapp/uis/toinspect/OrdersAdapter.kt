package com.example.qceqapp.uis.toinspect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.R
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.databinding.ItemOrderBinding

class OrdersAdapter(
    private var orders: List<Entities.QCOrderResponse>,
    private var growers: List<Entities.QCGrowerResponse> = emptyList(),
    private var customers: List<Entities.QCCustomerResponse> = emptyList(),
    private val onOrderClick: (Entities.QCOrderResponse) -> Unit,
    private val onQAClick: (Entities.QCOrderResponse) -> Unit,
    private val onDeleteClick: (Entities.QCOrderResponse) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(private val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Entities.QCOrderResponse) {
            val growerName = growers.find { it.groCod == order.grower }?.proVendor ?: order.grower ?: "-"
            val customerName = customers.find { it.codCustomer == order.customerid }?.custCompany ?: order.customerid ?: "-"

            val orderCode = "${order.grower ?: "-"}-${order.bxAWB ?: "-"}-${order.bxTELEX ?: "-"}"
            binding.tvOrderCode.text = orderCode
//            val boxId = order.boxId
//            if (boxId.isNullOrEmpty()) {
//                binding.tvOrderNumber.text = "-"
//            } else {
//                val boxCount = boxId.split(',').size
//                binding.tvOrderNumber.text = if (boxCount > 4) {
//                    "[$boxCount] Boxes"
//                } else {
//                    boxId
//                }
//            }
//            val boxId = order.boxId
//            if (boxId.isNullOrEmpty()) {
//                binding.tvOrderNumber.text = "-"
//            } else {
//                val parts = boxId.split(',').map { it.trim() }
//                val firstBox = parts.firstOrNull() ?: "-"
//                binding.tvOrderNumber.text = firstBox
//            }
            val boxId = order.boxId
            binding.tvOrderNumber.text =
                if (boxId.isNullOrBlank()) {
                    "-"
                } else {
                    boxId
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString(", ")
                }


            binding.ivAuthor.setImageResource(R.drawable.ic_flower)
            binding.ivAuthor.setColorFilter(
                binding.root.context.getColor(R.color.green)
            )
            binding.btnQA.text = order.author ?: "QA"
            when (order.author) {
                "QA" -> {
                    binding.btnQA.setBackgroundColor(
                        binding.root.context.getColor(R.color.yellow)
                    )
                    binding.btnQA.setTextColor(
                        binding.root.context.getColor(android.R.color.black)
                    )
                }
                "S" -> {
                    binding.btnQA.setBackgroundColor(
                        binding.root.context.getColor(R.color.blue)
                    )
                    binding.btnQA.setTextColor(
                        binding.root.context.getColor(android.R.color.white)
                    )
                }
                else -> {
                    binding.btnQA.setBackgroundColor(
                        binding.root.context.getColor(R.color.purple_500)
                    )
                    binding.btnQA.setTextColor(
                        binding.root.context.getColor(android.R.color.white)
                    )
                }
            }
            binding.ivIsSaved.isVisible = order.isSaved == "1"

            binding.cardOrder.setOnClickListener {
                onOrderClick(order)
            }

            binding.btnQA.setOnClickListener {
                onQAClick(order)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(order)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateData(
        newOrders: List<Entities.QCOrderResponse>,
        newGrowers: List<Entities.QCGrowerResponse> = growers,
        newCustomers: List<Entities.QCCustomerResponse> = customers
    ) {
        orders = newOrders
        growers = newGrowers
        customers = newCustomers
        notifyDataSetChanged()
    }

    fun updateOrders(newOrders: List<Entities.QCOrderResponse>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}