package com.laiza.worker.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laiza.worker.R
import com.laiza.worker.core.utils.DateFormatter
import com.laiza.worker.core.utils.formatIndianRupee
import com.laiza.worker.domain.hisaab.approvedRepairsOnBill
import com.laiza.worker.domain.hisaab.orderAddBalance
import com.laiza.worker.domain.hisaab.orderClosingBalance
import com.laiza.worker.domain.hisaab.orderGrossClosingBalance
import com.laiza.worker.domain.hisaab.pendingBillRepairs
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
    creditBalance: Double = 0.0,
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
                    SingleHisaabCalculationBox(order, orderPayments, repairs, creditBalance)
                    val pendingRepairs = remember(repairs) { pendingBillRepairs(repairs) }
                    CollapsiblePaymentsSection(orderPayments, pendingRepairs)
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
private fun SingleHisaabCalculationBox(
    order: KaarigerOrder,
    orderPayments: List<KaarigerOrderPayment>,
    repairs: List<OrderRepair>?,
    creditBalance: Double = 0.0
) {
    val weekPays = orderPayments.filter { !isOpeningOrCreditPayment(it) }
    val paidCash = weekPays.sumOf { it.amount.coerceAtLeast(0.0) }
    val priorOverpay = order.kharchaCarryIn.coerceAtLeast(0.0)
    val paidDisplay = paidCash + priorOverpay

    val orderRepairs = remember(repairs, order.id) {
        approvedRepairsOnBill(order, repairs)
    }

    val budget = order.kharchaGiven.coerceAtLeast(0.0)
    val opening = order.openingAtCreation ?: 0.0
    val productsTotal = order.productsTotal
    val subtotal = opening + productsTotal

    val grossClosing = orderGrossClosingBalance(order, repairs)
    val bakedCredit = order.creditApplied?.coerceAtLeast(0.0)
    val creditWasSettled = bakedCredit != null
    val liveCreditFallback =
        if (!creditWasSettled) {
            minOf(creditBalance.coerceAtLeast(0.0), grossClosing.coerceAtLeast(0.0))
        } else {
            0.0
        }
    val creditShown = if (creditWasSettled) bakedCredit!! else liveCreditFallback
    val totalRemaining =
        if (creditWasSettled) {
            order.closingAtCreation ?: (grossClosing - bakedCredit!!)
        } else {
            grossClosing - liveCreditFallback
        }
    val kharchaBox = budget - order.kharchaCarryIn - paidCash

    val jade = Color(0xFF0D8F63)
    val jadeSoft = Color(0xFFD8F8EB)
    val amber = Color(0xFFB45309)
    val amberSoft = Color(0xFFFEF3C7)
    val red = Color(0xFFDC2626)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, jade.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.kaariger_grand_total_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = jade
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = jadeSoft
                ) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_this_week_title),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = jade
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = jade.copy(alpha = 0.2f))

            // 1. Pichhla Baaki (Opening balance)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.kaariger_hisaab_pichhla_baaki),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    rupees(opening),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amber
                )
            }

            // 2. + MAAL (Kaam)
            if (order.products.isNotEmpty()) {
                order.products.forEach { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "+ MAAL: ${p.productName} (${p.quantity} × ${rupees(p.pricePerPiece)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            "+${rupees(p.lineTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = jade
                        )
                    }
                }
                if (order.products.size > 1) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.kaariger_hisaab_maal_kaam),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "+${rupees(productsTotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = jade
                        )
                    }
                }
            } else if (productsTotal > 0.0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_maal_kaam),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "+${rupees(productsTotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = jade
                    )
                }
            }

            // 3. Subtotal: Kul Rakam (Pichhla + Maal)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = jadeSoft.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, jade.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_subtotal_rakam),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = jade
                    )
                    Text(
                        rupees(subtotal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = jade
                    )
                }
            }

            // 4. Katauti (Deductions)
            val hasDeductions = order.materialDeductions.isNotEmpty() || orderRepairs.isNotEmpty() || budget > 0 || creditShown > 0
            if (hasDeductions) {
                Text(
                    stringResource(R.string.kaariger_hisaab_katauti_header),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = red,
                    modifier = Modifier.padding(top = 2.dp)
                )

                order.materialDeductions.forEach { d ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "− ${d.label} (${d.quantity} × ${rupees(d.pricePerPiece)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            "−${rupees(d.lineTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = red
                        )
                    }
                }

                orderRepairs.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "− Repairing: ${r.productName} (${r.faultyQuantity} × ${rupees(r.faultyPricePerPiece)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            "−${rupees(r.totalRepairCost)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = red
                        )
                    }
                }

                if (budget > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.kaariger_hisaab_hafte_kharcha),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "−${rupees(budget)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = red
                        )
                    }
                }

                if (creditShown > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.kaariger_hisaab_credit_settled_short),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "−${rupees(creditShown)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = red
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = amber.copy(alpha = 0.3f))

            // 5. Total Remaining (Kul Baaki Hisaab) - Prominently highlighted
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = amberSoft,
                border = BorderStroke(1.5.dp, amber.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.kaariger_hisaab_kul_baaki_final),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = amber
                        )
                        Text(
                            stringResource(R.string.kaariger_hisaab_outstanding_after),
                            style = MaterialTheme.typography.labelSmall,
                            color = amber.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        rupees(totalRemaining),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = amber
                    )
                }
            }

            // 6. Kharcha That Week (Budget vs Paid vs Extra/Left)
            if (budget > 0.0 || paidDisplay > 0.0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    stringResource(R.string.kaariger_hisaab_kharcha_that_week),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = jade
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.kaariger_hisaab_kharcha_budget), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(rupees(budget), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.kaariger_detail_kharcha_timeline), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(rupees(paidDisplay), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = jade)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (kharchaBox < 0) stringResource(R.string.kaariger_hisaab_kharcha_extra)
                                else stringResource(R.string.kaariger_hisaab_kharcha_left),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                rupees(kotlin.math.abs(kharchaBox)),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (kharchaBox < 0) amber else jade
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsiblePaymentsSection(
    orderPayments: List<KaarigerOrderPayment>,
    pendingRepairs: List<OrderRepair> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }
    val sortedPayments = remember(orderPayments) {
        orderPayments
            .filter { !isOpeningOrCreditPayment(it) }
            .sortedByDescending { "${it.date} ${it.time}" }
    }
    val totalPaid = sortedPayments.sumOf { it.amount }

    if (sortedPayments.isEmpty() && pendingRepairs.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sortedPayments.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                stringResource(R.string.kaariger_hisaab_paise_mile_section),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(
                                    R.string.kaariger_hisaab_payments_count_total,
                                    sortedPayments.size,
                                    rupees(totalPaid)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (expanded) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sortedPayments.forEach { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            DateFormatter.formatStoredDateTime(p.date, p.time),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        p.remarks?.takeIf { it.isNotBlank() }?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        "−${rupees(p.amount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (pendingRepairs.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Pending repairing (Agle bill mein katega)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB45309)
                    )
                    pendingRepairs.forEach { r ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "${r.productName} · ${r.faultyQuantity} × ${rupees(r.faultyPricePerPiece)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                rupees(r.totalRepairCost),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }
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
