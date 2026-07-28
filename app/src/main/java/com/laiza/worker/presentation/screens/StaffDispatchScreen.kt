package com.laiza.worker.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.laiza.worker.domain.models.EcommercePartner
import com.laiza.worker.domain.models.FinishedProduct
import com.laiza.worker.domain.models.ReturnType
import com.laiza.worker.presentation.components.AppSearchBar
import com.laiza.worker.presentation.components.CustomTextField
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.viewmodels.StoreOperationsViewModel

@Composable
fun StaffDispatchScreen(
    viewModel: StoreOperationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }
    var showPickupDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val storeInventory by viewModel.storeInventory.collectAsState()

    LaunchedEffect(Unit) {
        isRefreshing = true
        viewModel.refreshInventory { isRefreshing = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Pickup") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Return (RTO/DTO)") })
        }

        if (activeTab == 0) {
            DispatchTabContent(
                title = "Handoff to delivery partner",
                subtitle = "Select products given to Flipkart, Myntra, Amazon, Meesho, etc.",
                buttonText = "New Pickup",
                inventoryHint = if (storeInventory.any { it.quantity > 0 }) {
                    "${storeInventory.count { it.quantity > 0 }} product(s) ready to pick up"
                } else {
                    "No stock available. Verify kaariger orders first to add inventory."
                },
                isRefreshing = isRefreshing,
                onAction = {
                    viewModel.refreshInventory {
                        showPickupDialog = true
                    }
                }
            )
        } else {
            DispatchTabContent(
                title = "Product returns",
                subtitle = "Restock items returned via RTO or DTO",
                buttonText = "New Return",
                inventoryHint = if (storeInventory.isNotEmpty()) {
                    "${storeInventory.size} product(s) in catalog"
                } else {
                    "No products in catalog yet."
                },
                isRefreshing = isRefreshing,
                onAction = {
                    viewModel.refreshInventory {
                        showReturnDialog = true
                    }
                }
            )
        }
    }

    if (showPickupDialog) {
        InventoryOperationDialog(
            title = "Record Pickup",
            confirmText = "Confirm Pickup",
            viewModel = viewModel,
            requireStock = true,
            onDismiss = { showPickupDialog = false },
            onConfirm = { product, qty, partner, _, _ ->
                viewModel.recordPickup(
                    product, qty, partner,
                    onSuccess = {
                        showPickupDialog = false
                        Toast.makeText(context, "Pickup recorded successfully", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            },
            validateQuantity = { product, qty -> qty > 0 && qty <= product.quantity }
        )
    }

    if (showReturnDialog) {
        InventoryOperationDialog(
            title = "Record Return",
            confirmText = "Restock Item",
            viewModel = viewModel,
            requireStock = false,
            showReturnType = true,
            onDismiss = { showReturnDialog = false },
            onConfirm = { product, qty, partner, returnType, notes ->
                viewModel.recordReturn(
                    product, qty, partner, returnType ?: ReturnType.RTO, notes,
                    onSuccess = {
                        showReturnDialog = false
                        Toast.makeText(context, "Return recorded and stock updated", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            },
            validateQuantity = { _, qty -> qty > 0 }
        )
    }
}

@Composable
private fun DispatchTabContent(
    title: String,
    subtitle: String,
    buttonText: String,
    inventoryHint: String,
    isRefreshing: Boolean,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text(inventoryHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(
            text = if (isRefreshing) "Loading..." else buttonText,
            onClick = onAction,
            enabled = !isRefreshing,
            isLoading = isRefreshing
        )
    }
}

@Composable
private fun InventoryOperationDialog(
    title: String,
    confirmText: String,
    viewModel: StoreOperationsViewModel,
    requireStock: Boolean,
    showReturnType: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (FinishedProduct, Int, EcommercePartner, ReturnType?, String?) -> Unit,
    validateQuantity: (FinishedProduct, Int) -> Boolean = { _, qty -> qty > 0 }
) {
    val search by viewModel.inventorySearch.collectAsState()
    val products by viewModel.filteredInventory.collectAsState()
    var selectedProduct by remember { mutableStateOf<FinishedProduct?>(null) }
    var quantity by remember { mutableStateOf("") }
    var selectedPartner by remember { mutableStateOf(EcommercePartner.FLIPKART) }
    var selectedReturnType by remember { mutableStateOf(ReturnType.RTO) }
    var notes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val selectableProducts = remember(products, requireStock) {
        if (requireStock) products.filter { it.quantity > 0 }
        else products
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSearchBar(query = search, onQueryChange = viewModel::onInventorySearchChange, placeholder = "Search product or color...")
                if (selectedProduct == null) {
                    if (selectableProducts.isEmpty()) {
                        Text(
                            if (requireStock) {
                                "No products with stock available. Verify kaariger deliveries from the Verify tab to add inventory."
                            } else {
                                "No products found. Verify kaariger deliveries first to create inventory items."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        selectableProducts.forEach { product ->
                            PremiumCard(
                                modifier = Modifier.fillMaxWidth().clickable { selectedProduct = product }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.Bold)
                                        if (product.color.isNotBlank()) {
                                            Text("Color: ${product.color}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Text(
                                        "${product.quantity} pcs",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val p = selectedProduct!!
                    Text("Selected: ${p.name}", fontWeight = FontWeight.Bold)
                    if (p.color.isNotBlank()) Text("Color: ${p.color}")
                    Text("Available: ${p.quantity} pcs")
                    TextButton(onClick = { selectedProduct = null; quantity = "" }) { Text("Change product") }
                    CustomTextField(
                        quantity,
                        { quantity = it },
                        "Quantity",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text("E-commerce Partner", fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EcommercePartner.entries.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { partner ->
                                    FilterChip(
                                        selected = selectedPartner == partner,
                                        onClick = { selectedPartner = partner },
                                        label = { Text(partner.displayName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                    if (showReturnType) {
                        Text("Return Type", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedReturnType == ReturnType.RTO,
                                onClick = { selectedReturnType = ReturnType.RTO },
                                label = { Text("RTO") }
                            )
                            FilterChip(
                                selected = selectedReturnType == ReturnType.DTO,
                                onClick = { selectedReturnType = ReturnType.DTO },
                                label = { Text("DTO") }
                            )
                        }
                        CustomTextField(notes, { notes = it }, "Notes (optional)")
                    }
                }
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = confirmText,
                onClick = {
                    val p = selectedProduct ?: run {
                        validationError = "Please select a product"
                        return@PrimaryButton
                    }
                    val q = quantity.toIntOrNull() ?: run {
                        validationError = "Enter a valid quantity"
                        return@PrimaryButton
                    }
                    if (!validateQuantity(p, q)) {
                        validationError = if (requireStock) {
                            "Enter 1–${p.quantity} pcs (available stock)"
                        } else {
                            "Quantity must be greater than 0"
                        }
                        return@PrimaryButton
                    }
                    validationError = null
                    onConfirm(p, q, selectedPartner, if (showReturnType) selectedReturnType else null, notes.ifBlank { null })
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
