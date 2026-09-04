package com.laiza.worker.domain.models

data class OrderReceiptData(
    val orderId: String,
    val kaarigerName: String,
    val productName: String,
    val color: String,
    val targetQuantity: Int,
    val approvedQuantity: Int,
    val totalDealAmount: Double,
    val pricePerPiece: Double?,
    val pricingType: OrderPricingType,
    val rawMaterials: List<OrderMaterial>,
    val payments: List<KaarigerOrderPayment>,
    val totalPaid: Double,
    val remainingBalance: Double,
    val orderCreatedAt: Long,
    val verifiedAt: Long?,
    val verifiedBy: String?,
    val receiptGeneratedAt: Long = System.currentTimeMillis()
)

fun buildOrderReceiptData(
    order: KaarigerOrder,
    payments: List<KaarigerOrderPayment>
): OrderReceiptData {
    val orderPayments = payments.filter { it.orderId == order.id }
    val totalPaid = orderPayments.sumOf { it.amount }
    return OrderReceiptData(
        orderId = order.id.take(8).uppercase(),
        kaarigerName = order.kaarigerName,
        productName = order.productName,
        color = order.color,
        targetQuantity = order.targetQuantity,
        approvedQuantity = order.approvedQuantity,
        totalDealAmount = order.totalDealAmount,
        pricePerPiece = order.pricePerPiece,
        pricingType = order.pricingType,
        rawMaterials = order.rawMaterials,
        payments = orderPayments,
        totalPaid = totalPaid,
        remainingBalance = (order.totalDealAmount - totalPaid).coerceAtLeast(0.0),
        orderCreatedAt = order.createdAt,
        verifiedAt = order.verifiedAt,
        verifiedBy = order.verifiedBy
    )
}
