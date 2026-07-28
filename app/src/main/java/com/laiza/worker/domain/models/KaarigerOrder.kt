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
    val deliveredQuantity: Int? = null,
    val deliveryColor: String? = null,
    val deliveryNotes: String? = null,
    val deliverySubmittedAt: Long? = null,
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val rejectionReason: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)

data class OrderMaterial(
    val materialId: String,
    val materialName: String,
    val quantity: Double,
    val unit: String
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
    APPROVED,
    REJECTED;

    fun displayName(): String = when (this) {
        ASSIGNED -> "Assigned"
        PENDING_APPROVAL -> "Pending Approval"
        APPROVED -> "Approved"
        REJECTED -> "Rejected"
    }
}
