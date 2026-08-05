package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.R
import com.laiza.worker.core.utils.formatIndianRupee
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private const val DEFAULT_KHARCHA_ROWS = 6
private const val OPENING_ORDER_ID = "__opening__"

@Composable
fun KaarigerPaymentsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val session by authViewModel.userSession.collectAsState()
    val orders by orderViewModel.kaarigerOrders.collectAsState()
    val payments by orderViewModel.kaarigerPayments.collectAsState()
    val repairs by orderViewModel.kaarigerRepairs.collectAsState()
    val kaarigers by orderViewModel.kaarigers.collectAsState()
    var showAllKharcha by remember { mutableStateOf(false) }

    LaunchedEffect(session?.phone) {
        session?.phone?.let { orderViewModel.loadKaarigerData(it) }
    }

    val me = remember(kaarigers, session?.phone) {
        kaarigers.find { it.phone == session?.phone }
    }
    val openingBalance = me?.openingBalance ?: 0.0
    val creditBalance = me?.creditBalance ?: 0.0

    val activeOrders = remember(orders) {
        orders.filter { it.status != OrderStatus.REJECTED }
    }

    val orderSummaries = remember(activeOrders, payments, repairs) {
        activeOrders.map { order ->
            val orderPayments = payments.filter { it.orderId == order.id }
            val orderRepairs = repairs.filter { it.orderId == order.id && it.isApproved }
            val totalPaid = orderPayments.sumOf { it.amount }
            val originalDeal = order.originalDealAmount ?: order.totalDealAmount
            val repairTotal = order.repairDeductionTotal.takeIf { it > 0 }
                ?: orderRepairs.sumOf { it.totalRepairCost }
            val netDeal = (originalDeal - repairTotal).coerceAtLeast(0.0)
            val isCompleted = order.status == OrderStatus.COMPLETED
            val remaining = if (isCompleted) 0.0 else (netDeal - totalPaid).coerceAtLeast(0.0)
            OrderPaymentSummary(orderPayments, order.productName, totalPaid, remaining, isCompleted)
        }
    }

    val openingPayments = remember(payments) {
        payments.filter { isOpeningLikePayment(it) }
    }

    val totalKharchaPaid = remember(orderSummaries, openingPayments) {
        orderSummaries.filter { !it.isCompleted }.sumOf { it.totalPaid } +
            openingPayments.sumOf { it.amount }
    }
    val totalPending = remember(orderSummaries, openingBalance, creditBalance) {
        val gross =
            orderSummaries.filter { !it.isCompleted }.sumOf { it.remaining } +
                openingBalance.coerceAtLeast(0.0)
        (gross - creditBalance.coerceAtLeast(0.0)).coerceAtLeast(0.0)
    }
    val surplusCredit = remember(orderSummaries, openingBalance, creditBalance) {
        val gross =
            orderSummaries.filter { !it.isCompleted }.sumOf { it.remaining } +
                openingBalance.coerceAtLeast(0.0)
        (creditBalance.coerceAtLeast(0.0) - gross).coerceAtLeast(0.0)
    }

    val openingProductLabel = stringResource(R.string.kaariger_payment_opening_product)
    val creditProductLabel = stringResource(R.string.kaariger_payment_credit_product)

    val allPayments = remember(orderSummaries, openingPayments, openingProductLabel, creditProductLabel) {
        val fromOrders = orderSummaries.flatMap { summary ->
            summary.payments.map { PaymentWithOrder(it, summary.productName) }
        }
        val fromOpening = openingPayments.map { payment ->
            val label = when {
                payment.remarks?.contains("credit", ignoreCase = true) == true -> creditProductLabel
                else -> openingProductLabel
            }
            PaymentWithOrder(payment, label)
        }
        (fromOrders + fromOpening).sortedByDescending { it.payment.date + it.payment.time }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.kaariger_payments_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.kaariger_payment_hint_green),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF047857),
                fontWeight = FontWeight.Medium
            )
            Text(
                stringResource(R.string.kaariger_payment_hint_amber),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB45309),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        PaymentsSummaryCard(totalKharchaPaid, totalPending)

        if (totalPending <= 0.0 && surplusCredit > 0.0) {
            Spacer(modifier = Modifier.height(14.dp))
            CreditBalanceCard(surplusCredit)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            stringResource(R.string.kaariger_payment_history),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (allPayments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.kaariger_no_payments),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            val visible = if (showAllKharcha) allPayments else allPayments.take(DEFAULT_KHARCHA_ROWS)
            RecentKharchaList(visible)
            if (allPayments.size > DEFAULT_KHARCHA_ROWS) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showAllKharcha = !showAllKharcha },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        if (showAllKharcha) {
                            stringResource(R.string.kaariger_payment_show_less)
                        } else {
                            stringResource(R.string.kaariger_payment_view_all, allPayments.size)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun isOpeningLikePayment(p: KaarigerOrderPayment): Boolean {
    if (p.orderId == OPENING_ORDER_ID) return true
    val r = p.remarks ?: return false
    return r == "Opening / old remaining payment" ||
        r == "Old remaining payment" ||
        r == "Extra kharcha — carried as credit"
}

private data class OrderPaymentSummary(
    val payments: List<KaarigerOrderPayment>,
    val productName: String,
    val totalPaid: Double,
    val remaining: Double,
    val isCompleted: Boolean
)

private data class PaymentWithOrder(val payment: KaarigerOrderPayment, val productName: String)

@Composable
private fun PaymentsSummaryCard(totalPaid: Double, totalPending: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFD1FAE5),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                Text(
                    stringResource(R.string.kaariger_payment_advance),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF047857),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    formatIndianRupee(totalPaid),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF047857),
                    fontSize = 40.sp,
                    lineHeight = 44.sp
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFEF3C7),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                Text(
                    stringResource(R.string.kaariger_payment_remaining),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    formatIndianRupee(totalPending),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFB45309),
                    fontSize = 40.sp,
                    lineHeight = 44.sp
                )
            }
        }
    }
}

@Composable
private fun CreditBalanceCard(creditBalance: Double) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFDCFCE7)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                stringResource(R.string.kaariger_payment_extra_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF047857),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.kaariger_payment_extra_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF047857)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                formatIndianRupee(creditBalance),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF047857),
                fontSize = 32.sp
            )
        }
    }
}

@Composable
private fun RecentKharchaList(entries: List<PaymentWithOrder>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            entries.forEachIndexed { index, entry ->
                val payment = entry.payment
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFD1FAE5),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                "₹",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                R.string.kaariger_payment_received_amount,
                                formatIndianRupee(payment.amount)
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            entry.productName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            formatPaymentDayDate(payment.date, payment.time),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        payment.remarks?.takeIf { it.isNotBlank() }?.let { note ->
                            Text(
                                note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (index != entries.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

/** e.g. "Saturday, 1 Aug 2026 · 6:45 pm" */
private fun formatPaymentDayDate(date: String, time: String): String {
    val dayPart = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
        if (parsed != null) {
            SimpleDateFormat("EEEE, d MMM yyyy", Locale.ENGLISH).format(parsed)
        } else date
    } catch (_: Exception) {
        date
    }
    val timePart = time.trim().takeIf { it.isNotEmpty() } ?: return dayPart
    return "$dayPart · $timePart"
}
