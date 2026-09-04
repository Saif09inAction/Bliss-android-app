package com.laiza.worker.domain.hisaab

import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.OrderRepair

/**
 * ADD to running balance. When [KaarigerOrder.addBalance] is stored at bill create it already
 * includes material + bill-linked repair deductions — do not subtract deferred repairs again
 * (matches admin web: repairing only counts after admin adds it to the bill).
 */
fun orderAddBalance(order: KaarigerOrder, repairs: List<OrderRepair>? = emptyList()): Double {
    order.addBalance?.let { return it }

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

/**
 * Live outstanding: opening + ADD − week kharcha.
 * Prefer live math over stored closing (old builds floored negatives to 0).
 */
fun orderClosingBalance(order: KaarigerOrder, repairs: List<OrderRepair>? = emptyList()): Double {
    val opening = order.openingAtCreation ?: 0.0
    val budget = order.kharchaGiven.coerceAtLeast(0.0)
    return opening + orderAddBalance(order, repairs) - budget
}

/** Approved repairs already linked to this bill (deducted). */
fun approvedRepairsOnBill(order: KaarigerOrder, repairs: List<OrderRepair>?): List<OrderRepair> {
    return (repairs ?: emptyList()).filter {
        it.orderId == order.id && it.isApproved
    }
}

/** Approved repairing waiting for admin to add to a bill (not deducted yet). */
fun pendingBillRepairs(repairs: List<OrderRepair>?): List<OrderRepair> {
    return (repairs ?: emptyList()).filter {
        it.isStandalone && it.isApproved && it.deferToNextBill
    }
}

/** Repair amount on this bill — only linked repairs, never deferred standalone. */
fun repairDeductionForOrder(order: KaarigerOrder, repairs: List<OrderRepair>?): Double {
    val linked = approvedRepairsOnBill(order, repairs)
    if (linked.isNotEmpty()) return linked.sumOf { it.totalRepairCost }
    return order.repairDeductionTotal.coerceAtLeast(0.0)
}

fun repairTotalForOrder(order: KaarigerOrder, repairs: List<OrderRepair>?): Double {
    return repairDeductionForOrder(order, repairs)
}
