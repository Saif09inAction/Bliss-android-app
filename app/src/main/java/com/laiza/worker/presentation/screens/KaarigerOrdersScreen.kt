package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
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
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.components.CustomTextField
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

@Composable
fun KaarigerOrdersScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val session by authViewModel.userSession.collectAsState()
    val orders by orderViewModel.kaarigerOrders.collectAsState()
    var selectedOrder by remember { mutableStateOf<KaarigerOrder?>(null) }

    LaunchedEffect(session?.phone) {
        session?.phone?.let { orderViewModel.loadKaarigerData(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9F6))
            .padding(16.dp)
    ) {
        Text("My Orders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Orders assigned by admin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No orders assigned yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(orders) { order ->
                    KaarigerOrderCard(
                        order = order,
                        onSubmitDelivery = { selectedOrder = order }
                    )
                }
            }
        }
    }

    selectedOrder?.let { order ->
        SubmitDeliveryDialog(
            order = order,
            onDismiss = { selectedOrder = null },
            onSubmit = { qty, color, name, notes ->
                orderViewModel.submitDelivery(order.id, qty, color, name, notes,
                    onSuccess = { selectedOrder = null },
                    onError = {}
                )
            }
        )
    }
}

@Composable
private fun KaarigerOrderCard(order: KaarigerOrder, onSubmitDelivery: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                StatusBadge(order.status.displayName())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Target: ${order.targetQuantity} pcs", style = MaterialTheme.typography.bodyMedium)
            if (order.color.isNotBlank()) Text("Color: ${order.color}", style = MaterialTheme.typography.bodySmall)
            Text("Deal: ₹${order.totalDealAmount.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            if (order.rawMaterials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Raw Materials:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                order.rawMaterials.forEach {
                    Text("• ${it.materialName}: ${it.quantity.toInt()} ${it.unit}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (order.status == OrderStatus.ASSIGNED || order.status == OrderStatus.REJECTED) {
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(text = "Submit Delivery", onClick = onSubmitDelivery)
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
    var qty by remember { mutableStateOf(order.targetQuantity.toString()) }
    var color by remember { mutableStateOf(order.color) }
    var name by remember { mutableStateOf(order.productName) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Delivery", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomTextField(value = name, onValueChange = { name = it }, label = "Product Name")
                CustomTextField(value = color, onValueChange = { color = it }, label = "Color")
                CustomTextField(value = qty, onValueChange = { qty = it }, label = "Quantity Sent", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                CustomTextField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)")
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Submit",
                onClick = {
                    val q = qty.toIntOrNull()
                    if (q != null && name.isNotBlank()) {
                        onSubmit(q, color, name, notes.ifBlank { null })
                    }
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
