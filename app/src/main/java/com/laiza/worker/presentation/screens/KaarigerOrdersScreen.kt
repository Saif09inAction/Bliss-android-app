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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.OrderMaterial
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.components.CustomTextField
import com.laiza.worker.presentation.components.KaarigerOrderDetailSheet
import com.laiza.worker.presentation.components.OrderProgressChips
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.components.formatOrderDate
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

@Composable
fun KaarigerOrdersScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val session by authViewModel.userSession.collectAsState()
    val orders by orderViewModel.kaarigerOrders.collectAsState()
    var search by remember { mutableStateOf("") }
    var detailOrder by remember { mutableStateOf<KaarigerOrder?>(null) }
    var deliveryOrder by remember { mutableStateOf<KaarigerOrder?>(null) }
    var materialOrder by remember { mutableStateOf<KaarigerOrder?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session?.phone) {
        session?.phone?.let { orderViewModel.loadKaarigerData(it) }
    }

    val filtered = remember(orders, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) orders
        else orders.filter {
            it.productName.lowercase().contains(q) ||
                it.color.lowercase().contains(q) ||
                it.status.displayName().lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9F6))
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search orders...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (orders.isEmpty()) "No orders assigned yet" else "No matching orders",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.id }) { order ->
                    KaarigerOrderCard(
                        order = order,
                        onClick = { detailOrder = order },
                        onSubmitDelivery = { deliveryOrder = order },
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
            onSubmitDelivery = {
                detailOrder = null
                deliveryOrder = order
            },
            onReportMaterials = {
                detailOrder = null
                materialOrder = order
            }
        )
    }

    deliveryOrder?.let { order ->
        SubmitDeliveryDialog(
            order = order,
            onDismiss = { deliveryOrder = null },
            onSubmit = { qty, color, name, notes ->
                orderViewModel.submitDelivery(order.id, qty, color, name, notes,
                    onSuccess = {
                        deliveryOrder = null
                        errorMsg = null
                    },
                    onError = { msg -> errorMsg = msg }
                )
            }
        )
    }

    materialOrder?.let { order ->
        MaterialUsageDialog(
            order = order,
            onDismiss = { materialOrder = null },
            onSubmit = { materials ->
                orderViewModel.submitMaterialUsage(order.id, materials,
                    onSuccess = { materialOrder = null },
                    onError = { msg -> errorMsg = msg }
                )
            }
        )
    }
}

@Composable
fun KaarigerOrderCard(
    order: KaarigerOrder,
    onClick: () -> Unit,
    onSubmitDelivery: () -> Unit,
    onReportMaterials: () -> Unit,
    compact: Boolean = false
) {
    val remaining = order.remainingQuantity()
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(if (compact) 12.dp else 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                StatusBadge(order.status.displayName())
            }
            Spacer(modifier = Modifier.height(8.dp))
            OrderProgressChips(order)
            if (!compact) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Received: ${formatOrderDate(order.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (order.color.isNotBlank()) {
                Text("Color: ${order.color}", style = MaterialTheme.typography.bodySmall)
            }
            if (!compact && order.rawMaterials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${order.rawMaterials.size} raw material(s) assigned",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (order.status == OrderStatus.PENDING_APPROVAL) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "⏳ ${order.deliveredQuantity ?: 0} pcs awaiting staff approval",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB45309)
                )
            }
            if (!compact && (order.status == OrderStatus.ASSIGNED || order.status == OrderStatus.REJECTED) && remaining > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(text = "Submit Delivery ($remaining left)", onClick = onSubmitDelivery)
            }
            if (!compact && order.status == OrderStatus.COMPLETED && !order.materialUsageReported) {
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(text = "Report Material Usage", onClick = onReportMaterials)
            }
        }
    }
}

@Composable
private fun SubmitDeliveryDialog(
    order: KaarigerOrder,
    onDismiss: () -> Unit,
    onSubmit: (Int, String, String, String?) -> Unit
) {
    val remaining = order.remainingQuantity()
    var qty by remember { mutableStateOf(remaining.toString()) }
    var color by remember { mutableStateOf(order.color) }
    var name by remember { mutableStateOf(order.productName) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Delivery", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("You can send up to $remaining pcs. Staff will verify before adding to store inventory.", style = MaterialTheme.typography.bodySmall)
                Text("Already approved: ${order.approvedQuantity} / ${order.targetQuantity}", style = MaterialTheme.typography.labelMedium)
                CustomTextField(value = name, onValueChange = { name = it }, label = "Product Name")
                CustomTextField(value = color, onValueChange = { color = it }, label = "Color")
                CustomTextField(value = qty, onValueChange = { qty = it }, label = "Quantity Sending Now", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                CustomTextField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)")
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Submit",
                onClick = {
                    val q = qty.toIntOrNull()
                    if (q != null && q in 1..remaining && name.isNotBlank()) {
                        onSubmit(q, color, name, notes.ifBlank { null })
                    }
                }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
        title = { Text("Report Material Usage", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("How much of each assigned material did you use?", style = MaterialTheme.typography.bodySmall)
                order.rawMaterials.forEach { mat ->
                    Text("${mat.materialName} (assigned: ${mat.quantity.toInt()} ${mat.unit})", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = usedMap["${mat.materialId}_used"] ?: "",
                            onValueChange = { usedMap["${mat.materialId}_used"] = it },
                            label = { Text("Used") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = remainingMap["${mat.materialId}_rem"] ?: "",
                            onValueChange = { remainingMap["${mat.materialId}_rem"] = it },
                            label = { Text("Remaining") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Save Report",
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StatusBadge(status: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}
