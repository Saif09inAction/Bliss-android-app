package com.laiza.worker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laiza.worker.domain.models.KaarigerOrder
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
    onDismiss: () -> Unit,
    onSubmitDelivery: (() -> Unit)? = null,
    onReportMaterials: (() -> Unit)? = null
) {
    val remaining = order.remainingQuantity()
    val awaiting = if (order.status == OrderStatus.PENDING_APPROVAL) order.deliveredQuantity ?: 0 else 0

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
                        order.status.displayName(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            DetailRow("Received on", formatOrderDate(order.createdAt))
            if (order.color.isNotBlank()) DetailRow("Color", order.color)
            DetailRow("Target quantity", "${order.targetQuantity} pcs")
            DetailRow("Sent & approved", "${order.approvedQuantity} pcs")
            if (awaiting > 0) DetailRow("Awaiting approval", "$awaiting pcs")
            DetailRow("Remaining", "$remaining pcs")
            DetailRow(
                "Deal",
                if (order.pricingType.name == "PER_PIECE")
                    "₹${order.totalDealAmount.toInt()} (₹${order.pricePerPiece?.toInt() ?: 0}/pc)"
                else
                    "₹${order.totalDealAmount.toInt()}"
            )
            order.notes?.takeIf { it.isNotBlank() }?.let { DetailRow("Notes", it) }
            order.verifiedBy?.let { DetailRow("Last verified by", it) }
            order.verifiedAt?.let { DetailRow("Last verified on", formatOrderDate(it)) }

            if (order.rawMaterials.isNotEmpty()) {
                Text("Raw Materials Received", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                order.rawMaterials.forEach { mat ->
                    val usage = if (mat.usedQuantity != null) {
                        " · Used: ${mat.usedQuantity.toInt()} ${mat.unit} · Left: ${mat.remainingQuantity?.toInt() ?: 0} ${mat.unit}"
                    } else ""
                    Text(
                        "• ${mat.materialName}: ${mat.quantity.toInt()} ${mat.unit}$usage",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if ((order.status == OrderStatus.ASSIGNED || order.status == OrderStatus.REJECTED) && remaining > 0 && onSubmitDelivery != null) {
                Button(onClick = onSubmitDelivery, modifier = Modifier.fillMaxWidth()) {
                    Text("Submit Delivery ($remaining left)")
                }
            }
            if (order.status == OrderStatus.COMPLETED && !order.materialUsageReported && onReportMaterials != null) {
                OutlinedButton(onClick = onReportMaterials, modifier = Modifier.fillMaxWidth()) {
                    Text("Report Material Usage")
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

@Composable
fun OrderProgressChips(order: KaarigerOrder) {
    val remaining = order.remainingQuantity()
    val awaiting = if (order.status == OrderStatus.PENDING_APPROVAL) order.deliveredQuantity ?: 0 else 0
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ProgressChip("Approved", "${order.approvedQuantity}", Color(0xFF10B981), Modifier.weight(1f))
        if (awaiting > 0) ProgressChip("Pending", "$awaiting", Color(0xFFB45309), Modifier.weight(1f))
        ProgressChip("Left", "$remaining", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
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
