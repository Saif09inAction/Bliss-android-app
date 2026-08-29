package com.laiza.worker.presentation.screens

import androidx.compose.foundation.BorderStroke
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
import com.laiza.worker.domain.hisaab.orderAddBalance
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderRepair
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.components.KaarigerPaymentTimeline
import com.laiza.worker.presentation.components.KaarigerPreviousHisaabPanel
import com.laiza.worker.presentation.components.formatOrderDate
import com.laiza.worker.presentation.components.labeledKaarigerPayments
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

private val Amber = Color(0xFFB45309)
private val AmberSoft = Color(0xFFFEF3C7)
private val Jade = Color(0xFF047857)
private val JadeSoft = Color(0xFFD1FAE5)

/**
 * Mera Hisaab — mirrors admin Remaining + Kharcha box + Previous bills.
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

    val previousBills = remember(orders) {
        orders
            .filter { it.status == OrderStatus.COMPLETED }
            .sortedByDescending { it.createdAt }
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

        HisaabEquationCard(
            summary = summary,
            payments = payments,
            orders = orders
        )

        if (summary.orderLines.isNotEmpty()) {
            Text(
                stringResource(R.string.kaariger_hisaab_this_week_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            summary.orderLines.forEach { line ->
                OrderHisaabLineCard(line)
            }
        } else if (
            summary.runningBalance <= 0.0 &&
            summary.totalRemaining <= 0.0 &&
            allPayments.isEmpty() &&
            previousBills.isEmpty()
        ) {
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

        if (previousBills.isNotEmpty()) {
            Text(
                stringResource(R.string.kaariger_hisaab_previous_bills_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            previousBills.forEach { order ->
                PreviousBillCard(
                    order = order,
                    payments = payments.filter { it.orderId == order.id },
                    repairs = repairs
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
            KaarigerPaymentTimeline(allPayments)
        } else if (allPayments.isNotEmpty()) {
            Button(
                onClick = { showPayments = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Jade)
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
    val orderId: String,
    val productName: String,
    val weekLabel: String,
    val productsTotal: Double,
    val deductions: Double,
    val repair: Double,
    val weekKharcha: Double,
    val paidCash: Double,
    val priorOverpay: Double,
    val paid: Double,
    val kharchaRemaining: Double,
    val createdAt: Long
)

internal data class RemainingLedgerLine(
    val title: String,
    val subtitle: String? = null,
    val delta: Double,
    val remainingAfter: Double
)

internal data class KaarigerHisaabSummary(
    val runningBalance: Double,
    val weekKharcha: Double,
    val weekKharchaBudget: Double,
    val weekKharchaPaidCash: Double,
    val priorOverpayApplied: Double,
    val weekKharchaPaid: Double,
    val standaloneRepair: Double,
    val creditApplied: Double,
    val totalRemaining: Double,
    val surplusCredit: Double,
    val orderLines: List<KaarigerOrderHisaabLine>,
    val activeOrderIds: Set<String>,
    val remainingLedger: List<RemainingLedgerLine>
)

private fun isOpeningPayment(p: KaarigerOrderPayment): Boolean {
    val remarks = (p.remarks ?: "").lowercase()
    if (remarks.contains("carried as credit") || remarks.contains("credit carried")) return false
    return p.orderId == "__opening__" ||
        remarks.contains("opening balance") ||
        remarks.contains("old remaining")
}

private fun isCreditPayment(p: KaarigerOrderPayment): Boolean {
    val remarks = (p.remarks ?: "").lowercase()
    return remarks.contains("carried as credit") || remarks.contains("credit carried")
}

private fun paymentSortKey(p: KaarigerOrderPayment): Long {
    if (p.createdAt > 0L) return p.createdAt
    return 0L
}

internal fun buildKaarigerHisaabSummary(
    openingBalance: Double,
    oldKharcha: Double = 0.0,
    creditBalance: Double,
    orders: List<KaarigerOrder>,
    payments: List<KaarigerOrderPayment>,
    repairs: List<OrderRepair>?
): KaarigerHisaabSummary {
    val running = (openingBalance.coerceAtLeast(0.0) + oldKharcha.coerceAtLeast(0.0))
    val credit = creditBalance.coerceAtLeast(0.0)
    val usable = orders.filter { it.status != OrderStatus.REJECTED }
    val active = usable.filter { it.status != OrderStatus.COMPLETED }

    val orderLines = active.map { order ->
        val orderPayments = payments.filter {
            it.orderId == order.id && !isOpeningPayment(it) && !isCreditPayment(it)
        }
        val orderRepairs = (repairs ?: emptyList()).filter {
            if (order.status == OrderStatus.COMPLETED) {
                it.orderId == order.id && it.isApproved
            } else {
                (it.isStandalone || it.orderId == order.id) && it.isApproved
            }
        }
        val paidCash = orderPayments.sumOf { it.amount }
        val priorOverpay = order.kharchaCarryIn.coerceAtLeast(0.0)
        val productsTotal = if (order.productsTotal > 0) {
            order.productsTotal
        } else {
            order.originalDealAmount ?: order.totalDealAmount
        }
        val deductions = order.materialDeductionsTotal.coerceAtLeast(0.0)
        val repair = if (order.status == OrderStatus.COMPLETED) {
            order.repairDeductionTotal.takeIf { it > 0 } ?: orderRepairs.sumOf { it.totalRepairCost }
        } else {
            orderRepairs.sumOf { it.totalRepairCost }
        }
        val weekDue = (order.kharchaGiven - order.kharchaCarriedForward).coerceAtLeast(0.0)
        val kharchaRemaining = weekDue - order.kharchaCarryIn - paidCash
        KaarigerOrderHisaabLine(
            orderId = order.id,
            productName = order.productName.ifBlank { "Order" },
            weekLabel = order.displayWeekLabel(),
            productsTotal = productsTotal,
            deductions = deductions,
            repair = repair,
            weekKharcha = weekDue,
            paidCash = paidCash,
            priorOverpay = priorOverpay,
            paid = paidCash + priorOverpay,
            kharchaRemaining = kharchaRemaining,
            createdAt = order.createdAt
        )
    }

    val weekKharcha = orderLines.sumOf { it.kharchaRemaining }
    val weekKharchaBudget = orderLines.sumOf { it.weekKharcha }
    val weekKharchaPaidCash = orderLines.sumOf { it.paidCash }
    val priorOverpayApplied = orderLines.sumOf { it.priorOverpay }
    val weekKharchaPaid = weekKharchaPaidCash + priorOverpayApplied
    val standaloneRepair = (repairs ?: emptyList())
        .filter { it.isStandalone && it.isApproved }
        .sumOf { it.totalRepairCost }
    val afterRepairs = (running - standaloneRepair).coerceAtLeast(0.0)
    val creditApplied = minOf(credit, afterRepairs)
    val totalRemaining = (afterRepairs - creditApplied).coerceAtLeast(0.0)
    val surplusCredit = (credit - afterRepairs).coerceAtLeast(0.0)

    val remainingLedger = buildRemainingLedgerLines(
        openingBalance = openingBalance.coerceAtLeast(0.0),
        oldKharcha = oldKharcha.coerceAtLeast(0.0),
        orders = usable.sortedBy { it.createdAt },
        payments = payments,
        repairs = repairs,
        standaloneRepair = standaloneRepair,
        creditApplied = creditApplied,
        liveRemaining = totalRemaining
    )

    return KaarigerHisaabSummary(
        runningBalance = running,
        weekKharcha = weekKharcha,
        weekKharchaBudget = weekKharchaBudget,
        weekKharchaPaidCash = weekKharchaPaidCash,
        priorOverpayApplied = priorOverpayApplied,
        weekKharchaPaid = weekKharchaPaid,
        standaloneRepair = standaloneRepair,
        creditApplied = creditApplied,
        totalRemaining = totalRemaining,
        surplusCredit = surplusCredit,
        orderLines = orderLines,
        activeOrderIds = active.map { it.id }.toSet(),
        remainingLedger = remainingLedger
    )
}

private fun buildRemainingLedgerLines(
    openingBalance: Double,
    oldKharcha: Double,
    orders: List<KaarigerOrder>,
    payments: List<KaarigerOrderPayment>,
    repairs: List<OrderRepair>?,
    standaloneRepair: Double,
    creditApplied: Double,
    liveRemaining: Double
): List<RemainingLedgerLine> {
    val openingPays = payments.filter { isOpeningPayment(it) }
    val openingPaidTotal = openingPays.sumOf { it.amount.coerceAtLeast(0.0) }
    val billNet = orders.sumOf { orderAddBalance(it, repairs) - it.kharchaGiven.coerceAtLeast(0.0) }
    val foldTotal = orders.sumOf { it.kharchaCarriedForward.coerceAtLeast(0.0) }
    val startOpening = (
        openingBalance + openingPaidTotal - billNet - foldTotal
        ).coerceAtLeast(0.0)

    val lines = mutableListOf<RemainingLedgerLine>()
    var remaining = 0.0

    remaining = startOpening
    lines += RemainingLedgerLine(
        title = "Opening balance",
        delta = startOpening,
        remainingAfter = remaining
    )
    if (oldKharcha > 0.0) {
        remaining += oldKharcha
        lines += RemainingLedgerLine(
            title = "Old kharcha",
            delta = oldKharcha,
            remainingAfter = remaining
        )
    }

    data class Ev(val at: Long, val id: String, val build: (Double) -> Pair<RemainingLedgerLine, Double>)
    val events = mutableListOf<Ev>()

    orders.forEachIndexed { i, order ->
        val t = if (order.createdAt > 0L) order.createdAt else i.toLong()
        val add = orderAddBalance(order, repairs)
        val week = order.displayWeekLabel()
        if (add != 0.0) {
            events += Ev(t, "add-${order.id}") { rem ->
                val next = (rem + add).coerceAtLeast(0.0)
                RemainingLedgerLine(
                    title = "Bill · $week",
                    subtitle = order.productName.takeIf { it.isNotBlank() },
                    delta = add,
                    remainingAfter = next
                ) to next
            }
        }
        val kh = order.kharchaGiven.coerceAtLeast(0.0)
        if (kh > 0.0) {
            events += Ev(t + 1, "kh-${order.id}") { rem ->
                val next = (rem - kh).coerceAtLeast(0.0)
                RemainingLedgerLine(
                    title = "$week kharcha",
                    delta = -kh,
                    remainingAfter = next
                ) to next
            }
        }
    }

    openingPays.forEach { p ->
        val amt = p.amount.coerceAtLeast(0.0)
        if (amt <= 0.0) return@forEach
        events += Ev(paymentSortKey(p), "pay-${p.id}") { rem ->
            val next = (rem - amt).coerceAtLeast(0.0)
            val whenLabel = listOf(p.date, p.time).filter { it.isNotBlank() }.joinToString(" · ")
            RemainingLedgerLine(
                title = "Paid (Remaining)",
                subtitle = whenLabel.ifBlank { null },
                delta = -amt,
                remainingAfter = next
            ) to next
        }
    }

    events.sortedWith(compareBy({ it.at }, { it.id })).forEach { ev ->
        val (line, next) = ev.build(remaining)
        remaining = next
        lines += line
    }

    val approvedStandaloneRepairs = (repairs ?: emptyList()).filter { it.isStandalone && it.isApproved }
    if (approvedStandaloneRepairs.isNotEmpty()) {
        approvedStandaloneRepairs.forEach { r ->
            remaining = (remaining - r.totalRepairCost).coerceAtLeast(0.0)
            val sub = if (r.faultyQuantity > 0) "${r.faultyQuantity} × ₹${r.faultyPricePerPiece.toInt()}" else null
            lines += RemainingLedgerLine(
                title = "Repairing - ${r.productName}",
                subtitle = sub,
                delta = -r.totalRepairCost,
                remainingAfter = remaining
            )
        }
    } else if (standaloneRepair > 0.0) {
        remaining = (remaining - standaloneRepair).coerceAtLeast(0.0)
        lines += RemainingLedgerLine(
            title = "Repairing",
            delta = -standaloneRepair,
            remainingAfter = remaining
        )
    }
    if (creditApplied > 0.0) {
        remaining = (remaining - creditApplied).coerceAtLeast(0.0)
        lines += RemainingLedgerLine(
            title = "Credit",
            delta = -creditApplied,
            remainingAfter = remaining
        )
    }

    // Align last line to live Remaining if float drift
    if (lines.isNotEmpty() && kotlin.math.abs(lines.last().remainingAfter - liveRemaining) > 0.5) {
        lines[lines.lastIndex] = lines.last().copy(remainingAfter = liveRemaining)
    }
    return lines
}

@Composable
private fun HisaabEquationCard(
    summary: KaarigerHisaabSummary,
    payments: List<KaarigerOrderPayment>,
    orders: List<KaarigerOrder>
) {
    var showRemaining by remember { mutableStateOf(false) }
    var showKharcha by remember { mutableStateOf(false) }

    val weekPayments = remember(payments, summary.activeOrderIds) {
        payments
            .filter {
                it.orderId in summary.activeOrderIds &&
                    !isOpeningPayment(it) &&
                    !isCreditPayment(it)
            }
            .sortedWith(compareBy({ it.date }, { it.time }))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = {
                    showRemaining = !showRemaining
                    if (showRemaining) showKharcha = false
                },
                shape = RoundedCornerShape(16.dp),
                color = AmberSoft,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_total_remaining),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = Amber
                    )
                    Text(
                        formatIndianRupee(summary.totalRemaining),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Amber
                    )
                }
            }
            Surface(
                onClick = {
                    showKharcha = !showKharcha
                    if (showKharcha) showRemaining = false
                },
                shape = RoundedCornerShape(16.dp),
                color = JadeSoft,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_kharcha_label),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = Jade
                    )
                    Text(
                        formatIndianRupee(summary.weekKharcha),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Jade
                    )
                }
            }
        }

        if (showRemaining) {
            RemainingBreakdownPanel(summary)
        }

        if (showKharcha) {
            KharchaBreakdownPanel(summary, weekPayments)
        }

        if (summary.totalRemaining <= 0.0 && summary.surplusCredit > 0.0) {
            Text(
                stringResource(
                    R.string.kaariger_hisaab_surplus_credit,
                    formatIndianRupee(summary.surplusCredit)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Jade,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RemainingBreakdownPanel(summary: KaarigerHisaabSummary) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AmberSoft,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.kaariger_hisaab_remaining_breakdown_title),
                fontWeight = FontWeight.Bold,
                color = Amber
            )
            if (summary.remainingLedger.isEmpty()) {
                HisaabRow(
                    label = stringResource(R.string.kaariger_hisaab_running_balance),
                    value = formatIndianRupee(summary.runningBalance),
                    valueColor = Amber
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
                        valueColor = Jade
                    )
                }
            } else {
                summary.remainingLedger.forEach { line ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        HisaabRow(
                            label = line.title,
                            value = when {
                                line.delta > 0 -> "+${formatIndianRupee(line.delta)}"
                                line.delta < 0 -> "−${formatIndianRupee(kotlin.math.abs(line.delta))}"
                                else -> "—"
                            },
                            valueColor = when {
                                line.delta > 0 -> Jade
                                line.delta < 0 -> Color(0xFFB91C1C)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (!line.subtitle.isNullOrBlank()) {
                            Text(
                                line.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            stringResource(
                                R.string.kaariger_hisaab_remaining_after,
                                formatIndianRupee(line.remainingAfter)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Amber
                        )
                    }
                    HorizontalDivider(color = Color(0xFFFCD34D).copy(alpha = 0.5f))
                }
            }
            HorizontalDivider(color = Color(0xFFFCD34D))
            HisaabRow(
                label = stringResource(R.string.kaariger_hisaab_total_remaining),
                value = formatIndianRupee(summary.totalRemaining),
                valueColor = Amber,
                bold = true
            )
        }
    }
}

@Composable
private fun KharchaBreakdownPanel(
    summary: KaarigerHisaabSummary,
    weekPayments: List<KaarigerOrderPayment>
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = JadeSoft,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.kaariger_hisaab_kharcha_breakup_title),
                fontWeight = FontWeight.Bold,
                color = Jade
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStat(
                    label = stringResource(R.string.kaariger_hisaab_kharcha_budget),
                    value = formatIndianRupee(summary.weekKharchaBudget),
                    modifier = Modifier.weight(1f)
                )
                MiniStat(
                    label = stringResource(R.string.kaariger_hisaab_kharcha_paid_total),
                    value = formatIndianRupee(summary.weekKharchaPaid),
                    modifier = Modifier.weight(1f)
                )
                MiniStat(
                    label = if (summary.weekKharcha < 0) {
                        stringResource(R.string.kaariger_hisaab_kharcha_extra)
                    } else {
                        stringResource(R.string.kaariger_hisaab_kharcha_left)
                    },
                    value = formatIndianRupee(summary.weekKharcha),
                    modifier = Modifier.weight(1f)
                )
            }
            if (summary.priorOverpayApplied > 0.0) {
                HisaabRow(
                    label = stringResource(R.string.kaariger_hisaab_prior_overpay),
                    value = formatIndianRupee(summary.priorOverpayApplied),
                    valueColor = Jade
                )
            }
            HorizontalDivider(color = Color(0xFFA7F3D0))
            Text(
                stringResource(R.string.kaariger_hisaab_kharcha_payments_title),
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF065F46)
            )
            if (weekPayments.isEmpty() && summary.priorOverpayApplied <= 0.0) {
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
                        valueColor = Jade
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.85f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, fontWeight = FontWeight.Bold, color = Jade, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PreviousBillCard(
    order: KaarigerOrder,
    payments: List<KaarigerOrderPayment>,
    repairs: List<OrderRepair>?
) {
    var expanded by remember(order.id) { mutableStateOf(false) }
    val week = order.displayWeekLabel()
    val add = orderAddBalance(order, repairs)
    val budget = order.kharchaGiven.coerceAtLeast(0.0)
    val opening = order.openingAtCreation?.coerceAtLeast(0.0)
        ?: if (order.status == OrderStatus.COMPLETED && order.closingAtCreation != null) {
            (order.closingAtCreation - add + budget).coerceAtLeast(0.0)
        } else 0.0
    val closing = if (order.status == OrderStatus.COMPLETED) {
        order.closingAtCreation ?: (opening + add - budget).coerceAtLeast(0.0)
    } else {
        (opening + add - budget).coerceAtLeast(0.0)
    }

    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(week, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            formatOrderDate(order.createdAt).takeIf { it != "—" },
                            order.productName.takeIf { it.isNotBlank() },
                            stringResource(R.string.kaariger_previous_hisaab_badge)
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.kaariger_hisaab_outstanding_after),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatIndianRupee(closing),
                        fontWeight = FontWeight.ExtraBold,
                        color = Amber
                    )
                }
            }

            if (expanded) {
                HorizontalDivider()
                KaarigerPreviousHisaabPanel(
                    order = order,
                    payments = payments,
                    repairs = repairs,
                    showHeader = false
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    line.weekLabel,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                if (line.productName.isNotBlank() && line.productName != "Order") {
                    Text(
                        line.productName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    stringResource(
                        R.string.kaariger_hisaab_week_paid_left,
                        formatIndianRupee(line.paid),
                        formatIndianRupee(line.kharchaRemaining)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                    color = Amber
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
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
