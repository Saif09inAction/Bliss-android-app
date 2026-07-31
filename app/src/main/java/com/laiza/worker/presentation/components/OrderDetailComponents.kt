package com.laiza.worker.presentation.components

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
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatOrderDate(millis: Long?): String {
    if (millis == null || millis <= 0) return "—"
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaarigerOrderDetailSheet(
    order: KaarigerOrder,
    payments: List<KaarigerOrderPayment> = emptyList(),
    onDismiss: () -> Unit,
    onReportMaterials: (() -> Unit)? = null,
    onViewReceipt: (() -> Unit)? = null
) {
    val remaining = order.remainingQuantity()
    val awaiting = if (order.status == OrderStatus.PENDING_APPROVAL) order.deliveredQuantity ?: 0 else 0
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
            DetailRow(stringResource(R.string.kaariger_detail_target), stringResource(R.string.kaariger_pcs, order.targetQuantity))
            DetailRow(stringResource(R.string.kaariger_detail_sent_approved), stringResource(R.string.kaariger_pcs, order.approvedQuantity))
            if (awaiting > 0) DetailRow(stringResource(R.string.kaariger_detail_awaiting), stringResource(R.string.kaariger_pcs, awaiting))
            DetailRow(stringResource(R.string.kaariger_detail_remaining), stringResource(R.string.kaariger_pcs, remaining))
            DetailRow(
                stringResource(R.string.kaariger_detail_deal),
                if (order.pricingType.name == "PER_PIECE")
                    stringResource(R.string.kaariger_deal_per_piece, order.totalDealAmount.toInt(), order.pricePerPiece?.toInt() ?: 0)
                else
                    stringResource(R.string.kaariger_deal_total, order.totalDealAmount.toInt())
            )
            order.notes?.takeIf { it.isNotBlank() }?.let { DetailRow(stringResource(R.string.kaariger_detail_notes), it) }
            order.verifiedBy?.let { DetailRow(stringResource(R.string.kaariger_detail_last_verified_by), it) }
            order.verifiedAt?.let { DetailRow(stringResource(R.string.kaariger_detail_last_verified_on), formatOrderDate(it)) }

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
                OrderHisaabBreakdown(order, payments.filter { it.orderId == order.id })
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

private fun rupees(amount: Double): String = "₹${amount.toInt()}"

@Composable
private fun OrderHisaabBreakdown(order: KaarigerOrder, orderPayments: List<KaarigerOrderPayment>) {
    val sortedPayments = remember(orderPayments) {
        orderPayments.sortedBy { "${it.date} ${it.time}" }
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

            if (order.materialDeductions.isNotEmpty()) {
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
                DetailRow(stringResource(R.string.kaariger_detail_deductions_total), "−${rupees(order.materialDeductionsTotal)}")
            }

            if (order.repairDeductionTotal > 0) {
                Divider(modifier = Modifier.padding(vertical = 2.dp))
                DetailRow(stringResource(R.string.kaariger_detail_repair_deduction), "−${rupees(order.repairDeductionTotal)}")
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
                            Text("${p.date} · ${p.time}", style = MaterialTheme.typography.bodySmall)
                            p.remarks?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("−${rupees(p.amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                    }
                }
                DetailRow(stringResource(R.string.kaariger_detail_kharcha_total), "−${rupees(totalKharcha)}")
            }

            Divider(modifier = Modifier.padding(vertical = 2.dp))
            val finalBalance = (
                order.productsTotal - order.materialDeductionsTotal - order.repairDeductionTotal - totalKharcha
            ).coerceAtLeast(0.0)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.kaariger_detail_final_balance),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    rupees(finalBalance),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun OrderProgressChips(order: KaarigerOrder) {
    val remaining = order.remainingQuantity()
    val awaiting = if (order.status == OrderStatus.PENDING_APPROVAL) order.deliveredQuantity ?: 0 else 0
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ProgressChip(stringResource(R.string.kaariger_chip_approved), "${order.approvedQuantity}", Color(0xFF10B981), Modifier.weight(1f))
        if (awaiting > 0) ProgressChip(stringResource(R.string.kaariger_chip_pending), "$awaiting", Color(0xFFB45309), Modifier.weight(1f))
        ProgressChip(stringResource(R.string.kaariger_chip_left), "$remaining", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
    }
}

@Composable
private fun ProgressChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f), modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleSmall)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
