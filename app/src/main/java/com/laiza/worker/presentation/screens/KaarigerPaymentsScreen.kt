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
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.components.KaarigerPaymentTimeline
import com.laiza.worker.presentation.components.labeledKaarigerPayments
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

private const val DEFAULT_KHARCHA_ROWS = 6

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
    val oldKharcha = me?.oldKharcha ?: 0.0
    val creditBalance = me?.creditBalance ?: 0.0

    val activeOrders = remember(orders) {
        orders.filter { it.status != OrderStatus.REJECTED && it.status != OrderStatus.COMPLETED }
    }

    val orderSummaries = remember(activeOrders, payments, repairs) {
        activeOrders.map { order ->
            val orderPayments = payments.filter { it.orderId == order.id }
            val totalPaid = orderPayments.sumOf { it.amount }
            val weekDue = (order.kharchaGiven - order.kharchaCarriedForward).coerceAtLeast(0.0)
            val remaining = (weekDue - totalPaid).coerceAtLeast(0.0)
            val isCompleted = order.status == OrderStatus.COMPLETED
            OrderPaymentSummary(orderPayments, order.productName, totalPaid, remaining, isCompleted)
        }
    }

    val weekKharchaRemaining = remember(orderSummaries) {
        orderSummaries.sumOf { it.remaining }
    }
    val standaloneRepairTotal = remember(repairs) {
        repairs.filter { it.isStandalone && it.isApproved }.sumOf { it.totalRepairCost }
    }
    // All kharcha ever received (orders + opening + credit) — every transaction counts.
    val totalKharchaPaid = remember(payments) {
        payments.sumOf { it.amount }
    }
    val safeOpening = (openingBalance + oldKharcha).coerceAtLeast(0.0)
    val safeCredit = creditBalance.coerceAtLeast(0.0)
    // Total remaining = running balance + week kharcha unpaid − credit − repairs.
    val grossOwed = weekKharchaRemaining + safeOpening
    val totalPending = (grossOwed - safeCredit - standaloneRepairTotal).coerceAtLeast(0.0)
    val afterRepairs = (grossOwed - standaloneRepairTotal).coerceAtLeast(0.0)
    val surplusCredit = (safeCredit - afterRepairs).coerceAtLeast(0.0)

    val openingProductLabel = stringResource(R.string.kaariger_payment_opening_product)
    val creditProductLabel = stringResource(R.string.kaariger_payment_credit_product)

    // Show every payment — including opening-balance clears — never drop orphans.
    val allPayments = remember(payments, orders, openingProductLabel, creditProductLabel) {
        labeledKaarigerPayments(
            payments = payments,
            orders = orders,
            openingLabel = openingProductLabel,
            creditLabel = creditProductLabel
        )
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

        PaymentsSummaryCard(
            totalPaid = totalKharchaPaid,
            totalPending = totalPending,
            openingBalance = safeOpening,
            billsRemaining = weekKharchaRemaining
        )

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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.kaariger_payment_history_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Newest payments first (opening-balance clears included).
        val visible = if (showAllKharcha) allPayments else allPayments.take(DEFAULT_KHARCHA_ROWS)
        KaarigerPaymentTimeline(visible)
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
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private data class OrderPaymentSummary(
    val payments: List<com.laiza.worker.domain.models.KaarigerOrderPayment>,
    val productName: String,
    val totalPaid: Double,
    val remaining: Double,
    val isCompleted: Boolean
)

@Composable
private fun PaymentsSummaryCard(
    totalPaid: Double,
    totalPending: Double,
    openingBalance: Double,
    billsRemaining: Double
) {
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
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    if (openingBalance > 0.0) {
                        stringResource(
                            R.string.kaariger_hisaab_equation_with_opening,
                            formatIndianRupee(openingBalance),
                            formatIndianRupee(billsRemaining),
                            formatIndianRupee(totalPending)
                        )
                    } else {
                        stringResource(
                            R.string.kaariger_hisaab_equation_orders_only,
                            formatIndianRupee(billsRemaining),
                            formatIndianRupee(totalPending)
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF92400E),
                    fontWeight = FontWeight.Medium
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

