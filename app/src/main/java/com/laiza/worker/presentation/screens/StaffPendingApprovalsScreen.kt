package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.OrderApprovalRecord
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.formatOrderDate
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

private enum class ApprovalTab { Pending, History }

@Composable
fun StaffPendingApprovalsScreen(
    viewModel: OrderViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val session by authViewModel.userSession.collectAsState()
    val pending by viewModel.pendingApprovals.collectAsState()
    val history by viewModel.staffApprovalHistory.collectAsState()
    var tab by remember { mutableStateOf(ApprovalTab.Pending) }
    var search by remember { mutableStateOf("") }
    var rejectOrderId by remember { mutableStateOf<String?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var detailRecord by remember { mutableStateOf<OrderApprovalRecord?>(null) }

    LaunchedEffect(session?.phone) {
        session?.phone?.let { viewModel.loadStaffApprovalHistory(it) }
    }

    val filteredHistory = remember(history, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) history
        else history.filter {
            it.productName.lowercase().contains(q) ||
                it.kaarigerName.lowercase().contains(q) ||
                it.color.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = tab.ordinal) {
            Tab(
                selected = tab == ApprovalTab.Pending,
                onClick = { tab = ApprovalTab.Pending },
                text = {
                    BadgedBox(
                        badge = {
                            if (pending.isNotEmpty()) {
                                Badge(containerColor = Color(0xFFDC2626)) {
                                    Text(pending.size.toString())
                                }
                            }
                        }
                    ) {
                        Text("Pending")
                    }
                }
            )
            Tab(
                selected = tab == ApprovalTab.History,
                onClick = { tab = ApprovalTab.History },
                text = { Text("My Approvals") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tab == ApprovalTab.History) {
            Text(
                "Only deliveries you approved appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search your approvals...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Text(
                "Review deliveries before adding to inventory",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        when (tab) {
            ApprovalTab.Pending -> {
                if (pending.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pending deliveries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(pending, key = { it.id }) { order ->
                            PendingOrderCard(
                                order = order,
                                onApprove = { viewModel.approveOrder(order.id, onSuccess = {}, onError = {}) },
                                onReject = { rejectOrderId = order.id }
                            )
                        }
                    }
                }
            }
            ApprovalTab.History -> {
                if (filteredHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (search.isBlank()) "You haven't approved any deliveries yet" else "No matches found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredHistory, key = { it.id }) { record ->
                            HistoryApprovalCard(record = record, onClick = { detailRecord = record })
                        }
                    }
                }
            }
        }
    }

    if (rejectOrderId != null) {
        AlertDialog(
            onDismissRequest = { rejectOrderId = null },
            title = { Text("Reject Delivery") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectOrder(rejectOrderId!!, rejectReason, onSuccess = {
                            rejectOrderId = null
                            rejectReason = ""
                        }, onError = {})
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reject") }
            },
            dismissButton = {
                TextButton(onClick = { rejectOrderId = null }) { Text("Cancel") }
            }
        )
    }

    detailRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { detailRecord = null },
            title = { Text(record.productName, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLine("Kaariger", record.kaarigerName)
                    DetailLine("You approved", "${record.batchQuantity} pcs")
                    DetailLine("Order progress", "${record.approvedTotalAfter}/${record.targetQuantity} pcs")
                    if (record.color.isNotBlank()) DetailLine("Color", record.color)
                    DetailLine("Approved on", formatOrderDate(record.verifiedAt))
                }
            },
            confirmButton = {
                TextButton(onClick = { detailRecord = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PendingOrderCard(
    order: KaarigerOrder,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(order.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("From: ${order.kaarigerName}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Progress: ${order.approvedQuantity} / ${order.targetQuantity} approved",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailChip("This batch", "${order.deliveredQuantity ?: 0} pcs")
                DetailChip("Color", order.deliveryColor ?: order.color)
            }
            order.deliveryNotes?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Notes: $it", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun HistoryApprovalCard(record: OrderApprovalRecord, onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(record.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("${record.kaarigerName} · You approved ${record.batchQuantity} pcs", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Order now ${record.approvedTotalAfter}/${record.targetQuantity} · ${formatOrderDate(record.verifiedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
