package com.laiza.worker.domain.hisaab

import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.OrderRepair

/**
 * ADD for a bill — matches admin Grand Total:
 * products − materials − repairs linked to this bill only.
 * Deferred / pending standalone repairing is never included.
 */
fun orderAddBalance(order: KaarigerOrder, repairs: List<OrderRepair>? = emptyList()): Double {
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
    val live = products - materialOnly - repairTotal

    // Prefer live math when we have the repairs list — stored addBalance / repairDeductionTotal
    // can still include deferred repairing from older syncs.
    if (repairs != null) return live

    return order.addBalance ?: live
}

/**
 * Live outstanding: opening + ADD − week kharcha (can be negative).
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

/**
 * Approved repairing not on a bill yet — shown as pending, not deducted
 * (same as admin “Pending repairing (not deducted)”).
 */
fun pendingBillRepairs(repairs: List<OrderRepair>?): List<OrderRepair> {
    return (repairs ?: emptyList()).filter { it.isStandalone && it.isApproved }
}

/**
 * Repair amount on this bill.
 * When [repairs] is provided: only linked repairs (never stale repairDeductionTotal).
 */
fun repairDeductionForOrder(order: KaarigerOrder, repairs: List<OrderRepair>?): Double {
    if (repairs != null) {
        return approvedRepairsOnBill(order, repairs).sumOf { it.totalRepairCost }
    }
    return order.repairDeductionTotal.coerceAtLeast(0.0)
}

fun repairTotalForOrder(order: KaarigerOrder, repairs: List<OrderRepair>?): Double {
    return repairDeductionForOrder(order, repairs)
}
