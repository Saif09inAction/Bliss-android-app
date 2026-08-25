package com.laiza.worker.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiza.worker.R
import com.laiza.worker.core.utils.formatIndianRupee
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderRepair
import com.laiza.worker.domain.models.OrderStatus

private val Amber = Color(0xFFB45309)
private val Jade = Color(0xFF047857)
private val JadeSoft = Color(0xFFD1FAE5)
private val ExtraRed = Color(0xFFB91C1C)

/**
 * Admin-style Previous hisaab: WEEK BILL | KHARCHA THAT WEEK.
 */
@Composable
fun KaarigerPreviousHisaabPanel(
    order: KaarigerOrder,
    payments: List<KaarigerOrderPayment>,
    repairs: List<OrderRepair>? = emptyList(),
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val weekPays = remember(payments) {
        payments
            .filter { !isWeekKharchaExcluded(it) }
            .sortedWith(compareBy({ it.date }, { it.time }, { it.createdAt }))
    }
    val paidCash = weekPays.sumOf { it.amount.coerceAtLeast(0.0) }
    val priorOverpay = order.kharchaCarryIn.coerceAtLeast(0.0)
    val paidDisplay = paidCash + priorOverpay
    val budget = order.kharchaGiven.coerceAtLeast(0.0)
    val box = budget - order.kharchaCarryIn - paidCash
    val productsTotal = if (order.productsTotal > 0) order.productsTotal
    else order.originalDealAmount ?: order.totalDealAmount

    val orderRepairs = remember(repairs, order.id, order.status) {
        val list = repairs ?: emptyList()
        if (order.status == OrderStatus.COMPLETED) {
            list.filter { it.orderId == order.id && it.isApproved }
        } else {
            list.filter { (it.isStandalone || it.orderId == order.id) && it.isApproved }
        }
    }
    val repairTotal = if (orderRepairs.isNotEmpty()) orderRepairs.sumOf { it.totalRepairCost } else order.repairDeductionTotal.coerceAtLeast(0.0)

    val add = order.addBalance
        ?: (productsTotal - order.materialDeductionsTotal.coerceAtLeast(0.0) - repairTotal)
    val opening = order.openingAtCreation?.coerceAtLeast(0.0)
        ?: ((order.closingAtCreation ?: 0.0) - add + budget).coerceAtLeast(0.0)
    val closing = order.closingAtCreation
        ?: (opening + add - budget).coerceAtLeast(0.0)

    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val dateLabel = formatOrderDate(order.createdAt)
    val subtitle = listOfNotNull(
        dateLabel.takeIf { it != "—" },
        order.productName.takeIf { it.isNotBlank() },
        stringResource(R.string.kaariger_previous_hisaab_badge)
    ).joinToString(" · ")

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showHeader) {
            Text(
                order.displayWeekLabel(),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeekBillColumn(
                    order = order,
                    opening = opening,
                    productsTotal = productsTotal,
                    add = add,
                    budget = budget,
                    closing = closing,
                    repairs = repairs,
                    modifier = Modifier.weight(1f)
                )
                KharchaThatWeekColumn(
                    budget = budget,
                    paidDisplay = paidDisplay,
                    box = box,
                    priorOverpay = priorOverpay,
                    weekPays = weekPays,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            WeekBillColumn(
                order = order,
                opening = opening,
                productsTotal = productsTotal,
                add = add,
                budget = budget,
                closing = closing,
                repairs = repairs
            )
            KharchaThatWeekColumn(
                budget = budget,
                paidDisplay = paidDisplay,
                box = box,
                priorOverpay = priorOverpay,
                weekPays = weekPays
            )
        }
    }
}

@Composable
private fun WeekBillColumn(
    order: KaarigerOrder,
    opening: Double,
    productsTotal: Double,
    add: Double,
    budget: Double,
    closing: Double,
    repairs: List<OrderRepair>?,
    modifier: Modifier = Modifier
) {
    val orderRepairs = remember(repairs, order.id) {
        (repairs ?: emptyList()).filter { it.orderId == order.id && it.isApproved }
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF7ED),
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(R.string.kaariger_hisaab_week_bill_col).uppercase(),
                fontWeight = FontWeight.Bold,
                color = Amber,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 0.6.sp
            )
            PrevRow(
                stringResource(R.string.kaariger_hisaab_outstanding_before),
                formatIndianRupee(opening),
                Amber,
                bold = true
            )
            order.products.forEach { p ->
                PrevRow(
                    "${p.productName} (${p.quantity}×${formatIndianRupee(p.pricePerPiece)})",
                    formatIndianRupee(p.lineTotal)
                )
            }
            if (productsTotal > 0) {
                PrevRow("MAAL", formatIndianRupee(productsTotal))
            }
            order.materialDeductions.forEach { d ->
                PrevRow(
                    stringResource(R.string.kaariger_previous_less, d.label),
                    "−${formatIndianRupee(d.lineTotal)}",
                    Color(0xFFDC2626)
                )
            }
            orderRepairs.forEach { r ->
                PrevRow(
                    stringResource(R.string.kaariger_previous_less, "Repairing - ${r.productName}"),
                    "−${formatIndianRupee(r.totalRepairCost)}",
                    Color(0xFFDC2626)
                )
            }
            if (orderRepairs.isEmpty() && order.repairDeductionTotal > 0) {
                PrevRow(
                    stringResource(
                        R.string.kaariger_previous_less,
                        stringResource(R.string.kaariger_hisaab_repair_label)
                    ),
                    "−${formatIndianRupee(order.repairDeductionTotal)}",
                    Color(0xFFDC2626)
                )
            }
            PrevRow("ADD", formatIndianRupee(add), bold = true)
            if (budget > 0) {
                PrevRow(
                    stringResource(R.string.kaariger_hisaab_kharcha_on_bill),
                    "−${formatIndianRupee(budget)}",
                    Jade
                )
            }
            PrevRow(
                stringResource(R.string.kaariger_hisaab_outstanding_after),
                formatIndianRupee(closing),
                Amber,
                bold = true
            )
        }
    }
}

@Composable
private fun KharchaThatWeekColumn(
    budget: Double,
    paidDisplay: Double,
    box: Double,
    priorOverpay: Double,
    weekPays: List<KaarigerOrderPayment>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = JadeSoft.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, Jade.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.kaariger_hisaab_kharcha_that_week).uppercase(),
                fontWeight = FontWeight.Bold,
                color = Jade,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 0.6.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KharchaStatChip(
                    label = stringResource(R.string.kaariger_hisaab_kharcha_budget),
                    value = formatIndianRupee(budget),
                    modifier = Modifier.weight(1f)
                )
                KharchaStatChip(
                    label = stringResource(R.string.kaariger_hisaab_kharcha_paid_total),
                    value = formatIndianRupee(paidDisplay),
                    modifier = Modifier.weight(1f)
                )
                KharchaStatChip(
                    label = if (box < 0) {
                        stringResource(R.string.kaariger_hisaab_kharcha_extra)
                    } else {
                        stringResource(R.string.kaariger_hisaab_kharcha_left)
                    },
                    value = formatIndianRupee(box),
                    valueColor = if (box < 0) ExtraRed else Amber,
                    modifier = Modifier.weight(1f)
                )
            }
            if (priorOverpay > 0) {
                PrevRow(
                    stringResource(R.string.kaariger_hisaab_prior_overpay),
                    formatIndianRupee(priorOverpay),
                    Jade
                )
            }
            if (weekPays.isEmpty() && priorOverpay <= 0) {
                Text(
                    stringResource(R.string.kaariger_hisaab_kharcha_no_payments),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF065F46)
                )
            } else {
                weekPays.forEach { p ->
                    val whenLabel = listOf(p.date, p.time)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    val who = p.createdBy.takeIf { it.isNotBlank() && it != "—" }
                    val label = listOfNotNull(whenLabel.ifBlank { null }, who)
                        .joinToString(" · ")
                        .ifBlank { stringResource(R.string.kaariger_hisaab_kharcha_paid_total) }
                    PrevRow(label, formatIndianRupee(p.amount), Jade)
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.kaariger_previous_paid_total),
                        fontWeight = FontWeight.Bold,
                        color = Jade
                    )
                    Text(
                        formatIndianRupee(paidDisplay),
                        fontWeight = FontWeight.ExtraBold,
                        color = Jade
                    )
                }
            }
        }
    }
}

@Composable
private fun KharchaStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Jade
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.85f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(value, fontWeight = FontWeight.Bold, color = valueColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PrevRow(
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
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (valueColor == Color.Unspecified) {
                MaterialTheme.colorScheme.onSurface
            } else {
                valueColor
            }
        )
    }
}

private fun isWeekKharchaExcluded(p: KaarigerOrderPayment): Boolean {
    val remarks = (p.remarks ?: "").lowercase()
    if (remarks.contains("carried as credit") || remarks.contains("credit carried")) return true
    return p.orderId == "__opening__" ||
        remarks.contains("opening balance") ||
        remarks.contains("old remaining")
}
