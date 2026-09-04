package com.laiza.worker.domain.models

import java.util.UUID

data class OrderRepairLine(
    val type: String,
    val label: String,
    val quantity: Int,
    val pricePerPiece: Double,
    val lineTotal: Double
)

object RepairStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"

    /** Sentinel when repairing is recorded without an existing bill. */
    const val STANDALONE_ORDER_ID = "__standalone__"
}

data class OrderRepair(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String = RepairStatus.STANDALONE_ORDER_ID,
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
    val createdAt: Long = System.currentTimeMillis(),
    /** PENDING until admin approves; missing/blank on old docs = already deducted (APPROVED). */
    val status: String = RepairStatus.APPROVED,
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    /** Approved but not deducted yet — admin adds to a bill from Hisaab later. */
    val deferToNextBill: Boolean = false
) {
    val isApproved: Boolean
        get() = status.isBlank() || status == RepairStatus.APPROVED

    val isPending: Boolean
        get() = status == RepairStatus.PENDING

    val isStandalone: Boolean
        get() = orderId.isBlank() || orderId == RepairStatus.STANDALONE_ORDER_ID

    /**
     * Standalone repairing never cuts Remaining — only after admin links it to a bill
     * (matches admin: pending until “Add repairing to this bill”).
     */
    val countsAgainstRemaining: Boolean
        get() = false
}
