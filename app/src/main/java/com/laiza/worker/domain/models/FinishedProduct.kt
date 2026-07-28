package com.laiza.worker.domain.models

data class FinishedProduct(
    val id: String,
    val name: String,
    val quantity: Int,
    val lastUpdatedBy: String,
    val lastUpdatedTime: Long,
    val imagePath: String? = null,
    val unitPrice: Double = 0.0,
    val color: String = "",
    val orderId: String? = null
)

data class RawMaterialConsumption(
    val rawMaterialId: String,
    val rawMaterialName: String,
    val quantityUsed: Double
)
