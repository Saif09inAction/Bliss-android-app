package com.laiza.worker.domain.models

data class RawMaterial(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val minimumStock: Double,
    val supplier: String,
    val lastUpdatedBy: String,
    val lastUpdatedTime: Long,
    val imagePath: String? = null
)
