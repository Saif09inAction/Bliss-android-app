package com.laiza.worker.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.domain.models.FinishedProduct
import com.laiza.worker.domain.models.ProductColors
import com.laiza.worker.presentation.components.AppSearchBar
import com.laiza.worker.presentation.components.CustomTextField
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.viewmodels.InventoryViewModel

private data class SkuGroup(
    val displayName: String,
    val totalQty: Int,
    val variants: List<FinishedProduct>
)

@Composable
fun StoreInventoryScreen(
    canAdd: Boolean = true,
    canDelete: Boolean = false,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val products by viewModel.filteredFinishedProducts.collectAsState()
    val search by viewModel.productSearch.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedSku by remember { mutableStateOf<SkuGroup?>(null) }

    LaunchedEffect(Unit) {
        isRefreshing = true
        viewModel.refreshFinishedProducts { isRefreshing = false }
    }

    val skuGroups = remember(products) {
        products
            .groupBy { ProductColors.normalizeSku(it.name) }
            .map { (_, list) ->
                val byColor = list
                    .groupBy { it.color.trim().lowercase() }
                    .map { (_, colorList) ->
                        colorList.reduce { a, b ->
                            a.copy(quantity = a.quantity + b.quantity)
                        }
                    }
                    .sortedBy { it.color.lowercase() }
                SkuGroup(
                    displayName = list.first().name,
                    totalQty = byColor.sumOf { it.quantity },
                    variants = byColor
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Store Inventory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap a SKU to see colour breakdown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canAdd) {
                FilledTonalButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        AppSearchBar(
            query = search,
            onQueryChange = viewModel::onProductSearchChange,
            placeholder = "Search product or color..."
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isRefreshing && products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (skuGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No inventory items yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(skuGroups, key = { ProductColors.normalizeSku(it.displayName) }) { group ->
                    SkuInventoryCard(group = group, onClick = { selectedSku = group })
                }
            }
        }
    }

    selectedSku?.let { group ->
        AlertDialog(
            onDismissRequest = { selectedSku = null },
            title = { Text(group.displayName, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Total ${group.totalQty} pcs",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider()
                    group.variants.forEach { v ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                v.color.ifBlank { "No colour" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${v.quantity} pcs",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSku = null }) { Text("Close") }
            }
        )
    }

    if (showAddDialog) {
        AddInventoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, qty, price ->
                viewModel.addManualInventory(
                    name = name,
                    color = color,
                    quantity = qty,
                    unitPrice = price,
                    onSuccess = {
                        showAddDialog = false
                        Toast.makeText(context, "Inventory added", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
private fun SkuInventoryCard(group: SkuGroup, onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${group.variants.size} colour${if (group.variants.size == 1) "" else "s"} · tap for breakdown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${group.totalQty} pcs",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun AddInventoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: String, qty: Int, price: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add inventory", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Same SKU name (any capitalisation) + colour merges into one stock line.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CustomTextField(name, { name = it }, "Product / SKU name")
                CustomTextField(color, { color = it }, "Color (optional)")
                CustomTextField(
                    quantity,
                    { quantity = it },
                    "Quantity",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
                CustomTextField(
                    unitPrice,
                    { unitPrice = it },
                    "Unit price ₹ (optional)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Add stock",
                onClick = {
                    val qty = quantity.toIntOrNull()
                    if (name.trim().isEmpty()) {
                        error = "Enter product name"
                        return@PrimaryButton
                    }
                    if (qty == null || qty <= 0) {
                        error = "Enter a valid quantity"
                        return@PrimaryButton
                    }
                    error = null
                    onConfirm(name.trim(), color.trim(), qty, unitPrice.toDoubleOrNull() ?: 0.0)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
