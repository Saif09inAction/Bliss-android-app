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
    val kharchaGiven: Double = 0.0,
    /** Portion of week kharcha already folded into running balance. */
    val kharchaCarriedForward: Double = 0.0,
    /** e.g. "October 1st week" — from admin bill create. */
    val weekLabel: String = "",
    val weekKey: String = ""
) {
    fun remainingQuantity(): Int = (targetQuantity - approvedQuantity).coerceAtLeast(0)

    fun effectiveDealAmount(): Double {
        val original = originalDealAmount ?: totalDealAmount
        return (original - repairDeductionTotal).coerceAtLeast(0.0)
    }

    /** Prefer stored weekLabel; otherwise derive Saturday-week label from createdAt. */
    fun displayWeekLabel(): String {
        if (weekLabel.isNotBlank()) return weekLabel
        return weekLabelFromMillis(createdAt)
    }
}

/** Saturday-start week-of-month, e.g. "October 1st week". */
fun weekLabelFromMillis(ms: Long): String {
    if (ms <= 0L) return "Week bill"
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
    val monthName = cal.getDisplayName(
        java.util.Calendar.MONTH,
        java.util.Calendar.LONG,
        java.util.Locale.ENGLISH
    ) ?: "Month"
    val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val dow = cal.get(java.util.Calendar.DAY_OF_WEEK) // Sun=1 … Sat=7
    val daysSinceSaturday = (dow % 7) // Sat→0, Sun→1, … Fri→6
    val saturdayDate = day - daysSinceSaturday
    val weekNum = ((saturdayDate.coerceAtLeast(1) - 1) / 7) + 1
    val ordinal = when {
        weekNum % 100 in 11..13 -> "${weekNum}th"
        weekNum % 10 == 1 -> "${weekNum}st"
        weekNum % 10 == 2 -> "${weekNum}nd"
        weekNum % 10 == 3 -> "${weekNum}rd"
        else -> "${weekNum}th"
    }
    return "$monthName $ordinal week"
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
    val createdBy: String,
    /** Epoch ms when written — preferred for newest-first lists. */
    val createdAt: Long = 0L
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
