package com.laiza.worker.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.History
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
import com.laiza.worker.domain.models.PickupRecord
import com.laiza.worker.domain.models.PlatformDeliveryPartners
import com.laiza.worker.domain.models.ReturnRecord
import com.laiza.worker.domain.models.ReturnType
import com.laiza.worker.presentation.components.CustomTextField
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.SearchablePickerField
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
    val allPickups by viewModel.allPickups.collectAsState()
    val allReturns by viewModel.allReturns.collectAsState()

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
                title = "Pickup",
                subtitle = null,
                buttonText = "New Pickup",
                historyTitle = "Pickup history",
                emptyHistory = "No pickups recorded yet",
                onAction = { showPickupDialog = true },
                recentItems = allPickups.take(3),
                totalCount = allPickups.size,
                onViewAll = { showAllPickups = true }
            ) { record ->
                PickupHistoryCard(record)
            }
        } else {
            DispatchHistoryTab(
                title = "Return",
                subtitle = null,
                buttonText = "New Return",
                historyTitle = "Return history",
                emptyHistory = "No returns recorded yet",
                onAction = { showReturnDialog = true },
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
        DispatchDetailsDialog(
            viewModel = viewModel,
            title = "Record Pickup",
            confirmText = "Confirm Pickup",
            defaultPlatform = EcommercePlatform.AMAZON,
            onDismiss = { showPickupDialog = false },
            onConfirm = { clarisQty, blissQty, platform, courier ->
                viewModel.recordPickup(
                    clarisQuantity = clarisQty,
                    blissQuantity = blissQty,
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
            onConfirm = { clarisQty, blissQty, platform, courier, returnType, notes ->
                viewModel.recordReturn(
                    clarisQty, blissQty, platform, courier, returnType, notes,
                    onSuccess = {
                        showReturnDialog = false
                        Toast.makeText(context, "Return recorded successfully", Toast.LENGTH_SHORT).show()
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
    subtitle: String? = null,
    buttonText: String,
    historyTitle: String,
    emptyHistory: String,
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
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrimaryButton(text = buttonText, onClick = onAction)
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
            Text(
                record.qtyBreakdownLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryChip(record.partner)
                if (record.deliveryPartner.isNotBlank()) {
                    HistoryChip(record.deliveryPartner)
                }
                HistoryChip(com.laiza.worker.core.utils.DateFormatter.formatStoredDateTime(record.date, record.time))
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
                        Text(record.productName.ifBlank { "Return" }, fontWeight = FontWeight.Bold)
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
                    "+${record.totalQuantity} pcs",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                record.qtyBreakdownLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryChip(record.returnType.displayName)
                HistoryChip(record.partner)
                if (record.deliveryPartner.isNotBlank()) {
                    HistoryChip(record.deliveryPartner)
                }
                HistoryChip(com.laiza.worker.core.utils.DateFormatter.formatStoredDateTime(record.date, record.time))
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

/**
 * Shared Quantity → Marketplace → Delivery-partner form used for Pickup.
 * The delivery-partner dropdown only shows couriers relevant to the chosen
 * marketplace (e.g. Amazon only shows Amazon's couriers, Flipkart only shows
 * Flipkart's, and so on) — see [PlatformDeliveryPartners].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DispatchDetailsDialog(
    viewModel: StoreOperationsViewModel,
    title: String,
    confirmText: String,
    defaultPlatform: String,
    onDismiss: () -> Unit,
    onConfirm: (clarisQty: Int, blissQty: Int, platform: String, courier: String) -> Unit
) {
    val context = LocalContext.current
    val partners by viewModel.deliveryPartners.collectAsState()

    var clarisQty by remember { mutableStateOf("") }
    var blissQty by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf(defaultPlatform) }
    var courier by remember { mutableStateOf("") }
    var platformExpanded by remember { mutableStateOf(false) }
    var showAddCourier by remember { mutableStateOf(false) }
    var newCourierName by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    val relevantCouriers = remember(partners, platform) {
        partners.filter { PlatformDeliveryPartners.isRelevant(it.name, platform) }
    }
    val courierNames = remember(relevantCouriers) { relevantCouriers.map { it.name } }

    // Drop a previously picked courier if it isn't relevant to the newly selected platform.
    LaunchedEffect(platform) {
        if (courier.isNotBlank() && courierNames.none { it.equals(courier, ignoreCase = true) }) {
            courier = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Quantity", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CustomTextField(
                        value = clarisQty,
                        onValueChange = { v -> clarisQty = v.filter { ch -> ch.isDigit() } },
                        label = "Claris qty",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    CustomTextField(
                        value = blissQty,
                        onValueChange = { v -> blissQty = v.filter { ch -> ch.isDigit() } },
                        label = "Bliss qty",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                val totalPreview = (clarisQty.toIntOrNull() ?: 0) + (blissQty.toIntOrNull() ?: 0)
                if (totalPreview > 0) {
                    Text(
                        "Total: $totalPreview pcs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text("Company", fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = platformExpanded,
                    onExpandedChange = { platformExpanded = it }
                ) {
                    OutlinedTextField(
                        value = platform,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Company") },
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

                Text("Delivery partner", fontWeight = FontWeight.SemiBold)
                SearchablePickerField(
                    selected = courier,
                    onSelected = { courier = it },
                    options = courierNames,
                    label = "Courier",
                    placeholder = "Search or pick courier",
                    emptyText = "No couriers for $platform",
                    addNewLabel = "+ Add new partner",
                    onAddNew = {
                        newCourierName = ""
                        showAddCourier = true
                    }
                )

                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (submitting) "Saving…" else confirmText,
                onClick = {
                    if (submitting) return@PrimaryButton
                    val claris = clarisQty.toIntOrNull() ?: 0
                    val bliss = blissQty.toIntOrNull() ?: 0
                    if (claris + bliss <= 0) {
                        validationError = "Enter Claris and/or Bliss quantity"
                        return@PrimaryButton
                    }
                    if (platform.isBlank()) {
                        validationError = "Select a company"
                        return@PrimaryButton
                    }
                    if (courier.isBlank()) {
                        validationError = "Select a delivery partner"
                        return@PrimaryButton
                    }
                    validationError = null
                    submitting = true
                    onConfirm(claris, bliss, platform, courier)
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
    onConfirm: (clarisQty: Int, blissQty: Int, platform: String, courier: String, returnType: ReturnType, notes: String?) -> Unit
) {
    val context = LocalContext.current
    val partners by viewModel.deliveryPartners.collectAsState()

    var clarisQty by remember { mutableStateOf("") }
    var blissQty by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf(EcommercePlatform.FLIPKART) }
    var platformExpanded by remember { mutableStateOf(false) }
    var courier by remember { mutableStateOf("") }
    var selectedReturnType by remember { mutableStateOf(ReturnType.RTO) }
    var notes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showAddCourier by remember { mutableStateOf(false) }
    var newCourierName by remember { mutableStateOf("") }

    val relevantCouriers = remember(partners, platform) {
        partners.filter { PlatformDeliveryPartners.isRelevant(it.name, platform) }
    }
    val courierNames = remember(relevantCouriers) { relevantCouriers.map { it.name } }

    LaunchedEffect(platform) {
        if (courier.isNotBlank() && courierNames.none { it.equals(courier, ignoreCase = true) }) {
            courier = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        title = { Text("Record Return", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Quantity", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CustomTextField(
                        value = clarisQty,
                        onValueChange = { v -> clarisQty = v.filter { ch -> ch.isDigit() } },
                        label = "Claris qty",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    CustomTextField(
                        value = blissQty,
                        onValueChange = { v -> blissQty = v.filter { ch -> ch.isDigit() } },
                        label = "Bliss qty",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                val returnTotal = (clarisQty.toIntOrNull() ?: 0) + (blissQty.toIntOrNull() ?: 0)
                if (returnTotal > 0) {
                    Text(
                        "Total: $returnTotal pcs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text("Company", fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(expanded = platformExpanded, onExpandedChange = { platformExpanded = it }) {
                    OutlinedTextField(
                        value = platform,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Company") },
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
                SearchablePickerField(
                    selected = courier,
                    onSelected = { courier = it },
                    options = courierNames,
                    label = "Courier",
                    placeholder = "Search or pick courier",
                    emptyText = "No couriers for $platform",
                    addNewLabel = "+ Add new partner",
                    onAddNew = {
                        newCourierName = ""
                        showAddCourier = true
                    }
                )

                Text("Return type", fontWeight = FontWeight.SemiBold)
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

                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Save Return",
                onClick = {
                    val claris = clarisQty.toIntOrNull() ?: 0
                    val bliss = blissQty.toIntOrNull() ?: 0
                    if (claris + bliss <= 0) {
                        validationError = "Enter Claris and/or Bliss quantity"
                        return@PrimaryButton
                    }
                    if (platform.isBlank()) {
                        validationError = "Select a company"
                        return@PrimaryButton
                    }
                    if (courier.isBlank()) {
                        validationError = "Select a delivery partner"
                        return@PrimaryButton
                    }
                    validationError = null
                    onConfirm(claris, bliss, platform, courier, selectedReturnType, notes.ifBlank { null })
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
