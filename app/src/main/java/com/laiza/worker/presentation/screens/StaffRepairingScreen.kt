package com.laiza.worker.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.laiza.worker.domain.models.OrderRepair
import com.laiza.worker.domain.models.RepairStatus
import com.laiza.worker.presentation.components.CustomTextField
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.components.SearchablePickerField
import com.laiza.worker.presentation.components.formatOrderDate
import com.laiza.worker.presentation.viewmodels.OrderViewModel
import com.laiza.worker.presentation.viewmodels.RepairSubmission
import java.util.UUID

private data class RepairProductOption(
    val orderId: String,
    val productName: String,
    val pricePerPiece: Double
)

private data class RepairLineDraft(
    val id: String = UUID.randomUUID().toString(),
    val selectedProduct: RepairProductOption? = null,
    val qtyText: String = "",
    val priceText: String = ""
)

@Composable
fun StaffRepairingScreen(
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val kaarigers by orderViewModel.kaarigers.collectAsState()
    val kaarigerRepairs by orderViewModel.kaarigerRepairs.collectAsState()
    val catalog by orderViewModel.productCatalog.collectAsState()

    var selectedKaariger by remember { mutableStateOf<Employee?>(null) }
    var repairLines by remember { mutableStateOf(listOf(RepairLineDraft())) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedKaariger?.phone) {
        repairLines = listOf(RepairLineDraft())
        message = null
        selectedKaariger?.phone?.let { orderViewModel.loadKaarigerData(it) }
    }

    val kaarigerNames = remember(kaarigers) { kaarigers.map { it.name } }
    val kaarigerByName = remember(kaarigers) { kaarigers.associateBy { it.name } }

    val productOptions = remember(catalog) {
        catalog
            .filter { it.name.isNotBlank() }
            .map { product ->
                RepairProductOption(
                    orderId = RepairStatus.STANDALONE_ORDER_ID,
                    productName = product.name,
                    pricePerPiece = product.price.coerceAtLeast(0.0)
                )
            }
    }

    val productLabels = remember(productOptions) { productOptions.map { it.productName } }
    val productByLabel = remember(productOptions) { productOptions.associateBy { it.productName } }

    val validLines = repairLines.filter {
        it.selectedProduct != null &&
            (it.qtyText.toIntOrNull() ?: 0) > 0 &&
            (it.priceText.toDoubleOrNull() ?: 0.0) >= 0.0
    }

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

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kaariger", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                SearchablePickerField(
                    selected = selectedKaariger?.name.orEmpty(),
                    onSelected = { name ->
                        selectedKaariger = if (name.isBlank()) null else kaarigerByName[name]
                    },
                    options = kaarigerNames,
                    label = "Kaariger",
                    placeholder = "Search name",
                    emptyText = "No kaariger found",
                    optionSubtitle = { name -> kaarigerByName[name]?.phone }
                )
            }
        }

        if (selectedKaariger != null) {
            if (productOptions.isEmpty()) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No catalog products yet",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                repairLines.forEachIndexed { index, line ->
                    key(line.id) {
                        val selectedLabel = line.selectedProduct?.productName.orEmpty()

                        RepairLineRow(
                            index = index,
                            line = line,
                            selectedLabel = selectedLabel,
                            productLabels = productLabels,
                            productByLabel = productByLabel,
                            onProductSelected = { option ->
                                repairLines = repairLines.map { draft ->
                                    if (draft.id != line.id) draft
                                    else draft.copy(
                                        selectedProduct = option,
                                        // Prefill catalog ₹/pc; staff can still edit.
                                        priceText = if (option.pricePerPiece > 0) {
                                            formatCatalogPrice(option.pricePerPiece)
                                        } else {
                                            draft.priceText
                                        }
                                    )
                                }
                            },
                            onProductCleared = {
                                repairLines = repairLines.map { draft ->
                                    if (draft.id == line.id) {
                                        draft.copy(selectedProduct = null, priceText = "")
                                    } else draft
                                }
                            },
                            onQtyChanged = { qty ->
                                repairLines = repairLines.map { draft ->
                                    if (draft.id == line.id) draft.copy(qtyText = qty) else draft
                                }
                            },
                            onPriceChanged = { price ->
                                repairLines = repairLines.map { draft ->
                                    if (draft.id == line.id) draft.copy(priceText = price) else draft
                                }
                            },
                            onRemove = if (repairLines.size > 1) {
                                { repairLines = repairLines.filter { it.id != line.id } }
                            } else null
                        )
                    }
                }

                OutlinedButton(
                    onClick = { repairLines = repairLines + RepairLineDraft() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add product")
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
                        val kaariger = selectedKaariger ?: return@PrimaryButton
                        val submissions = validLines.mapNotNull { line ->
                            val product = line.selectedProduct ?: return@mapNotNull null
                            val qty = line.qtyText.toIntOrNull() ?: return@mapNotNull null
                            val price = line.priceText.toDoubleOrNull() ?: 0.0
                            if (qty <= 0) return@mapNotNull null
                            if (price < 0) return@mapNotNull null
                            RepairSubmission(
                                orderId = product.orderId,
                                productName = product.productName,
                                faultyQuantity = qty,
                                faultyPricePerPiece = price,
                                kaarigerId = kaariger.phone,
                                kaarigerName = kaariger.name
                            )
                        }
                        if (submissions.isEmpty()) {
                            isError = true
                            message = "Add product and quantity"
                            return@PrimaryButton
                        }
                        saving = true
                        message = null
                        orderViewModel.createRepairs(
                            submissions = submissions,
                            onSuccess = {
                                saving = false
                                isError = false
                                message = "Submitted"
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
                text = "Recent",
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

private fun formatCatalogPrice(price: Double): String {
    return if (price % 1.0 == 0.0) price.toInt().toString()
    else price.toString().trimEnd('0').trimEnd('.')
}

@Composable
private fun RepairLineRow(
    index: Int,
    line: RepairLineDraft,
    selectedLabel: String,
    productLabels: List<String>,
    productByLabel: Map<String, RepairProductOption>,
    onProductSelected: (RepairProductOption) -> Unit,
    onProductCleared: () -> Unit,
    onQtyChanged: (String) -> Unit,
    onPriceChanged: (String) -> Unit,
    onRemove: (() -> Unit)?
) {
    val qty = line.qtyText.toIntOrNull() ?: 0
    val price = line.priceText.toDoubleOrNull() ?: 0.0
    val lineTotal = qty * price

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Product ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }

            SearchablePickerField(
                selected = selectedLabel,
                onSelected = { label ->
                    if (label.isBlank()) onProductCleared()
                    else productByLabel[label]?.let(onProductSelected)
                },
                options = productLabels,
                label = "Product",
                placeholder = "Search product",
                emptyText = "No products"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CustomTextField(
                    value = line.qtyText,
                    onValueChange = { input -> onQtyChanged(input.filter { it.isDigit() }.take(6)) },
                    label = "Quantity",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                CustomTextField(
                    value = line.priceText,
                    onValueChange = { input ->
                        onPriceChanged(
                            input.filter { it.isDigit() || it == '.' }
                                .let { raw ->
                                    val parts = raw.split('.')
                                    if (parts.size <= 1) raw.take(8)
                                    else parts[0].take(8) + "." + parts.drop(1).joinToString("").take(2)
                                }
                        )
                    },
                    label = "₹ / pc",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            if (qty > 0 && price > 0) {
                Text(
                    text = "Total: ₹${formatCatalogPrice(lineTotal)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RepairHistoryRow(repair: OrderRepair) {
    val statusLabel = when {
        repair.isPending -> "Pending"
        repair.status == RepairStatus.REJECTED -> "Rejected"
        else -> "Approved"
    }
    val statusColor = when {
        repair.isPending -> Color(0xFFB45309)
        repair.status == RepairStatus.REJECTED -> MaterialTheme.colorScheme.error
        else -> Color(0xFF047857)
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(repair.productName, fontWeight = FontWeight.SemiBold)
                if (repair.faultyQuantity > 0) {
                    Text(
                        "${repair.faultyQuantity} pcs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatOrderDate(repair.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(shape = MaterialTheme.shapes.small, color = statusColor.copy(alpha = 0.12f)) {
                Text(
                    statusLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }
        }
    }
}
