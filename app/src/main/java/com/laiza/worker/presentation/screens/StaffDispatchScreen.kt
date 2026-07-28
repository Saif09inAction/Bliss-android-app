package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var activeTab by remember { mutableIntStateOf(0) }
    var showPickupDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }

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
                onAction = { showPickupDialog = true }
            )
        } else {
            DispatchTabContent(
                title = "Product returns",
                subtitle = "Restock items returned via RTO or DTO",
                buttonText = "New Return",
                onAction = { showReturnDialog = true }
            )
        }
    }

    if (showPickupDialog) {
        InventoryOperationDialog(
            title = "Record Pickup",
            confirmText = "Confirm Pickup",
            viewModel = viewModel,
            onDismiss = { showPickupDialog = false },
            onConfirm = { product, qty, partner, _, notes ->
                viewModel.recordPickup(product, qty, partner,
                    onSuccess = { showPickupDialog = false },
                    onError = {}
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
            showReturnType = true,
            onDismiss = { showReturnDialog = false },
            onConfirm = { product, qty, partner, returnType, notes ->
                viewModel.recordReturn(product, qty, partner, returnType ?: ReturnType.RTO, notes,
                    onSuccess = { showReturnDialog = false },
                    onError = {}
                )
            },
            validateQuantity = { product, qty -> qty > 0 }
        )
    }
}

@Composable
private fun DispatchTabContent(
    title: String,
    subtitle: String,
    buttonText: String,
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
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = buttonText, onClick = onAction)
    }
}

@Composable
private fun InventoryOperationDialog(
    title: String,
    confirmText: String,
    viewModel: StoreOperationsViewModel,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSearchBar(query = search, onQueryChange = viewModel::onInventorySearchChange, placeholder = "Search product or color...")
                if (selectedProduct == null) {
                    products.filter { it.quantity > 0 }.forEach { product ->
                        PremiumCard(
                            modifier = Modifier.fillMaxWidth().clickable { selectedProduct = product }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    if (product.color.isNotBlank()) Text("Color: ${product.color}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("${product.quantity} pcs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    val p = selectedProduct!!
                    Text("Selected: ${p.name}", fontWeight = FontWeight.Bold)
                    if (p.color.isNotBlank()) Text("Color: ${p.color}")
                    Text("Available: ${p.quantity} pcs")
                    TextButton(onClick = { selectedProduct = null }) { Text("Change product") }
                    CustomTextField(quantity, { quantity = it }, "Quantity", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
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
                            FilterChip(selected = selectedReturnType == ReturnType.RTO, onClick = { selectedReturnType = ReturnType.RTO }, label = { Text("RTO") })
                            FilterChip(selected = selectedReturnType == ReturnType.DTO, onClick = { selectedReturnType = ReturnType.DTO }, label = { Text("DTO") })
                        }
                        CustomTextField(notes, { notes = it }, "Notes (optional)")
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = confirmText,
                onClick = {
                    val p = selectedProduct ?: return@PrimaryButton
                    val q = quantity.toIntOrNull() ?: return@PrimaryButton
                    if (validateQuantity(p, q)) {
                        onConfirm(p, q, selectedPartner, if (showReturnType) selectedReturnType else null, notes.ifBlank { null })
                    }
                }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
