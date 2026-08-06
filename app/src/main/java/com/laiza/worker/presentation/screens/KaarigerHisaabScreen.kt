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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.laiza.worker.presentation.components.isOpeningBalancePayment
import com.laiza.worker.presentation.components.labeledKaarigerPayments
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

/**
 * Full hisaab for the logged-in kaariger:
 * Opening (if any) + current unpaid orders − katauti/credit = total remaining.
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

    LaunchedEffect(session?.phone) {
        session?.phone?.let { orderViewModel.loadKaarigerData(it) }
    }

    val me = remember(kaarigers, session?.phone) {
        kaarigers.find { it.phone == session?.phone }
    }
    val summary = remember(me, orders, payments, repairs) {
        buildKaarigerHisaabSummary(
            openingBalance = me?.openingBalance ?: 0.0,
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

        HisaabEquationCard(summary)

        if (summary.orderLines.isNotEmpty()) {
            Text(
                stringResource(R.string.kaariger_hisaab_orders_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            summary.orderLines.forEach { line ->
                OrderHisaabLineCard(line)
            }
        } else if (summary.opening <= 0.0 && summary.openingPaid <= 0.0 && allPayments.isEmpty()) {
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

        Spacer(modifier = Modifier.height(12.dp))
    }
}

internal data class KaarigerOrderHisaabLine(
    val productName: String,
    val productsTotal: Double,
    val deductions: Double,
    val repair: Double,
    val paid: Double,
    val remaining: Double
)

internal data class KaarigerHisaabSummary(
    val opening: Double,
    val openingPaid: Double,
    val billsRemaining: Double,
    val billsPaid: Double,
    val orderDeductions: Double,
    val standaloneRepair: Double,
    val creditApplied: Double,
    val totalPaid: Double,
    val totalRemaining: Double,
    val surplusCredit: Double,
    val orderLines: List<KaarigerOrderHisaabLine>
) {
    val katautiTotal: Double get() = orderDeductions + standaloneRepair
}

internal fun buildKaarigerHisaabSummary(
    openingBalance: Double,
    creditBalance: Double,
    orders: List<KaarigerOrder>,
    payments: List<KaarigerOrderPayment>,
    repairs: List<OrderRepair>
): KaarigerHisaabSummary {
    val opening = openingBalance.coerceAtLeast(0.0)
    val credit = creditBalance.coerceAtLeast(0.0)
    val openingPaid = payments.filter { isOpeningBalancePayment(it) }.sumOf { it.amount }
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
        val net = (productsTotal - deductions - repair).coerceAtLeast(0.0)
            .let { base ->
                // Prefer stored deal when products breakdown is empty/legacy.
                if (order.products.isEmpty()) {
                    ((order.originalDealAmount ?: order.totalDealAmount) - repair).coerceAtLeast(0.0)
                } else base
            }
        val remaining = (net - paid).coerceAtLeast(0.0)
        KaarigerOrderHisaabLine(
            productName = order.productName.ifBlank { "Order" },
            productsTotal = productsTotal,
            deductions = deductions,
            repair = repair,
            paid = paid,
            remaining = remaining
        )
    }

    val billsRemaining = orderLines.sumOf { it.remaining }
    val billsPaid = orderLines.sumOf { it.paid }
    val orderDeductions = orderLines.sumOf { it.deductions }
    val standaloneRepair = repairs
        .filter { it.isStandalone && it.isApproved }
        .sumOf { it.totalRepairCost }
    val gross = opening + billsRemaining
    val afterRepairs = (gross - standaloneRepair).coerceAtLeast(0.0)
    val creditApplied = minOf(credit, afterRepairs)
    val totalRemaining = (afterRepairs - creditApplied).coerceAtLeast(0.0)
    val surplusCredit = (credit - afterRepairs).coerceAtLeast(0.0)
    val totalPaid = payments.sumOf { it.amount }

    return KaarigerHisaabSummary(
        opening = opening,
        openingPaid = openingPaid,
        billsRemaining = billsRemaining,
        billsPaid = billsPaid,
        orderDeductions = orderDeductions,
        standaloneRepair = standaloneRepair,
        creditApplied = creditApplied,
        totalPaid = totalPaid,
        totalRemaining = totalRemaining,
        surplusCredit = surplusCredit,
        orderLines = orderLines
    )
}

@Composable
private fun HisaabEquationCard(summary: KaarigerHisaabSummary) {
    val amber = Color(0xFFB45309)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFEF3C7),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.kaariger_hisaab_calc_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = amber
            )

            if (summary.opening > 0.0 || summary.openingPaid > 0.0) {
                HisaabRow(
                    label = stringResource(R.string.kaariger_payment_opening_label),
                    value = formatIndianRupee(summary.opening),
                    valueColor = amber
                )
            }
            HisaabRow(
                label = stringResource(R.string.kaariger_hisaab_current_orders),
                value = formatIndianRupee(summary.billsRemaining)
            )
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

            if (summary.openingPaid > 0.0 || summary.billsPaid > 0.0 || summary.totalPaid > 0.0) {
                HorizontalDivider(color = Color(0xFFFCD34D))
                Text(
                    stringResource(R.string.kaariger_hisaab_payments_so_far),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857)
                )
                if (summary.openingPaid > 0.0) {
                    HisaabRow(
                        label = stringResource(R.string.kaariger_hisaab_opening_paid),
                        value = formatIndianRupee(summary.openingPaid),
                        valueColor = Color(0xFF047857)
                    )
                }
                if (summary.billsPaid > 0.0) {
                    HisaabRow(
                        label = stringResource(R.string.kaariger_hisaab_orders_paid),
                        value = formatIndianRupee(summary.billsPaid),
                        valueColor = Color(0xFF047857)
                    )
                }
                if (summary.totalPaid > 0.0) {
                    HisaabRow(
                        label = stringResource(R.string.kaariger_hisaab_total_paid),
                        value = formatIndianRupee(summary.totalPaid),
                        valueColor = Color(0xFF047857),
                        bold = true
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFFCD34D))

            // Simple equation line
            Text(
                if (summary.opening > 0.0) {
                    stringResource(
                        R.string.kaariger_hisaab_equation_with_opening,
                        formatIndianRupee(summary.opening),
                        formatIndianRupee(summary.billsRemaining),
                        formatIndianRupee(summary.totalRemaining)
                    )
                } else {
                    stringResource(
                        R.string.kaariger_hisaab_equation_orders_only,
                        formatIndianRupee(summary.billsRemaining),
                        formatIndianRupee(summary.totalRemaining)
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF92400E),
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.kaariger_hisaab_total_remaining),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = amber
                )
                Text(
                    formatIndianRupee(summary.totalRemaining),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = amber
                )
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
}

@Composable
private fun OrderHisaabLineCard(line: KaarigerOrderHisaabLine) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(line.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            HisaabRow(stringResource(R.string.kaariger_detail_products_total), formatIndianRupee(line.productsTotal))
            if (line.deductions > 0.0) {
                HisaabRow(
                    stringResource(R.string.kaariger_detail_deductions),
                    "−${formatIndianRupee(line.deductions)}",
                    Color(0xFFB91C1C)
                )
            }
            if (line.repair > 0.0) {
                HisaabRow(
                    stringResource(R.string.kaariger_detail_repair_deduction),
                    "−${formatIndianRupee(line.repair)}",
                    Color(0xFFB91C1C)
                )
            }
            if (line.paid > 0.0) {
                HisaabRow(
                    stringResource(R.string.kaariger_grand_total_paid),
                    "−${formatIndianRupee(line.paid)}",
                    Color(0xFF047857)
                )
            }
            HorizontalDivider()
            HisaabRow(
                stringResource(R.string.kaariger_hisaab_order_baaki),
                formatIndianRupee(line.remaining),
                Color(0xFFB45309),
                bold = true
            )
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
