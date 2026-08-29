package com.laiza.worker.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laiza.worker.R
import com.laiza.worker.core.utils.DateFormatter
import com.laiza.worker.core.utils.formatIndianRupee
import com.laiza.worker.domain.hisaab.orderAddBalance
import com.laiza.worker.domain.hisaab.repairTotalForOrder
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderStatus

import com.laiza.worker.domain.models.OrderRepair

fun formatOrderDate(millis: Long?): String {
    if (millis == null || millis <= 0) return "—"
    return DateFormatter.formatEpochToDisplayDateTime(millis)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaarigerOrderDetailSheet(
    order: KaarigerOrder,
    payments: List<KaarigerOrderPayment> = emptyList(),
    repairs: List<OrderRepair>? = emptyList(),
    onDismiss: () -> Unit,
    onReportMaterials: (() -> Unit)? = null,
    onViewReceipt: (() -> Unit)? = null
) {
    val emDash = stringResource(R.string.kaariger_em_dash)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.productName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        order.status.kaarigerDisplayName(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            DetailRow(stringResource(R.string.kaariger_detail_received_on), formatOrderDate(order.createdAt).ifBlank { emDash })
            if (order.color.isNotBlank()) DetailRow(stringResource(R.string.kaariger_detail_color), order.color)
            DetailRow(
                stringResource(R.string.kaariger_detail_deal),
                if (order.pricingType.name == "PER_PIECE")
                    stringResource(
                        R.string.kaariger_deal_per_piece,
                        rupees(order.totalDealAmount),
                        rupees(order.pricePerPiece ?: 0.0)
                    )
                else
                    stringResource(R.string.kaariger_deal_total, rupees(order.totalDealAmount))
            )
            order.notes?.takeIf { it.isNotBlank() }?.let { DetailRow(stringResource(R.string.kaariger_detail_notes), it) }

            if (order.rawMaterials.isNotEmpty()) {
                Text(stringResource(R.string.kaariger_detail_raw_materials), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                order.rawMaterials.forEach { mat ->
                    val usage = if (mat.usedQuantity != null) {
                        stringResource(
                            R.string.kaariger_detail_material_usage,
                            "${mat.usedQuantity!!.toInt()} ${mat.unit}",
                            "${mat.remainingQuantity?.toInt() ?: 0} ${mat.unit}"
                        )
                    } else ""
                    Text(
                        stringResource(R.string.kaariger_detail_material_line, mat.materialName, mat.quantity.toInt(), mat.unit) + usage,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (order.products.isNotEmpty()) {
                val orderPayments = payments.filter { it.orderId == order.id }
                if (order.status == OrderStatus.COMPLETED) {
                    KaarigerPreviousHisaabPanel(
                        order = order,
                        payments = orderPayments,
                        repairs = repairs,
                        showHeader = true
                    )
                } else {
                    OrderHisaabBreakdown(order, orderPayments, repairs)
                    GrandTotalBox(order, orderPayments, repairs)
                }
            }

            if (order.status == OrderStatus.COMPLETED && order.rawMaterials.isNotEmpty() && !order.materialUsageReported && onReportMaterials != null) {
                OutlinedButton(onClick = onReportMaterials, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.kaariger_report_materials))
                }
            }
            if (order.status == OrderStatus.COMPLETED && order.materialUsageReported && onViewReceipt != null) {
                Button(onClick = onViewReceipt, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.receipt_view))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun rupees(amount: Double): String = formatIndianRupee(amount)

@Composable
private fun OrderHisaabBreakdown(
    order: KaarigerOrder,
    orderPayments: List<KaarigerOrderPayment>,
    repairs: List<OrderRepair>?
) {
    val sortedPayments = remember(orderPayments) {
        orderPayments
            .filter { !isOpeningOrCreditPayment(it) }
            .sortedBy { "${it.date} ${it.time}" }
    }
    val totalKharcha = sortedPayments.sumOf { it.amount }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.kaariger_detail_products),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            order.products.forEach { p ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${p.productName} · ${p.quantity} × ${rupees(p.pricePerPiece)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(rupees(p.lineTotal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
            DetailRow(stringResource(R.string.kaariger_detail_products_total), rupees(order.productsTotal))

            val orderRepairs = remember(repairs, order.id, order.status) {
                val list = repairs ?: emptyList()
                if (order.status == OrderStatus.COMPLETED) {
                    list.filter { it.orderId == order.id && it.isApproved }
                } else {
                    list.filter { (it.isStandalone || it.orderId == order.id) && it.isApproved }
                }
            }
            val hasDeductions = order.materialDeductions.isNotEmpty() || orderRepairs.isNotEmpty() || order.repairDeductionTotal > 0
            if (hasDeductions) {
                Divider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    stringResource(R.string.kaariger_detail_deductions),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                order.materialDeductions.forEach { it2 ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${it2.label} · ${it2.quantity} × ${rupees(it2.pricePerPiece)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text("−${rupees(it2.lineTotal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                    }
                }
                orderRepairs.forEach { r ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Repairing - ${r.productName} · ${r.faultyQuantity} × ${rupees(r.faultyPricePerPiece)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text("−${rupees(r.totalRepairCost)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                    }
                }
                if (orderRepairs.isEmpty() && order.repairDeductionTotal > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Repairing",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text("−${rupees(order.repairDeductionTotal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                    }
                }
                val repairTotal = if (orderRepairs.isNotEmpty()) orderRepairs.sumOf { it.totalRepairCost } else order.repairDeductionTotal.coerceAtLeast(0.0)
                val totalDeductions = order.materialDeductionsTotal.coerceAtLeast(0.0) + repairTotal
                DetailRow(stringResource(R.string.kaariger_detail_deductions_total), "−${rupees(totalDeductions)}")
            }

            if (sortedPayments.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    stringResource(R.string.kaariger_detail_kharcha_timeline),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                sortedPayments.forEach { p ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                DateFormatter.formatStoredDateTime(p.date, p.time),
                                style = MaterialTheme.typography.bodySmall
                            )
                            p.remarks?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("−${rupees(p.amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                    }
                }
                DetailRow(stringResource(R.string.kaariger_detail_kharcha_total), "−${rupees(totalKharcha)}")
            }
        }
    }
}

/**
 * Same money model as admin bill:
 * Remaining = opening + ADD − week kharcha (Pay does not add into Remaining).
 * Kharcha box = week budget − carry − week Pays.
 */
@Composable
private fun GrandTotalBox(
    order: KaarigerOrder,
    orderPayments: List<KaarigerOrderPayment>,
    repairs: List<OrderRepair>?
) {
    val weekPays = orderPayments.filter { !isOpeningOrCreditPayment(it) }
    val paidCash = weekPays.sumOf { it.amount.coerceAtLeast(0.0) }
    val priorOverpay = order.kharchaCarryIn.coerceAtLeast(0.0)
    val paidDisplay = paidCash + priorOverpay

    val orderRepairs = remember(repairs, order.id, order.status) {
        val list = repairs ?: emptyList()
        if (order.status == OrderStatus.COMPLETED) {
            list.filter { it.orderId == order.id && it.isApproved }
        } else {
            list.filter { (it.isStandalone || it.orderId == order.id) && it.isApproved }
        }
    }
    val repairTotal = repairTotalForOrder(order, repairs)
    val net = orderAddBalance(order, repairs)

    val budget = order.kharchaGiven.coerceAtLeast(0.0)
    val opening = order.openingAtCreation?.coerceAtLeast(0.0)
    val closing = if (order.status == OrderStatus.COMPLETED) {
        order.closingAtCreation ?: ((opening ?: 0.0) + net - budget).coerceAtLeast(0.0)
    } else {
        ((opening ?: 0.0) + net - budget).coerceAtLeast(0.0)
    }
    val kharchaBox = budget - order.kharchaCarryIn - paidCash
    val jade = Color(0xFF0D8F63)
    val jadeSoft = Color(0xFFD8F8EB)
    val amber = Color(0xFFB45309)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = jadeSoft.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, jade.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.kaariger_grand_total_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = jade
            )

            if (opening != null) {
                DetailRow("Opening / running balance", rupees(opening))
            } else {
                DetailRow("Opening / running balance", rupees(0.0))
            }
            DetailRow("MAAL (product cost)", rupees(order.productsTotal))

            order.materialDeductions.forEach { d ->
                DetailRow("Less: ${d.label}", "−${rupees(d.lineTotal)}")
            }

            orderRepairs.forEach { r ->
                DetailRow("Less: Repairing - ${r.productName}", "−${rupees(r.totalRepairCost)}")
            }
            if (orderRepairs.isEmpty() && order.repairDeductionTotal > 0) {
                DetailRow("Less: Repairing", "−${rupees(order.repairDeductionTotal)}")
            }

            Divider(modifier = Modifier.padding(vertical = 2.dp))
            BoldRow("ADD BALANCE", rupees(net))
            DetailRow("After ADD", rupees((opening ?: 0.0) + net))
            if (budget > 0.0) {
                DetailRow("Kharcha on bill", "−${rupees(budget)}")
            }
            BoldRow(stringResource(R.string.kaariger_hisaab_outstanding_after), rupees(closing ?: 0.0), amber)

            Divider(modifier = Modifier.padding(vertical = 2.dp))
            Text(
                stringResource(R.string.kaariger_hisaab_kharcha_that_week),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = jade
            )
            DetailRow(stringResource(R.string.kaariger_hisaab_kharcha_budget), rupees(budget))
            DetailRow(stringResource(R.string.kaariger_hisaab_kharcha_paid_total), rupees(paidDisplay))
            if (priorOverpay > 0.0) {
                DetailRow(stringResource(R.string.kaariger_hisaab_prior_overpay), rupees(priorOverpay))
            }
            BoldRow(
                if (kharchaBox < 0) {
                    stringResource(R.string.kaariger_hisaab_kharcha_extra)
                } else {
                    stringResource(R.string.kaariger_hisaab_order_baaki)
                },
                rupees(kharchaBox),
                if (kharchaBox < 0) amber else jade
            )
        }
    }
}

private fun isOpeningOrCreditPayment(p: KaarigerOrderPayment): Boolean {
    val remarks = (p.remarks ?: "").lowercase()
    if (remarks.contains("carried as credit") || remarks.contains("credit carried")) return true
    return p.orderId == "__opening__" ||
        remarks.contains("opening balance") ||
        remarks.contains("old remaining")
}

@Composable
private fun BoldRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
    }
}
