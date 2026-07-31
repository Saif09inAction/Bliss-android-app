package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.R
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.OrderMaterial
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.domain.models.OrderReceiptData
import com.laiza.worker.domain.models.buildOrderReceiptData
import com.laiza.worker.presentation.components.KaarigerOrderDetailSheet
import com.laiza.worker.presentation.components.OrderProgressChips
import com.laiza.worker.presentation.components.OrderReceiptDialog
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.components.formatOrderDate
import com.laiza.worker.presentation.components.kaarigerDisplayName
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

@Composable
fun KaarigerOrdersScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val session by authViewModel.userSession.collectAsState()
    val orders by orderViewModel.kaarigerOrders.collectAsState()
    val payments by orderViewModel.kaarigerPayments.collectAsState()
    var search by remember { mutableStateOf("") }
    var detailOrder by remember { mutableStateOf<KaarigerOrder?>(null) }
    var materialOrder by remember { mutableStateOf<KaarigerOrder?>(null) }
    var receiptData by remember { mutableStateOf<OrderReceiptData?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session?.phone) {
        session?.phone?.let { orderViewModel.loadKaarigerData(it) }
    }

    val filtered = remember(orders, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) orders
        else orders.filter {
            it.productName.lowercase().contains(q) ||
                it.color.lowercase().contains(q)
        }
    }

    var activeTab by remember { mutableIntStateOf(0) }
    val tabOrders = remember(filtered, activeTab) {
        when (activeTab) {
            0 -> filtered.filter { it.status != OrderStatus.COMPLETED }
            else -> filtered.filter { it.status == OrderStatus.COMPLETED }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(stringResource(R.string.kaariger_tab_pending)) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(stringResource(R.string.kaariger_tab_completed)) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.kaariger_search_orders)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (tabOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    when {
                        orders.isEmpty() -> stringResource(R.string.kaariger_no_orders)
                        search.isNotBlank() -> stringResource(R.string.kaariger_no_matching_orders)
                        activeTab == 0 -> stringResource(R.string.kaariger_no_pending_orders)
                        else -> stringResource(R.string.kaariger_no_completed_orders)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tabOrders, key = { it.id }) { order ->
                    KaarigerOrderCard(
                        order = order,
                        onClick = { detailOrder = order },
                        onReportMaterials = { materialOrder = order }
                    )
                }
            }
        }
    }

    detailOrder?.let { order ->
        KaarigerOrderDetailSheet(
            order = order,
            onDismiss = { detailOrder = null },
            onReportMaterials = {
                detailOrder = null
                materialOrder = order
            },
            onViewReceipt = if (order.status == OrderStatus.COMPLETED && order.materialUsageReported) {
                {
                    detailOrder = null
                    receiptData = buildOrderReceiptData(order, payments)
                }
            } else null
        )
    }

    materialOrder?.let { order ->
        MaterialUsageDialog(
            order = order,
            onDismiss = { materialOrder = null },
            onSubmit = { materials ->
                orderViewModel.submitMaterialUsage(order.id, materials,
                    onSuccess = {
                        materialOrder = null
                        errorMsg = null
                        val updatedOrder = order.copy(
                            rawMaterials = materials,
                            materialUsageReported = true
                        )
                        receiptData = buildOrderReceiptData(updatedOrder, payments)
                    },
                    onError = { msg -> errorMsg = msg }
                )
            }
        )
    }

    receiptData?.let { data ->
        OrderReceiptDialog(data = data, onDismiss = { receiptData = null })
    }
}

@Composable
fun KaarigerOrderCard(
    order: KaarigerOrder,
    onClick: () -> Unit,
    onReportMaterials: () -> Unit,
    compact: Boolean = false
) {
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(if (compact) 12.dp else 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                StatusBadge(order.status.kaarigerDisplayName())
            }
            Spacer(modifier = Modifier.height(8.dp))
            OrderProgressChips(order)
            if (!compact) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(stringResource(R.string.kaariger_received, formatOrderDate(order.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (order.color.isNotBlank()) {
                Text(stringResource(R.string.kaariger_color, order.color), style = MaterialTheme.typography.bodySmall)
            }
            if (!compact && order.rawMaterials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.kaariger_raw_materials_count, order.rawMaterials.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (order.status == OrderStatus.PENDING_APPROVAL) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.kaariger_awaiting_approval, order.deliveredQuantity ?: 0),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB45309)
                )
            }
            if (!compact && order.status == OrderStatus.COMPLETED && order.materialUsageReported) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.receipt_available_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!compact && order.status == OrderStatus.COMPLETED && order.rawMaterials.isNotEmpty() && !order.materialUsageReported) {
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(text = stringResource(R.string.kaariger_report_materials), onClick = onReportMaterials)
            }
        }
    }
}

@Composable
private fun MaterialUsageDialog(
    order: KaarigerOrder,
    onDismiss: () -> Unit,
    onSubmit: (List<OrderMaterial>) -> Unit
) {
    val usedMap = remember(order.id) {
        mutableStateMapOf<String, String>().apply {
            order.rawMaterials.forEach { put("${it.materialId}_used", it.usedQuantity?.toString() ?: "") }
        }
    }
    val remainingMap = remember(order.id) {
        mutableStateMapOf<String, String>().apply {
            order.rawMaterials.forEach { put("${it.materialId}_rem", it.remainingQuantity?.toString() ?: "") }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.kaariger_material_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.kaariger_material_dialog_hint), style = MaterialTheme.typography.bodySmall)
                order.rawMaterials.forEach { mat ->
                    Text(stringResource(R.string.kaariger_material_assigned, mat.materialName, mat.quantity.toInt(), mat.unit), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = usedMap["${mat.materialId}_used"] ?: "",
                            onValueChange = { usedMap["${mat.materialId}_used"] = it },
                            label = { Text(stringResource(R.string.kaariger_field_used)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = remainingMap["${mat.materialId}_rem"] ?: "",
                            onValueChange = { remainingMap["${mat.materialId}_rem"] = it },
                            label = { Text(stringResource(R.string.kaariger_field_remaining)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.kaariger_save_report),
                onClick = {
                    val list = order.rawMaterials.map { mat ->
                        mat.copy(
                            usedQuantity = usedMap["${mat.materialId}_used"]?.toDoubleOrNull() ?: 0.0,
                            remainingQuantity = remainingMap["${mat.materialId}_rem"]?.toDoubleOrNull() ?: 0.0
                        )
                    }
                    onSubmit(list)
                }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.kaariger_cancel)) } }
    )
}

@Composable
private fun StatusBadge(status: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}
