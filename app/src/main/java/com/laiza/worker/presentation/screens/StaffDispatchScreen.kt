package com.laiza.worker.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.domain.models.EcommercePlatform
import com.laiza.worker.domain.models.FinishedProduct
import com.laiza.worker.domain.models.PickupLineItem
import com.laiza.worker.domain.models.PickupRecord
import com.laiza.worker.domain.models.ReturnRecord
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
    var showAllPickups by remember { mutableStateOf(false) }
    var showAllReturns by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val storeInventory by viewModel.storeInventory.collectAsState()
    val allPickups by viewModel.allPickups.collectAsState()
    val allReturns by viewModel.allReturns.collectAsState()

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
            DispatchHistoryTab(
                title = "Handoff to delivery partner",
                subtitle = "Select products → marketplace → courier (BlueDart, Shiprocket…)",
                buttonText = "New Pickup",
                inventoryHint = if (storeInventory.any { it.quantity > 0 }) {
                    "${storeInventory.count { it.quantity > 0 }} product(s) ready to pick up"
                } else {
                    "No stock available. Verify kaariger orders first to add inventory."
                },
                historyTitle = "Pickup history",
                emptyHistory = "No pickups recorded yet",
                isRefreshing = isRefreshing,
                onAction = {
                    viewModel.refreshInventory { showPickupDialog = true }
                },
                recentItems = allPickups.take(3),
                totalCount = allPickups.size,
                onViewAll = { showAllPickups = true }
            ) { record ->
                PickupHistoryCard(record)
            }
        } else {
            DispatchHistoryTab(
                title = "Product returns",
                subtitle = "Restock items returned via RTO or DTO",
                buttonText = "New Return",
                inventoryHint = if (storeInventory.isNotEmpty()) {
                    "${storeInventory.size} product(s) in catalog"
                } else {
                    "No products in catalog yet."
                },
                historyTitle = "Return history",
                emptyHistory = "No returns recorded yet",
                isRefreshing = isRefreshing,
                onAction = {
                    viewModel.refreshInventory { showReturnDialog = true }
                },
                recentItems = allReturns.take(3),
                totalCount = allReturns.size,
                onViewAll = { showAllReturns = true }
            ) { record ->
                ReturnHistoryCard(record)
            }
        }
    }

    if (showAllPickups) {
        FullHistorySheet(
            title = "All pickup history",
            onDismiss = { showAllPickups = false }
        ) {
            if (allPickups.isEmpty()) {
                item {
                    Text(
                        "No pickups recorded yet",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(allPickups, key = { it.id }) { PickupHistoryCard(it) }
            }
        }
    }

    if (showAllReturns) {
        FullHistorySheet(
            title = "All return history",
            onDismiss = { showAllReturns = false }
        ) {
            if (allReturns.isEmpty()) {
                item {
                    Text(
                        "No returns recorded yet",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(allReturns, key = { it.id }) { ReturnHistoryCard(it) }
            }
        }
    }

    if (showPickupDialog) {
        MultiPickupDialog(
            viewModel = viewModel,
            onDismiss = { showPickupDialog = false },
            onConfirm = { items, platform, courier ->
                viewModel.recordPickup(
                    items = items,
                    platform = platform,
                    deliveryPartner = courier,
                    onSuccess = {
                        showPickupDialog = false
                        Toast.makeText(context, "Pickup recorded successfully", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (showReturnDialog) {
        ReturnOperationDialog(
            viewModel = viewModel,
            onDismiss = { showReturnDialog = false },
            onConfirm = { product, qty, platform, courier, returnType, notes ->
                viewModel.recordReturn(
                    product, qty, platform, courier, returnType, notes,
                    onSuccess = {
                        showReturnDialog = false
                        Toast.makeText(context, "Return recorded and stock updated", Toast.LENGTH_SHORT).show()
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
private fun <T> DispatchHistoryTab(
    title: String,
    subtitle: String,
    buttonText: String,
    inventoryHint: String,
    historyTitle: String,
    emptyHistory: String,
    isRefreshing: Boolean,
    onAction: () -> Unit,
    recentItems: List<T>,
    totalCount: Int,
    onViewAll: () -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(inventoryHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton(
            text = if (isRefreshing) "Loading…" else buttonText,
            onClick = onAction,
            enabled = !isRefreshing
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(historyTitle, fontWeight = FontWeight.SemiBold)
            if (totalCount > 3) {
                TextButton(onClick = onViewAll) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View all ($totalCount)")
                }
            }
        }
        if (recentItems.isEmpty()) {
            Text(emptyHistory, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            recentItems.forEach { itemContent(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullHistorySheet(
    title: String,
    onDismiss: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun PickupHistoryCard(record: PickupRecord) {
    val lines = record.lineItems
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(record.productsLabel, fontWeight = FontWeight.Bold)
                        if (lines.size == 1 && lines.first().color.isNotBlank()) {
                            Text(
                                "Color: ${lines.first().color}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (lines.size > 1) {
                            Text(
                                lines.joinToString(" · ") { "${it.productName} ×${it.quantity}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
                Text(
                    "${record.totalQuantity} pcs",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryChip(record.partner)
                if (record.deliveryPartner.isNotBlank()) {
                    HistoryChip(record.deliveryPartner)
                }
                HistoryChip("${record.date} · ${record.time}")
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "By ${record.staffName.ifBlank { "Staff" }}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReturnHistoryCard(record: ReturnRecord) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(record.productName, fontWeight = FontWeight.Bold)
                        if (record.color.isNotBlank()) {
                            Text(
                                "Color: ${record.color}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    "+${record.quantity} pcs",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryChip(record.returnType.displayName)
                HistoryChip(record.partner)
                if (record.deliveryPartner.isNotBlank()) {
                    HistoryChip(record.deliveryPartner)
                }
                HistoryChip("${record.date} · ${record.time}")
            }
            record.notes?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "By ${record.staffName.ifBlank { "Staff" }}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiPickupDialog(
    viewModel: StoreOperationsViewModel,
    onDismiss: () -> Unit,
    onConfirm: (List<PickupLineItem>, String, String) -> Unit
) {
    val context = LocalContext.current
    val search by viewModel.inventorySearch.collectAsState()
    val products by viewModel.filteredInventory.collectAsState()
    val partners by viewModel.deliveryPartners.collectAsState()

    // productId -> qty string
    var selectedQty by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var platform by remember { mutableStateOf(EcommercePlatform.AMAZON) }
    var platformExpanded by remember { mutableStateOf(false) }
    var courier by remember { mutableStateOf("") }
    var courierExpanded by remember { mutableStateOf(false) }
    var courierQuery by remember { mutableStateOf("") }
    var showAddCourier by remember { mutableStateOf(false) }
    var newCourierName by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    val selectable = remember(products) { products.filter { it.quantity > 0 } }
    val filteredCouriers = remember(partners, courierQuery) {
        if (courierQuery.isBlank()) partners
        else partners.filter { it.name.contains(courierQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        title = { Text("Record Pickup", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("1. Select products (tap to add)", fontWeight = FontWeight.SemiBold)
                AppSearchBar(
                    query = search,
                    onQueryChange = viewModel::onInventorySearchChange,
                    placeholder = "Search product or color..."
                )
                if (selectable.isEmpty()) {
                    Text(
                        "No products with stock. Verify kaariger deliveries first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    selectable.forEach { product ->
                        val selected = selectedQty.containsKey(product.id)
                        val qtyValue = selectedQty[product.id] ?: "1"
                        Surface(
                            onClick = {
                                selectedQty = if (selected) {
                                    selectedQty - product.id
                                } else {
                                    selectedQty + (product.id to "1")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (selected) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.Inventory
                                        },
                                        contentDescription = null,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (product.color.isNotBlank()) {
                                            Text(
                                                text = "Color: ${product.color}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${product.quantity} avail",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (selected) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = qtyValue,
                                        onValueChange = { v ->
                                            selectedQty = selectedQty + (
                                                product.id to v.filter { ch -> ch.isDigit() }
                                            )
                                        },
                                        label = { Text("Qty (max ${product.quantity})") },
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedQty.isNotEmpty()) {
                    HorizontalDivider()
                    Text("2. Marketplace", fontWeight = FontWeight.SemiBold)
                    ExposedDropdownMenuBox(
                        expanded = platformExpanded,
                        onExpandedChange = { platformExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = platform,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Platform") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(platformExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = platformExpanded,
                            onDismissRequest = { platformExpanded = false }
                        ) {
                            EcommercePlatform.DEFAULTS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        platform = option
                                        platformExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text("3. Delivery partner", fontWeight = FontWeight.SemiBold)
                    ExposedDropdownMenuBox(
                        expanded = courierExpanded,
                        onExpandedChange = { courierExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (courierExpanded) courierQuery else courier.ifBlank { courierQuery },
                            onValueChange = {
                                courierQuery = it
                                courierExpanded = true
                            },
                            label = { Text("Search courier") },
                            placeholder = { Text("BlueDart, Shiprocket…") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(courierExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = courierExpanded,
                            onDismissRequest = { courierExpanded = false }
                        ) {
                            filteredCouriers.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        courier = p.name
                                        courierQuery = p.name
                                        courierExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add new partner…")
                                    }
                                },
                                onClick = {
                                    courierExpanded = false
                                    newCourierName = courierQuery.trim()
                                    showAddCourier = true
                                }
                            )
                        }
                    }
                    if (courier.isNotBlank()) {
                        Text(
                            "Selected: $courier",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (submitting) "Saving…" else "Confirm Pickup",
                onClick = {
                    if (submitting) return@PrimaryButton
                    val productById = selectable.associateBy { it.id }
                    val lines = mutableListOf<PickupLineItem>()
                    for ((id, qtyStr) in selectedQty) {
                        val product = productById[id] ?: continue
                        val q = qtyStr.toIntOrNull() ?: 0
                        if (q <= 0 || q > product.quantity) {
                            validationError = "Enter 1–${product.quantity} for ${product.name}"
                            return@PrimaryButton
                        }
                        lines += PickupLineItem(
                            productId = product.id,
                            productName = product.name,
                            color = product.color,
                            quantity = q
                        )
                    }
                    if (lines.isEmpty()) {
                        validationError = "Select at least one product"
                        return@PrimaryButton
                    }
                    if (platform.isBlank()) {
                        validationError = "Select a marketplace"
                        return@PrimaryButton
                    }
                    val courierName = courier.ifBlank { courierQuery }.trim()
                    if (courierName.isBlank()) {
                        validationError = "Select or add a delivery partner"
                        return@PrimaryButton
                    }
                    validationError = null
                    submitting = true
                    onConfirm(lines, platform, courierName)
                },
                enabled = !submitting
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("Cancel") }
        }
    )

    if (showAddCourier) {
        AlertDialog(
            onDismissRequest = { showAddCourier = false },
            title = { Text("Add delivery partner") },
            text = {
                CustomTextField(
                    value = newCourierName,
                    onValueChange = { newCourierName = it },
                    label = "Partner name"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addDeliveryPartner(
                            name = newCourierName,
                            onSuccess = { added ->
                                courier = added.name
                                courierQuery = added.name
                                showAddCourier = false
                                Toast.makeText(context, "${added.name} added", Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCourier = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnOperationDialog(
    viewModel: StoreOperationsViewModel,
    onDismiss: () -> Unit,
    onConfirm: (FinishedProduct, Int, String, String, ReturnType, String?) -> Unit
) {
    val search by viewModel.inventorySearch.collectAsState()
    val products by viewModel.filteredInventory.collectAsState()
    val partners by viewModel.deliveryPartners.collectAsState()

    var selectedProduct by remember { mutableStateOf<FinishedProduct?>(null) }
    var quantity by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf(EcommercePlatform.FLIPKART) }
    var platformExpanded by remember { mutableStateOf(false) }
    var courier by remember { mutableStateOf("") }
    var courierExpanded by remember { mutableStateOf(false) }
    var courierQuery by remember { mutableStateOf("") }
    var selectedReturnType by remember { mutableStateOf(ReturnType.RTO) }
    var notes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showAddCourier by remember { mutableStateOf(false) }
    var newCourierName by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filteredCouriers = remember(partners, courierQuery) {
        if (courierQuery.isBlank()) partners
        else partners.filter { it.name.contains(courierQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Return", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSearchBar(query = search, onQueryChange = viewModel::onInventorySearchChange, placeholder = "Search product or color...")
                if (selectedProduct == null) {
                    if (products.isEmpty()) {
                        Text(
                            "No products found. Verify kaariger deliveries first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        products.forEach { product ->
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
                    TextButton(onClick = { selectedProduct = null; quantity = "" }) { Text("Change product") }
                    CustomTextField(
                        quantity,
                        { quantity = it },
                        "Quantity",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text("Marketplace", fontWeight = FontWeight.SemiBold)
                    ExposedDropdownMenuBox(expanded = platformExpanded, onExpandedChange = { platformExpanded = it }) {
                        OutlinedTextField(
                            value = platform,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Platform") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(platformExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = platformExpanded, onDismissRequest = { platformExpanded = false }) {
                            EcommercePlatform.DEFAULTS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { platform = option; platformExpanded = false }
                                )
                            }
                        }
                    }

                    Text("Delivery partner", fontWeight = FontWeight.SemiBold)
                    ExposedDropdownMenuBox(expanded = courierExpanded, onExpandedChange = { courierExpanded = it }) {
                        OutlinedTextField(
                            value = if (courierExpanded) courierQuery else courier.ifBlank { courierQuery },
                            onValueChange = { courierQuery = it; courierExpanded = true },
                            label = { Text("Search courier") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(courierExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = courierExpanded, onDismissRequest = { courierExpanded = false }) {
                            filteredCouriers.forEach { partner ->
                                DropdownMenuItem(
                                    text = { Text(partner.name) },
                                    onClick = {
                                        courier = partner.name
                                        courierQuery = partner.name
                                        courierExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Add new partner…") },
                                onClick = {
                                    courierExpanded = false
                                    newCourierName = courierQuery.trim()
                                    showAddCourier = true
                                }
                            )
                        }
                    }

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
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Restock Item",
                onClick = {
                    val p = selectedProduct ?: run {
                        validationError = "Please select a product"
                        return@PrimaryButton
                    }
                    val q = quantity.toIntOrNull() ?: run {
                        validationError = "Enter a valid quantity"
                        return@PrimaryButton
                    }
                    if (q <= 0) {
                        validationError = "Quantity must be greater than 0"
                        return@PrimaryButton
                    }
                    val courierName = courier.ifBlank { courierQuery }.trim()
                    validationError = null
                    onConfirm(p, q, platform, courierName, selectedReturnType, notes.ifBlank { null })
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showAddCourier) {
        AlertDialog(
            onDismissRequest = { showAddCourier = false },
            title = { Text("Add delivery partner") },
            text = {
                CustomTextField(
                    value = newCourierName,
                    onValueChange = { newCourierName = it },
                    label = "Partner name"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addDeliveryPartner(
                            name = newCourierName,
                            onSuccess = { added ->
                                courier = added.name
                                courierQuery = added.name
                                showAddCourier = false
                                Toast.makeText(context, "${added.name} added", Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCourier = false }) { Text("Cancel") }
            }
        )
    }
}
