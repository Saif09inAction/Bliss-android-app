package com.laiza.worker.domain.hisaab

import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.OrderRepair
import com.laiza.worker.domain.models.OrderStatus

/**
 * ADD to running balance. When [KaarigerOrder.addBalance] is stored at bill create it already
 * includes material + repair deductions — do not subtract repairs again (matches admin web).
 */
fun orderAddBalance(order: KaarigerOrder, repairs: List<OrderRepair>? = emptyList()): Double {
    order.addBalance?.let { return it }

    val opening = order.openingAtCreation
    val closing = order.closingAtCreation
    if (opening != null && closing != null) {
        return closing - opening + order.kharchaGiven.coerceAtLeast(0.0)
    }

    val products = if (order.productsTotal > 0) {
        order.productsTotal
    } else {
        order.originalDealAmount ?: order.totalDealAmount
    }
    val deductions = order.materialDeductionsTotal.coerceAtLeast(0.0)
    val repairTotal = repairTotalForOrder(order, repairs)
    return products - deductions - repairTotal
}

/** Prefer stored closing snapshot; otherwise opening + ADD − week kharcha. */
fun orderClosingBalance(order: KaarigerOrder, repairs: List<OrderRepair>? = emptyList()): Double {
    order.closingAtCreation?.let { return it.coerceAtLeast(0.0) }
    val opening = order.openingAtCreation?.coerceAtLeast(0.0) ?: 0.0
    val budget = order.kharchaGiven.coerceAtLeast(0.0)
    return (opening + orderAddBalance(order, repairs) - budget).coerceAtLeast(0.0)
}

fun repairTotalForOrder(order: KaarigerOrder, repairs: List<OrderRepair>?): Double {
    val orderRepairs = (repairs ?: emptyList()).filter {
        if (order.status == OrderStatus.COMPLETED) {
            it.orderId == order.id && it.isApproved
        } else {
            (it.isStandalone || it.orderId == order.id) && it.isApproved
        }
    }
    return if (orderRepairs.isNotEmpty()) {
        orderRepairs.sumOf { it.totalRepairCost }
    } else {
        order.repairDeductionTotal.coerceAtLeast(0.0)
    }
}
