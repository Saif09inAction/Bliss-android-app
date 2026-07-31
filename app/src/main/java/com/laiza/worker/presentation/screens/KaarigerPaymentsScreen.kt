package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.R
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderRepair
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.formatOrderDate
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

@Composable
fun KaarigerPaymentsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val session by authViewModel.userSession.collectAsState()
    val orders by orderViewModel.kaarigerOrders.collectAsState()
    val payments by orderViewModel.kaarigerPayments.collectAsState()
    val repairs by orderViewModel.kaarigerRepairs.collectAsState()
    var activeTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(session?.phone) {
        session?.phone?.let { orderViewModel.loadKaarigerData(it) }
    }

    val activeOrders = remember(orders) {
        orders.filter { it.status != OrderStatus.REJECTED }
    }

    val orderSummaries = remember(activeOrders, payments, repairs) {
        activeOrders.map { order ->
            val orderPayments = payments.filter { it.orderId == order.id }
            val orderRepairs = repairs.filter { it.orderId == order.id }
            val totalPaid = orderPayments.sumOf { it.amount }
            val originalDeal = order.originalDealAmount ?: order.totalDealAmount
            val repairTotal = order.repairDeductionTotal.takeIf { it > 0 }
                ?: orderRepairs.sumOf { it.totalRepairCost }
            val netDeal = (originalDeal - repairTotal).coerceAtLeast(0.0)
            val remaining = (netDeal - totalPaid).coerceAtLeast(0.0)
            OrderPaymentSummary(order, orderPayments, orderRepairs, originalDeal, repairTotal, netDeal, totalPaid, remaining)
        }
    }

    val tabSummaries = remember(orderSummaries, activeTab) {
        when (activeTab) {
            0 -> orderSummaries.filter { it.remaining > 0 }
            else -> orderSummaries.filter { it.remaining <= 0 }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.kaariger_payments_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(stringResource(R.string.kaariger_tab_pending)) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(stringResource(R.string.kaariger_tab_completed)) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (tabSummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    when {
                        activeOrders.isEmpty() -> stringResource(R.string.kaariger_no_payments)
                        activeTab == 0 -> stringResource(R.string.kaariger_no_pending_payments)
                        else -> stringResource(R.string.kaariger_no_completed_payments)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tabSummaries, key = { it.order.id }) { summary ->
                    OrderPaymentCard(summary)
                }
            }
        }
    }
}

private data class OrderPaymentSummary(
    val order: KaarigerOrder,
    val payments: List<KaarigerOrderPayment>,
    val repairs: List<OrderRepair>,
    val originalDeal: Double,
    val repairTotal: Double,
    val netDeal: Double,
    val totalPaid: Double,
    val remaining: Double
)

@Composable
private fun OrderPaymentCard(summary: OrderPaymentSummary) {
    val isPending = summary.remaining > 0
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(summary.order.productName, fontWeight = FontWeight.Bold)
                PaymentStatusBadge(isPending = isPending)
            }
            Spacer(modifier = Modifier.height(8.dp))
            PaymentRow("Original deal", "₹${summary.originalDeal.toInt()}")
            if (summary.repairTotal > 0) {
                PaymentRow("Repairing deduction", "−₹${summary.repairTotal.toInt()}", danger = true)
                PaymentRow("Net deal", "₹${summary.netDeal.toInt()}", highlight = true)
            }
            PaymentRow(
                stringResource(R.string.kaariger_payment_remaining),
                "₹${summary.remaining.toInt()}",
                highlight = isPending
            )

            if (summary.order.products.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Hisaab breakup",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                HisaabBreakupCard(summary.order)
            }

            if (summary.repairs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Repairing breakup",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                summary.repairs.forEach { repair ->
                    RepairBreakupCard(repair)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.kaariger_payment_advance),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            KharchaTimeline(summary.payments)
        }
    }
}

@Composable
private fun KharchaTimeline(payments: List<KaarigerOrderPayment>) {
    if (payments.isEmpty()) {
        PaymentRow(stringResource(R.string.kaariger_payment_advance), "₹0")
        return
    }
    val sorted = remember(payments) { payments.sortedBy { it.date + it.time } }
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            sorted.forEachIndexed { index, payment ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "₹${payment.amount.toInt()} paid",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${payment.date} · ${payment.time}" +
                                (payment.remarks?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            PaymentRow("Grand Total Kharcha", "₹${sorted.sumOf { it.amount }.toInt()}", highlight = true)
        }
    }
}

@Composable
private fun RepairBreakupCard(repair: OrderRepair) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF7ED)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFC2410C), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "−₹${repair.totalRepairCost.toInt()} · ${formatOrderDate(repair.createdAt)}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC2410C),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (repair.faultyQuantity > 0) {
                PaymentRow(
                    "Faulty × ${repair.faultyQuantity} @ ₹${repair.faultyPricePerPiece.toInt()}",
                    "₹${repair.faultyTotal.toInt()}"
                )
            }
            repair.items.forEach { line ->
                PaymentRow(
                    "${line.label} × ${line.quantity} @ ₹${line.pricePerPiece.toInt()}",
                    "₹${line.lineTotal.toInt()}"
                )
            }
            PaymentRow("Remaining after this", "₹${repair.dealAfterThisRepair.toInt()}", highlight = true)
            repair.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HisaabBreakupCard(order: KaarigerOrder) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF0FDF4)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            order.products.forEach { p ->
                PaymentRow("${p.productName} × ${p.quantity} @ ₹${p.pricePerPiece.toInt()}", "₹${p.lineTotal.toInt()}")
            }
            PaymentRow("Products total", "₹${order.productsTotal.toInt()}", highlight = true)
            if (order.materialDeductions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                order.materialDeductions.forEach { d ->
                    PaymentRow("${d.label} × ${d.quantity} @ ₹${d.pricePerPiece.toInt()}", "−₹${d.lineTotal.toInt()}", danger = true)
                }
                PaymentRow("Deductions total", "−₹${order.materialDeductionsTotal.toInt()}", danger = true)
            }
            if (order.kharchaGiven > 0) {
                PaymentRow("Kharcha given", "−₹${order.kharchaGiven.toInt()}", danger = true)
            }
        }
    }
}

@Composable
private fun PaymentStatusBadge(isPending: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isPending) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
    ) {
        Text(
            text = stringResource(
                if (isPending) R.string.kaariger_payment_status_pending
                else R.string.kaariger_payment_status_paid
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isPending) Color(0xFFB45309) else Color(0xFF047857),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PaymentRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    danger: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            fontWeight = if (highlight || danger) FontWeight.Bold else FontWeight.Normal,
            color = when {
                danger -> Color(0xFFDC2626)
                highlight -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

