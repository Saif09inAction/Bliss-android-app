package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.R
import com.laiza.worker.core.utils.formatIndianRupee
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderRepair
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.components.KaarigerPaymentTimeline
import com.laiza.worker.presentation.components.labeledKaarigerPayments
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

/**
 * Simple hisaab for the logged-in kaariger:
 * Opening (if any) + current unpaid orders − katauti/credit = total remaining.
 * Payment history stays behind a toggle so the main view stays easy to read.
 */
@Composable
fun KaarigerHisaabScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val session by authViewModel.userSession.collectAsState()
    val orders by orderViewModel.kaarigerOrders.collectAsState()
    val payments by orderViewModel.kaarigerPayments.collectAsState()
    val repairs by orderViewModel.kaarigerRepairs.collectAsState()
    val kaarigers by orderViewModel.kaarigers.collectAsState()
    var showPayments by remember { mutableStateOf(false) }

    LaunchedEffect(session?.phone) {
        session?.phone?.let { orderViewModel.loadKaarigerData(it) }
    }

    val me = remember(kaarigers, session?.phone) {
        kaarigers.find { it.phone == session?.phone }
    }
    val summary = remember(me, orders, payments, repairs) {
        buildKaarigerHisaabSummary(
            openingBalance = me?.openingBalance ?: 0.0,
            oldKharcha = me?.oldKharcha ?: 0.0,
            creditBalance = me?.creditBalance ?: 0.0,
            orders = orders,
            payments = payments,
            repairs = repairs
        )
    }

    val openingLabel = stringResource(R.string.kaariger_payment_opening_product)
    val creditLabel = stringResource(R.string.kaariger_payment_credit_product)
    val allPayments = remember(payments, orders, openingLabel, creditLabel) {
        labeledKaarigerPayments(
            payments = payments,
            orders = orders,
            openingLabel = openingLabel,
            creditLabel = creditLabel
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            stringResource(R.string.kaariger_hisaab_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.kaariger_hisaab_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HisaabEquationCard(summary = summary, payments = payments)

        if (summary.orderLines.isNotEmpty()) {
            Text(
                stringResource(R.string.kaariger_hisaab_orders_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            summary.orderLines.forEach { line ->
                OrderHisaabLineCard(line)
            }
        } else if (summary.runningBalance <= 0.0 && summary.totalRemaining <= 0.0 && allPayments.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.kaariger_hisaab_empty),
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showPayments) {
            OutlinedButton(
                onClick = { showPayments = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.kaariger_hisaab_hide_payments))
            }
            Text(
                stringResource(R.string.kaariger_hisaab_all_payments),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.kaariger_hisaab_all_payments_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            KaarigerPaymentTimeline(allPayments)
        } else if (allPayments.isNotEmpty()) {
            Button(
                onClick = { showPayments = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF047857)
                )
            ) {
                Text(
                    stringResource(
                        R.string.kaariger_hisaab_show_payments,
                        allPayments.size
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

internal data class KaarigerOrderHisaabLine(
    val productName: String,
    val productsTotal: Double,
    val deductions: Double,
    val repair: Double,
    val weekKharcha: Double,
    val paid: Double,
    val kharchaRemaining: Double,
    val createdAt: Long
)

internal data class KaarigerHisaabSummary(
    val runningBalance: Double,
    val weekKharcha: Double,
    val weekKharchaBudget: Double,
    val weekKharchaPaid: Double,
    val standaloneRepair: Double,
    val creditApplied: Double,
    val totalRemaining: Double,
    val surplusCredit: Double,
    val orderLines: List<KaarigerOrderHisaabLine>,
    val activeOrderIds: Set<String>
)

internal fun buildKaarigerHisaabSummary(
    openingBalance: Double,
    oldKharcha: Double = 0.0,
    creditBalance: Double,
    orders: List<KaarigerOrder>,
    payments: List<KaarigerOrderPayment>,
    repairs: List<OrderRepair>
): KaarigerHisaabSummary {
    val running = (openingBalance.coerceAtLeast(0.0) + oldKharcha.coerceAtLeast(0.0))
    val credit = creditBalance.coerceAtLeast(0.0)
    val active = orders.filter { it.status != OrderStatus.REJECTED && it.status != OrderStatus.COMPLETED }

    val orderLines = active.map { order ->
        val orderPayments = payments.filter { it.orderId == order.id }
        val orderRepairs = repairs.filter { it.orderId == order.id && it.isApproved }
        val paid = orderPayments.sumOf { it.amount }
        val productsTotal = if (order.productsTotal > 0) {
            order.productsTotal
        } else {
            order.originalDealAmount ?: order.totalDealAmount
        }
        val deductions = order.materialDeductionsTotal.coerceAtLeast(0.0)
        val repair = order.repairDeductionTotal.takeIf { it > 0 }
            ?: orderRepairs.sumOf { it.totalRepairCost }
        val weekDue = (order.kharchaGiven - order.kharchaCarriedForward).coerceAtLeast(0.0)
        val kharchaRemaining = (weekDue - paid).coerceAtLeast(0.0)
        KaarigerOrderHisaabLine(
            productName = order.productName.ifBlank { "Order" },
            productsTotal = productsTotal,
            deductions = deductions,
            repair = repair,
            weekKharcha = weekDue,
            paid = paid,
            kharchaRemaining = kharchaRemaining,
            createdAt = order.createdAt
        )
    }

    val weekKharcha = orderLines.sumOf { it.kharchaRemaining }
    val weekKharchaBudget = orderLines.sumOf { it.weekKharcha }
    val weekKharchaPaid = orderLines.sumOf { it.paid }
    val standaloneRepair = repairs
        .filter { it.isStandalone && it.isApproved }
        .sumOf { it.totalRepairCost }
    val gross = running + weekKharcha
    val afterRepairs = (gross - standaloneRepair).coerceAtLeast(0.0)
    val creditApplied = minOf(credit, afterRepairs)
    val totalRemaining = (afterRepairs - creditApplied).coerceAtLeast(0.0)
    val surplusCredit = (credit - afterRepairs).coerceAtLeast(0.0)

    return KaarigerHisaabSummary(
        runningBalance = running,
        weekKharcha = weekKharcha,
        weekKharchaBudget = weekKharchaBudget,
        weekKharchaPaid = weekKharchaPaid,
        standaloneRepair = standaloneRepair,
        creditApplied = creditApplied,
        totalRemaining = totalRemaining,
        surplusCredit = surplusCredit,
        orderLines = orderLines,
        activeOrderIds = active.map { it.id }.toSet()
    )
}

@Composable
private fun HisaabEquationCard(
    summary: KaarigerHisaabSummary,
    payments: List<KaarigerOrderPayment>
) {
    val amber = Color(0xFFB45309)
    var showBreakdown by remember { mutableStateOf(false) }
    var showKharchaBreakup by remember { mutableStateOf(false) }

    val weekPayments = remember(payments, summary.activeOrderIds) {
        payments
            .filter { it.orderId in summary.activeOrderIds }
            .sortedWith(compareBy({ it.date }, { it.time }))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = {
                    showBreakdown = !showBreakdown
                    if (showBreakdown) showKharchaBreakup = false
                },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEF3C7),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_total_remaining),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = amber
                    )
                    Text(
                        formatIndianRupee(summary.totalRemaining),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = amber
                    )
                    Text(
                        stringResource(R.string.kaariger_hisaab_tap_calc),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF92400E)
                    )
                }
            }
            Surface(
                onClick = {
                    showKharchaBreakup = !showKharchaBreakup
                    if (showKharchaBreakup) showBreakdown = false
                },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFD1FAE5),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_kharcha_label),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF047857)
                    )
                    Text(
                        formatIndianRupee(summary.weekKharcha),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Color(0xFF047857)
                    )
                    Text(
                        stringResource(R.string.kaariger_hisaab_tap_payments),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF065F46)
                    )
                }
            }
        }

        if (showBreakdown) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEF3C7),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_calc_title),
                        fontWeight = FontWeight.Bold,
                        color = amber
                    )
                    HisaabRow(
                        label = stringResource(R.string.kaariger_hisaab_running_balance),
                        value = formatIndianRupee(summary.runningBalance),
                        valueColor = amber
                    )
                    HisaabRow(
                        label = stringResource(R.string.kaariger_hisaab_kharcha_label),
                        value = formatIndianRupee(summary.weekKharcha),
                        valueColor = Color(0xFF047857)
                    )
                    summary.orderLines.filter { it.weekKharcha > 0.0 }.forEach { line ->
                        HisaabRow(
                            label = "${line.productName} · ${formatIndianRupee(line.weekKharcha)} − ${formatIndianRupee(line.paid)}",
                            value = formatIndianRupee(line.kharchaRemaining),
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (summary.standaloneRepair > 0.0) {
                        HisaabRow(
                            label = stringResource(R.string.kaariger_hisaab_repair_label),
                            value = "−${formatIndianRupee(summary.standaloneRepair)}",
                            valueColor = Color(0xFFB91C1C)
                        )
                    }
                    if (summary.creditApplied > 0.0) {
                        HisaabRow(
                            label = stringResource(R.string.kaariger_hisaab_credit_label),
                            value = "−${formatIndianRupee(summary.creditApplied)}",
                            valueColor = Color(0xFF047857)
                        )
                    }
                    HorizontalDivider(color = Color(0xFFFCD34D))
                    HisaabRow(
                        label = stringResource(R.string.kaariger_hisaab_total_remaining),
                        value = formatIndianRupee(summary.totalRemaining),
                        valueColor = amber
                    )
                    Text(
                        stringResource(R.string.kaariger_hisaab_fold_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        if (showKharchaBreakup) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFD1FAE5),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_kharcha_breakup_title),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    HisaabRow(
                        label = stringResource(R.string.kaariger_hisaab_kharcha_budget),
                        value = formatIndianRupee(summary.weekKharchaBudget),
                        valueColor = Color(0xFF047857)
                    )
                    HorizontalDivider(color = Color(0xFFA7F3D0))
                    Text(
                        stringResource(R.string.kaariger_hisaab_kharcha_payments_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF065F46)
                    )
                    if (weekPayments.isEmpty()) {
                        Text(
                            stringResource(R.string.kaariger_hisaab_kharcha_no_payments),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF065F46)
                        )
                    } else {
                        weekPayments.forEach { p ->
                            HisaabRow(
                                label = listOf(p.date, p.time).filter { it.isNotBlank() }.joinToString(" · "),
                                value = "−${formatIndianRupee(p.amount)}",
                                valueColor = Color(0xFF047857)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFA7F3D0))
                        HisaabRow(
                            label = stringResource(R.string.kaariger_hisaab_kharcha_paid_total),
                            value = formatIndianRupee(summary.weekKharchaPaid),
                            valueColor = Color(0xFF047857)
                        )
                    }
                    HisaabRow(
                        label = stringResource(R.string.kaariger_hisaab_kharcha_label) + " remaining",
                        value = formatIndianRupee(summary.weekKharcha),
                        valueColor = amber
                    )
                }
            }
        }

        if (summary.totalRemaining <= 0.0 && summary.surplusCredit > 0.0) {
            Text(
                stringResource(
                    R.string.kaariger_hisaab_surplus_credit,
                    formatIndianRupee(summary.surplusCredit)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF047857),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OrderHisaabLineCard(line: KaarigerOrderHisaabLine) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                line.productName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.kaariger_hisaab_order_baaki),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatIndianRupee(line.kharchaRemaining),
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB45309)
                )
            }
        }
    }
}

@Composable
private fun HisaabRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
