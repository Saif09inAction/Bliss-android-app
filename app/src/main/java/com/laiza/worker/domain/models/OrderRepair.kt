package com.laiza.worker.domain.models

import java.util.UUID

data class OrderRepairLine(
    val type: String,
    val label: String,
    val quantity: Int,
    val pricePerPiece: Double,
    val lineTotal: Double
)

data class OrderRepair(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val kaarigerId: String,
    val kaarigerName: String,
    val productName: String,
    val faultyQuantity: Int = 0,
    val faultyPricePerPiece: Double = 0.0,
    val faultyTotal: Double = 0.0,
    val items: List<OrderRepairLine> = emptyList(),
    val totalRepairCost: Double = 0.0,
    val originalDealAmount: Double = 0.0,
    val dealAfterThisRepair: Double = 0.0,
    val notes: String? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
