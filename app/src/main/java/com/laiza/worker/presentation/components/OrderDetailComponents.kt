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
import com.laiza.worker.core.utils.formatIndianRupee
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
                    stringResource(R.string.kaariger_deal_per_piece, order.totalDealAmount.toInt(), order.pricePerPiece?.toInt() ?: 0)
                else
                    stringResource(R.string.kaariger_deal_total, order.totalDealAmount.toInt())
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
                OrderHisaabBreakdown(order, orderPayments)
                GrandTotalBox(order, orderPayments)
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
        }
    }
}

/**
 * Mirrors the admin panel's Grand Total box: product cost total minus
 * deductions/repairs gives the net Total, then compares it against what's
 * actually been paid to show Extra paid / Total remaining / Fully cleared.
 */
@Composable
private fun GrandTotalBox(order: KaarigerOrder, orderPayments: List<KaarigerOrderPayment>) {
    val paid = orderPayments.sumOf { it.amount }
    val net = (order.productsTotal - order.materialDeductionsTotal - order.repairDeductionTotal).coerceAtLeast(0.0)
    val diff = paid - net
    val jade = Color(0xFF0D8F63)
    val jadeSoft = Color(0xFFD8F8EB)

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
            DetailRow(stringResource(R.string.kaariger_detail_products_total), rupees(order.productsTotal))
            if (order.materialDeductionsTotal > 0) {
                DetailRow(stringResource(R.string.kaariger_detail_deductions), "−${rupees(order.materialDeductionsTotal)}")
            }
            if (order.repairDeductionTotal > 0) {
                DetailRow(stringResource(R.string.kaariger_detail_repair_deduction), "−${rupees(order.repairDeductionTotal)}")
            }
            Divider(modifier = Modifier.padding(vertical = 2.dp))
            BoldRow(stringResource(R.string.kaariger_grand_total_net), rupees(net))
            DetailRow(stringResource(R.string.kaariger_grand_total_paid), rupees(paid))
            Divider(modifier = Modifier.padding(vertical = 2.dp))
            when {
                diff > 0.5 -> BoldRow(stringResource(R.string.kaariger_grand_total_extra), "+${rupees(diff)}", Color(0xFF047857))
                diff < -0.5 -> BoldRow(stringResource(R.string.kaariger_grand_total_remaining), rupees(-diff), Color(0xFFB45309))
                else -> BoldRow(stringResource(R.string.kaariger_grand_total_cleared), "₹0", Color(0xFF047857))
            }
        }
    }
}

@Composable
private fun BoldRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
    }
}
