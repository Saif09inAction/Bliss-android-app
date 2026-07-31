package com.laiza.worker.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.domain.models.Employee
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.OrderProductLine
import com.laiza.worker.domain.models.OrderRepair
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.components.CustomTextField
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.components.formatOrderDate
import com.laiza.worker.presentation.viewmodels.OrderViewModel
import kotlin.math.roundToInt

private data class RepairProductOption(
    val order: KaarigerOrder,
    val productName: String,
    val pricePerPiece: Double
)

private fun rupees(amount: Double): String = "₹${amount.roundToInt()}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffRepairingScreen(
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val kaarigers by orderViewModel.kaarigers.collectAsState()
    val kaarigerOrders by orderViewModel.kaarigerOrders.collectAsState()
    val kaarigerRepairs by orderViewModel.kaarigerRepairs.collectAsState()

    var selectedKaariger by remember { mutableStateOf<Employee?>(null) }
    var kaarigerQuery by remember { mutableStateOf("") }
    var kaarigerExpanded by remember { mutableStateOf(false) }

    var selectedProduct by remember { mutableStateOf<RepairProductOption?>(null) }
    var productExpanded by remember { mutableStateOf(false) }

    var qtyText by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedKaariger?.phone) {
        selectedProduct = null
        qtyText = ""
        message = null
        val phone = selectedKaariger?.phone
        if (phone != null) {
            orderViewModel.loadKaarigerData(phone)
        }
    }

    val filteredKaarigers = remember(kaarigers, kaarigerQuery) {
        if (kaarigerQuery.isBlank()) kaarigers
        else kaarigers.filter { it.name.contains(kaarigerQuery, ignoreCase = true) || it.phone.contains(kaarigerQuery) }
    }

    val activeOrders = remember(kaarigerOrders) {
        kaarigerOrders.filter { it.status != OrderStatus.REJECTED }
    }

    val productOptions = remember(activeOrders) {
        activeOrders.flatMap { order ->
            val lines = if (order.products.isNotEmpty()) {
                order.products
            } else {
                val fallbackPrice = order.pricePerPiece
                    ?: (if (order.targetQuantity > 0) order.totalDealAmount / order.targetQuantity else 0.0)
                listOf(OrderProductLine(order.productName, order.targetQuantity, fallbackPrice, 0.0))
            }
            lines.filter { it.productName.isNotBlank() }.map { line ->
                RepairProductOption(order, line.productName, line.pricePerPiece)
            }
        }
    }

    val faultyQty = qtyText.toIntOrNull() ?: 0
    val deduction = faultyQty * (selectedProduct?.pricePerPiece ?: 0.0)
    val currentBalance = selectedProduct?.order?.effectiveDealAmount() ?: 0.0
    val balanceAfter = (currentBalance - deduction).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Repairing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Report faulty / rejected pieces to deduct from a kaariger's hisaab",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Kaariger",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                ExposedDropdownMenuBox(
                    expanded = kaarigerExpanded,
                    onExpandedChange = { kaarigerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedKaariger?.name ?: kaarigerQuery,
                        onValueChange = {
                            kaarigerQuery = it
                            selectedKaariger = null
                            kaarigerExpanded = true
                        },
                        placeholder = { Text("Search kaariger by name or phone") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(kaarigerExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = kaarigerExpanded && filteredKaarigers.isNotEmpty(),
                        onDismissRequest = { kaarigerExpanded = false }
                    ) {
                        filteredKaarigers.forEach { k ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(k.name, fontWeight = FontWeight.SemiBold)
                                        Text(k.phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedKaariger = k
                                    kaarigerQuery = k.name
                                    kaarigerExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (selectedKaariger != null) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Product",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (productOptions.isEmpty()) {
                        Text(
                            text = "No active bill found for this kaariger yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = productExpanded,
                            onExpandedChange = { productExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedProduct?.let { "${it.productName} · ${rupees(it.pricePerPiece)}/pc" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Select product") },
                                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(productExpanded) },
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = productExpanded,
                                onDismissRequest = { productExpanded = false }
                            ) {
                                productOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(option.productName, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    text = "${rupees(option.pricePerPiece)}/pc · bill of ${formatOrderDate(option.order.createdAt)} · balance ${rupees(option.order.effectiveDealAmount())}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedProduct = option
                                            productExpanded = false
                                            message = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedProduct != null) {
            CustomTextField(
                value = qtyText,
                onValueChange = { input -> qtyText = input.filter { it.isDigit() }.take(6) },
                label = "Faulty / rejected quantity",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryRow("Faulty qty × price/pc", "$faultyQty × ${rupees(selectedProduct?.pricePerPiece ?: 0.0)}")
                    SummaryRow("Deduction", "− ${rupees(deduction)}")
                    SummaryRow("Current balance", rupees(currentBalance))
                    SummaryRow("Balance after update", rupees(balanceAfter), emphasize = true)
                }
            }

            message?.let {
                Text(
                    text = it,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            PrimaryButton(
                text = if (saving) "Updating…" else "Update",
                enabled = faultyQty > 0 && !saving,
                isLoading = saving,
                onClick = {
                    val product = selectedProduct ?: return@PrimaryButton
                    saving = true
                    message = null
                    orderViewModel.createRepair(
                        orderId = product.order.id,
                        productName = product.productName,
                        faultyQuantity = faultyQty,
                        faultyPricePerPiece = product.pricePerPiece,
                        onSuccess = {
                            saving = false
                            isError = false
                            message = "Deducted ${rupees(deduction)} from ${selectedKaariger?.name}'s hisaab."
                            qtyText = ""
                        },
                        onError = { err ->
                            saving = false
                            isError = true
                            message = err
                        }
                    )
                }
            )
        }

        if (selectedKaariger != null && kaarigerRepairs.isNotEmpty()) {
            Text(
                text = "Recent deductions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            kaarigerRepairs.take(15).forEach { repair ->
                RepairHistoryRow(repair)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RepairHistoryRow(repair: OrderRepair) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = formatOrderDate(repair.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(repair.productName, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${repair.faultyQuantity} pcs × ${rupees(repair.faultyPricePerPiece)} = − ${rupees(repair.totalRepairCost)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Balance after: ${rupees(repair.dealAfterThisRepair)} · by ${repair.createdBy}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
