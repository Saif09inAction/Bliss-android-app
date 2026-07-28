package com.laiza.worker.domain.models

data class SaleRecord(
    val id: String,
    val finishedProductId: String,
    val finishedProductName: String,
    val quantity: Int,
    val clientName: String,
    val clientPhone: String,
    val courierName: String,
    val trackingNumber: String,
    val date: String,
    val time: String,
    val soldBy: String,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)
