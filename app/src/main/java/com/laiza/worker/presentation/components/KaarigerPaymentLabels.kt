package com.laiza.worker.presentation.components

import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.KaarigerOrderPayment

const val OPENING_ORDER_ID = "__opening__"

fun isCreditLedgerPayment(p: KaarigerOrderPayment): Boolean {
    val r = p.remarks ?: return false
    return r == "Extra kharcha — carried as credit" ||
        r.contains("carried as credit", ignoreCase = true)
}

/** Payment applied to opening / old remaining (not credit leftover). */
fun isOpeningBalancePayment(p: KaarigerOrderPayment): Boolean {
    if (isCreditLedgerPayment(p)) return false
    val r = p.remarks ?: ""
    return p.orderId == OPENING_ORDER_ID ||
        r == "Opening / old remaining payment" ||
        r == "Old remaining payment" ||
        r == "Opening balance payment" ||
        r.contains("old remaining", ignoreCase = true) ||
        r.contains("opening balance", ignoreCase = true)
}

fun isOpeningLikePayment(p: KaarigerOrderPayment): Boolean {
    return p.orderId == OPENING_ORDER_ID ||
        isOpeningBalancePayment(p) ||
        isCreditLedgerPayment(p)
}

data class LabeledKaarigerPayment(
    val payment: KaarigerOrderPayment,
    val label: String
)

/**
 * Every payment for this kaariger, newest first, with a human label.
 * Never drop rows — orphan orderIds still show as "Kharcha".
 */
fun labeledKaarigerPayments(
    payments: List<KaarigerOrderPayment>,
    orders: List<KaarigerOrder>,
    openingLabel: String,
    creditLabel: String,
    orphanLabel: String = "Kharcha"
): List<LabeledKaarigerPayment> {
    val orderName = orders.associate { it.id to it.productName.ifBlank { orphanLabel } }
    return payments
        .sortedWith(
            compareByDescending<KaarigerOrderPayment> { it.date }
                .thenByDescending { com.laiza.worker.core.utils.DateFormatter.timeSortKey(it.time) }
        )
        .map { payment ->
            val label = when {
                isCreditLedgerPayment(payment) -> creditLabel
                isOpeningBalancePayment(payment) || payment.orderId == OPENING_ORDER_ID -> openingLabel
                else -> orderName[payment.orderId] ?: orphanLabel
            }
            LabeledKaarigerPayment(payment, label)
        }
}
