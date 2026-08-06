package com.laiza.worker.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.domain.models.Employee
import com.laiza.worker.domain.models.OrderProductLine
import com.laiza.worker.domain.models.OrderRepair
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.domain.models.RepairStatus
import com.laiza.worker.presentation.components.CustomTextField
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.components.formatOrderDate
import com.laiza.worker.presentation.viewmodels.OrderViewModel
import com.laiza.worker.presentation.viewmodels.RepairSubmission

private data class RepairProductOption(
    val orderId: String,
    val productName: String,
    val pricePerPiece: Double,
    val fromCatalog: Boolean,
    val billLabel: String? = null
)

private data class RepairLineDraft(
    val selectedProduct: RepairProductOption? = null,
    val qtyText: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffRepairingScreen(
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val kaarigers by orderViewModel.kaarigers.collectAsState()
    val kaarigerOrders by orderViewModel.kaarigerOrders.collectAsState()
    val kaarigerRepairs by orderViewModel.kaarigerRepairs.collectAsState()
    val catalogNames by orderViewModel.productCatalogNames.collectAsState()

    var selectedKaariger by remember { mutableStateOf<Employee?>(null) }
    var kaarigerQuery by remember { mutableStateOf("") }
    var kaarigerExpanded by remember { mutableStateOf(false) }

    var repairLines by remember { mutableStateOf(listOf(RepairLineDraft())) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedKaariger?.phone) {
        repairLines = listOf(RepairLineDraft())
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

    val productOptions = remember(activeOrders, catalogNames) {
        val fromBills = activeOrders.flatMap { order ->
            val lines = if (order.products.isNotEmpty()) {
                order.products
            } else {
                val fallbackPrice = order.pricePerPiece
                    ?: (if (order.targetQuantity > 0) order.totalDealAmount / order.targetQuantity else 0.0)
                listOf(OrderProductLine(order.productName, order.targetQuantity, fallbackPrice, 0.0))
            }
            lines.filter { it.productName.isNotBlank() }.map { line ->
                RepairProductOption(
                    orderId = order.id,
                    productName = line.productName,
                    pricePerPiece = line.pricePerPiece,
                    fromCatalog = false,
                    billLabel = "Bill · ${formatOrderDate(order.createdAt)}"
                )
            }
        }
        val billNames = fromBills.map { it.productName.lowercase() }.toSet()
        val fromCatalog = catalogNames
            .filter { it.lowercase() !in billNames }
            .map { name ->
                RepairProductOption(
                    orderId = RepairStatus.STANDALONE_ORDER_ID,
                    productName = name,
                    pricePerPiece = 0.0,
                    fromCatalog = true,
                    billLabel = "Catalog · no bill"
                )
            }
        fromBills + fromCatalog
    }

    val validLines = repairLines.filter { it.selectedProduct != null && (it.qtyText.toIntOrNull() ?: 0) > 0 }

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
            text = "Kaariger + product + qty. Price is set by admin on approval. Works even without a bill.",
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
            if (productOptions.isEmpty()) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No products found. Add products in Catalog (admin), then try again.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (activeOrders.isEmpty()) {
                    Text(
                        text = "No bill yet — pick a catalog product. Admin will set ₹/pc on approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB45309)
                    )
                }

                repairLines.forEachIndexed { index, line ->
                    RepairLineRow(
                        index = index,
                        line = line,
                        productOptions = productOptions,
                        onProductSelected = { option ->
                            repairLines = repairLines.toMutableList().also {
                                it[index] = it[index].copy(selectedProduct = option)
                            }
                        },
                        onQtyChanged = { qty ->
                            repairLines = repairLines.toMutableList().also {
                                it[index] = it[index].copy(qtyText = qty)
                            }
                        },
                        onRemove = if (repairLines.size > 1) {
                            {
                                repairLines = repairLines.toMutableList().also { it.removeAt(index) }
                            }
                        } else null
                    )
                }

                OutlinedButton(
                    onClick = { repairLines = repairLines + RepairLineDraft() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add more product")
                }

                message?.let {
                    Text(
                        text = it,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                PrimaryButton(
                    text = if (saving) "Submitting…" else "Submit",
                    enabled = validLines.isNotEmpty() && !saving,
                    isLoading = saving,
                    onClick = {
                        saving = true
                        message = null
                        val kaariger = selectedKaariger!!
                        val submissions = validLines.map { line ->
                            val product = line.selectedProduct!!
                            RepairSubmission(
                                orderId = product.orderId,
                                productName = product.productName,
                                faultyQuantity = line.qtyText.toInt(),
                                faultyPricePerPiece = product.pricePerPiece,
                                kaarigerId = kaariger.phone,
                                kaarigerName = kaariger.name
                            )
                        }
                        orderViewModel.createRepairs(
                            submissions = submissions,
                            onSuccess = {
                                saving = false
                                isError = false
                                val count = submissions.size
                                message = "Sent $count product${if (count == 1) "" else "s"} for admin approval."
                                repairLines = listOf(RepairLineDraft())
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepairLineRow(
    index: Int,
    line: RepairLineDraft,
    productOptions: List<RepairProductOption>,
    onProductSelected: (RepairProductOption) -> Unit,
    onQtyChanged: (String) -> Unit,
    onRemove: (() -> Unit)?
) {
    var productExpanded by remember { mutableStateOf(false) }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Product ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove product")
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = productExpanded,
                onExpandedChange = { productExpanded = it }
            ) {
                OutlinedTextField(
                    value = line.selectedProduct?.productName ?: "",
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
                                        text = option.billLabel ?: if (option.fromCatalog) "Catalog" else "Bill",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onProductSelected(option)
                                productExpanded = false
                            }
                        )
                    }
                }
            }

            CustomTextField(
                value = line.qtyText,
                onValueChange = { input -> onQtyChanged(input.filter { it.isDigit() }.take(6)) },
                label = "Faulty / rejected quantity",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
private fun RepairHistoryRow(repair: OrderRepair) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = when {
                        repair.isPending -> "Pending approval"
                        repair.status == RepairStatus.REJECTED -> "Rejected"
                        else -> "Approved"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        repair.isPending -> Color(0xFFB45309)
                        repair.status == RepairStatus.REJECTED -> MaterialTheme.colorScheme.error
                        else -> Color(0xFF047857)
                    }
                )
            }
            Text(repair.productName, fontWeight = FontWeight.SemiBold)
            if (repair.faultyQuantity > 0) {
                Text(
                    text = "${repair.faultyQuantity} pcs" +
                        if (repair.isStandalone) " · no bill" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatOrderDate(repair.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
