package com.laiza.worker.presentation.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.laiza.worker.R
import com.laiza.worker.core.theme.BlissCream
import com.laiza.worker.core.theme.BlissGold
import com.laiza.worker.core.theme.BlissGreen
import com.laiza.worker.core.theme.BlissGreenDark
import com.laiza.worker.core.utils.OrderReceiptImageHelper
import com.laiza.worker.core.utils.DateFormatter
import com.laiza.worker.core.utils.formatIndianRupee
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderPricingType
import com.laiza.worker.domain.models.OrderReceiptData

private fun formatPaymentDateTime(date: String, time: String): String {
    val clock = DateFormatter.formatStoredTime(time)
    return when {
        date.isBlank() -> clock
        clock.isBlank() -> date
        else -> "$date, $clock"
    }
}

private fun formatRupee(amount: Double): String = formatIndianRupee(amount)

@Composable
fun OrderReceiptCard(
    data: OrderReceiptData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, BlissGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BlissGreenDark, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.receipt_brand),
                    color = BlissGold,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.receipt_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = stringResource(R.string.receipt_order_ref, data.orderId),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ReceiptSection(stringResource(R.string.receipt_section_order)) {
                ReceiptRow(stringResource(R.string.receipt_kaariger), data.kaarigerName)
                ReceiptRow(stringResource(R.string.receipt_product), data.productName)
                if (data.color.isNotBlank()) {
                    ReceiptRow(stringResource(R.string.receipt_color), data.color)
                }
                ReceiptRow(
                    stringResource(R.string.receipt_quantity),
                    stringResource(
                        R.string.receipt_qty_detail,
                        data.approvedQuantity,
                        data.targetQuantity
                    )
                )
                ReceiptRow(
                    stringResource(R.string.receipt_order_date),
                    formatOrderDate(data.orderCreatedAt)
                )
                data.verifiedAt?.let {
                    ReceiptRow(stringResource(R.string.receipt_verified_on), formatOrderDate(it))
                }
                data.verifiedBy?.let {
                    ReceiptRow(stringResource(R.string.receipt_verified_by), it)
                }
            }

            ReceiptSection(stringResource(R.string.receipt_section_payment)) {
                val dealLabel = if (data.pricingType == OrderPricingType.PER_PIECE) {
                    stringResource(
                        R.string.receipt_deal_per_piece,
                        formatRupee(data.totalDealAmount),
                        formatRupee(data.pricePerPiece ?: 0.0)
                    )
                } else {
                    stringResource(R.string.receipt_deal_total, formatRupee(data.totalDealAmount))
                }
                ReceiptRow(stringResource(R.string.receipt_total_deal), dealLabel)
                ReceiptRow(stringResource(R.string.receipt_advance_paid), formatRupee(data.totalPaid))
                ReceiptRow(
                    stringResource(R.string.receipt_balance_due),
                    formatRupee(data.remainingBalance),
                    highlight = data.remainingBalance > 0
                )
                if (data.payments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.receipt_payment_history),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    data.payments.forEach { payment ->
                        PaymentReceiptLine(payment)
                    }
                } else {
                    Text(
                        stringResource(R.string.receipt_no_payments),
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            if (data.rawMaterials.isNotEmpty()) {
                ReceiptSection(stringResource(R.string.receipt_section_materials)) {
                    data.rawMaterials.forEach { mat ->
                        val used = mat.usedQuantity?.let { "${it.toInt()} ${mat.unit}" } ?: "—"
                        val remaining = mat.remainingQuantity?.let { "${it.toInt()} ${mat.unit}" } ?: "—"
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(mat.materialName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                stringResource(
                                    R.string.receipt_material_line,
                                    mat.quantity.toInt(),
                                    mat.unit,
                                    used,
                                    remaining
                                ),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                    }
                }
            }

            Text(
                text = stringResource(
                    R.string.receipt_generated_on,
                    formatOrderDate(data.receiptGeneratedAt)
                ),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReceiptSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlissGreen)
        HorizontalDivider(color = BlissGreen.copy(alpha = 0.15f))
        content()
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) Color(0xFFB45309) else Color(0xFF0F172A),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PaymentReceiptLine(payment: KaarigerOrderPayment) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatRupee(payment.amount), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                formatPaymentDateTime(payment.date, payment.time),
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
        payment.remarks?.takeIf { it.isNotBlank() }?.let {
            Text(it, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun OrderReceiptDialog(
    data: OrderReceiptData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = BlissCream
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.receipt_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.receipt_close))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    OrderReceiptCard(data = data, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (activity == null) return@OutlinedButton
                            val bitmap = OrderReceiptImageHelper.captureComposable(activity) {
                                MaterialTheme {
                                    OrderReceiptCard(data = data, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            OrderReceiptImageHelper.shareImage(context, bitmap, data.orderId)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.receipt_share))
                    }
                    Button(
                        onClick = {
                            if (activity == null) return@Button
                            val bitmap = OrderReceiptImageHelper.captureComposable(activity) {
                                MaterialTheme {
                                    OrderReceiptCard(data = data, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            val saved = OrderReceiptImageHelper.saveToGallery(context, bitmap, data.orderId)
                            val message = if (saved != null) {
                                context.getString(R.string.receipt_saved)
                            } else {
                                context.getString(R.string.receipt_save_failed)
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BlissGreen)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.receipt_download))
                    }
                }
            }
        }
    }
}
