package com.example.qceqapp.utils

import com.example.qceqapp.data.model.Entities

object GlobalOrder {
    private var currentOrder: Entities.QCOrderResponse? = null

    fun set(order: Entities.QCOrderResponse?) {
        currentOrder = order
    }

    fun get(): Entities.QCOrderResponse? = currentOrder

    fun clear() {
        currentOrder = null
    }

    // Métodos de conveniencia para acceder a campos específicos
    fun getBxNUM(): String? = currentOrder?.bxNUM
    fun getBxAWB(): String? = currentOrder?.bxAWB
    fun getBxTELEX(): String? = currentOrder?.bxTELEX
    fun getOrderNum(): String? = currentOrder?.orderNum
}