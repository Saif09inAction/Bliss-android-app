package com.laiza.worker.domain.models

import java.util.UUID

data class KaarigerOrder(
    val id: String = UUID.randomUUID().toString(),
    val kaarigerId: String,
    val kaarigerName: String,
    val productName: String,
    val targetQuantity: Int,
    val color: String = "",
    val rawMaterials: List<OrderMaterial> = emptyList(),
    val totalDealAmount: Double,
    val pricePerPiece: Double? = null,
    val pricingType: OrderPricingType = OrderPricingType.OVERALL,
    val status: OrderStatus = OrderStatus.ASSIGNED,
    val approvedQuantity: Int = 0,
    val deliveredQuantity: Int? = null,
    val deliveryColor: String? = null,
    val deliveryNotes: String? = null,
    val deliverySubmittedAt: Long? = null,
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val rejectionReason: String? = null,
    val materialUsageReported: Boolean = false,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val originalDealAmount: Double? = null,
    val repairDeductionTotal: Double = 0.0,
    val products: List<OrderProductLine> = emptyList(),
    val productsTotal: Double = 0.0,
    val materialDeductions: List<OrderRepairLine> = emptyList(),
    val materialDeductionsTotal: Double = 0.0,
    val kharchaGiven: Double = 0.0
) {
    fun remainingQuantity(): Int = (targetQuantity - approvedQuantity).coerceAtLeast(0)

    fun effectiveDealAmount(): Double {
        val original = originalDealAmount ?: totalDealAmount
        return (original - repairDeductionTotal).coerceAtLeast(0.0)
    }
}

data class OrderMaterial(
    val materialId: String,
    val materialName: String,
    val quantity: Double,
    val unit: String,
    val usedQuantity: Double? = null,
    val remainingQuantity: Double? = null
)

/** One product line on a Kaarigar bill — price is always per piece. */
data class OrderProductLine(
    val productName: String,
    val quantity: Int,
    val pricePerPiece: Double,
    val lineTotal: Double
)

data class KaarigerOrderPayment(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val kaarigerId: String,
    val amount: Double,
    val date: String,
    val time: String,
    val remarks: String? = null,
    val createdBy: String
)

enum class OrderPricingType {
    PER_PIECE, OVERALL
}

enum class OrderStatus {
    ASSIGNED,
    PENDING_APPROVAL,
    COMPLETED,
    REJECTED;

    fun displayName(): String = when (this) {
        ASSIGNED -> "In Progress"
        PENDING_APPROVAL -> "Pending Approval"
        COMPLETED -> "Completed"
        REJECTED -> "Rejected"
    }
}
