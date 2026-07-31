package com.laiza.worker.domain.models

import java.util.UUID

data class OrderApprovalRecord(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val productName: String,
    val kaarigerId: String,
    val kaarigerName: String,
    val batchQuantity: Int,
    val rejectedQuantity: Int = 0,
    val approvedTotalAfter: Int,
    val targetQuantity: Int,
    val color: String = "",
    /** e.g. "Red:10, Blue:20" */
    val colorBreakdown: String = "",
    val verifiedByName: String,
    val verifiedByPhone: String,
    val verifiedAt: Long = System.currentTimeMillis()
)
