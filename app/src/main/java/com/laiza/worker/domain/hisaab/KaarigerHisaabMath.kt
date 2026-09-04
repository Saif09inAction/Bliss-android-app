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
    val materialOnly = if (order.materialDeductions.isNotEmpty()) {
        order.materialDeductions.sumOf { it.lineTotal }
    } else {
        order.materialDeductionsTotal.coerceAtLeast(0.0)
    }
    val repairTotal = repairDeductionForOrder(order, repairs)
    return products - materialOnly - repairTotal
}

/** Prefer stored closing snapshot; otherwise opening + ADD − week kharcha. Can be negative. */
fun orderClosingBalance(order: KaarigerOrder, repairs: List<OrderRepair>? = emptyList()): Double {
    order.closingAtCreation?.let { return it }
    val opening = order.openingAtCreation ?: 0.0
    val budget = order.kharchaGiven.coerceAtLeast(0.0)
    return opening + orderAddBalance(order, repairs) - budget
}

/** Repair amount on this bill — prefer stored total from bill create. */
fun repairDeductionForOrder(order: KaarigerOrder, repairs: List<OrderRepair>?): Double {
    if (order.repairDeductionTotal > 0) return order.repairDeductionTotal
    return repairTotalFromList(order, repairs)
}

fun repairTotalForOrder(order: KaarigerOrder, repairs: List<OrderRepair>?): Double {
    return repairDeductionForOrder(order, repairs)
}

private fun repairTotalFromList(order: KaarigerOrder, repairs: List<OrderRepair>?): Double {
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
