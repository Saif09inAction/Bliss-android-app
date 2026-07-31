package com.laiza.worker.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.domain.models.ColorQuantity
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.OrderApprovalRecord
import com.laiza.worker.domain.models.ProductColors
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.formatOrderDate
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel

private enum class ApprovalTab { Pending, History }

private data class ColorLineDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    val color: String = ProductColors.PRESETS.first(),
    val quantity: String = ""
)

@Composable
fun StaffPendingApprovalsScreen(
    viewModel: OrderViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val session by authViewModel.userSession.collectAsState()
    val pending by viewModel.pendingApprovals.collectAsState()
    val history by viewModel.staffApprovalHistory.collectAsState()
    var tab by remember { mutableStateOf(ApprovalTab.Pending) }
    var search by remember { mutableStateOf("") }
    var rejectOrderId by remember { mutableStateOf<String?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var approveTarget by remember { mutableStateOf<KaarigerOrder?>(null) }
    var detailRecord by remember { mutableStateOf<OrderApprovalRecord?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(session?.phone) {
        session?.phone?.let { viewModel.loadStaffApprovalHistory(it) }
    }

    val filteredHistory = remember(history, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) history
        else history.filter {
            it.productName.lowercase().contains(q) ||
                it.kaarigerName.lowercase().contains(q) ||
                it.color.lowercase().contains(q) ||
                it.colorBreakdown.lowercase().contains(q)
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
                "Edit qty & colours — accept good pieces, reject defective",
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
                                onEditApprove = { approveTarget = order },
                                onRejectAll = { rejectOrderId = order.id }
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

    approveTarget?.let { order ->
        ApproveEditDialog(
            order = order,
            saving = saving,
            onDismiss = { if (!saving) approveTarget = null },
            onConfirm = { accepted, colors, rejected, note ->
                saving = true
                viewModel.approveOrder(
                    orderId = order.id,
                    acceptedQuantity = accepted,
                    colorBreakdown = colors,
                    rejectedQuantity = rejected,
                    rejectionNote = note,
                    onSuccess = {
                        saving = false
                        approveTarget = null
                        Toast.makeText(context, "Approved $accepted pcs", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg ->
                        saving = false
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (rejectOrderId != null) {
        AlertDialog(
            onDismissRequest = { rejectOrderId = null },
            title = { Text("Reject entire delivery") },
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
                            Toast.makeText(context, "Delivery rejected", Toast.LENGTH_SHORT).show()
                        }, onError = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reject all") }
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
                    DetailLine("Accepted", "${record.batchQuantity} pcs")
                    if (record.rejectedQuantity > 0) {
                        DetailLine("Rejected / defective", "${record.rejectedQuantity} pcs")
                    }
                    DetailLine("Order progress", "${record.approvedTotalAfter}/${record.targetQuantity} pcs")
                    if (record.colorBreakdown.isNotBlank()) DetailLine("Colours", record.colorBreakdown)
                    else if (record.color.isNotBlank()) DetailLine("Color", record.color)
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
private fun ApproveEditDialog(
    order: KaarigerOrder,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (accepted: Int, colors: List<ColorQuantity>, rejected: Int, note: String?) -> Unit
) {
    val delivered = order.deliveredQuantity ?: 0
    var acceptedStr by remember(order.id) { mutableStateOf(delivered.toString()) }
    var rejectNote by remember { mutableStateOf("") }
    var showCustomPicker by remember { mutableStateOf(false) }
    var customPickerLineId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var lines by remember(order.id) {
        mutableStateOf(
            listOf(
                ColorLineDraft(
                    color = order.deliveryColor?.takeIf { it.isNotBlank() }
                        ?: order.color.takeIf { it.isNotBlank() }
                        ?: ProductColors.PRESETS.first(),
                    quantity = delivered.toString()
                )
            )
        )
    }

    val accepted = acceptedStr.toIntOrNull() ?: -1
    val rejected = (delivered - accepted).coerceAtLeast(0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text("Review & approve", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${order.productName} · from ${order.kaarigerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailLine("Delivered by kaariger", "$delivered pcs")
                        DetailLine("Order progress", "${order.approvedQuantity}/${order.targetQuantity}")
                    }

                    OutlinedTextField(
                        value = acceptedStr,
                        onValueChange = {
                            acceptedStr = it.filter { c -> c.isDigit() }
                            error = null
                        },
                        label = { Text("Accept quantity") },
                        supportingText = {
                            Text(
                                if (accepted in 0..delivered) "Reject / defective: $rejected pcs"
                                else "Must be between 0 and $delivered"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (accepted > 0) {
                        Text("Colours for accepted pieces", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Total colour qty must equal $accepted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        lines.forEachIndexed { index, line ->
                            ColorLineEditor(
                                line = line,
                                onColorChange = { c ->
                                    lines = lines.toMutableList().also { it[index] = line.copy(color = c) }
                                },
                                onQtyChange = { q ->
                                    lines = lines.toMutableList().also { it[index] = line.copy(quantity = q.filter { ch -> ch.isDigit() }) }
                                },
                                onCustomColor = {
                                    customPickerLineId = line.id
                                    showCustomPicker = true
                                },
                                onRemove = {
                                    if (lines.size > 1) {
                                        lines = lines.filterNot { it.id == line.id }
                                    }
                                },
                                canRemove = lines.size > 1
                            )
                        }
                        TextButton(
                            onClick = {
                                lines = lines + ColorLineDraft(quantity = "")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add colour")
                        }
                    }

                    if (rejected > 0 || accepted == 0) {
                        OutlinedTextField(
                            value = rejectNote,
                            onValueChange = { rejectNote = it },
                            label = { Text("Reject / defective note (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (accepted !in 0..delivered) {
                                error = "Accept qty must be 0–$delivered"
                                return@Button
                            }
                            val rejectQty = delivered - accepted
                            val colors = if (accepted == 0) emptyList() else lines.map {
                                ColorQuantity(it.color.trim(), it.quantity.toIntOrNull() ?: 0)
                            }
                            if (accepted > 0) {
                                val sum = colors.sumOf { it.quantity }
                                if (sum != accepted) {
                                    error = "Colour quantities ($sum) must equal accepted ($accepted)"
                                    return@Button
                                }
                                if (colors.any { it.color.isBlank() || it.quantity <= 0 }) {
                                    error = "Each colour needs a name and qty > 0"
                                    return@Button
                                }
                            }
                            onConfirm(accepted, colors, rejectQty, rejectNote.ifBlank { null })
                        },
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (accepted == 0) "Reject all" else "Confirm")
                        }
                    }
                }
            }
        }
    }

    if (showCustomPicker && customPickerLineId != null) {
        CustomColorPickerDialog(
            initialHex = "#E11D48",
            onDismiss = { showCustomPicker = false },
            onPick = { nameOrHex ->
                val id = customPickerLineId
                lines = lines.map { if (it.id == id) it.copy(color = nameOrHex) else it }
                showCustomPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorLineEditor(
    line: ColorLineDraft,
    onColorChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onCustomColor: () -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val options = ProductColors.PRESETS + "Custom…"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1.2f)
        ) {
            OutlinedTextField(
                value = if (line.color in ProductColors.PRESETS) line.color else line.color.ifBlank { "Custom" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Colour") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                singleLine = true
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            expanded = false
                            if (opt == "Custom…") onCustomColor()
                            else onColorChange(opt)
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = line.quantity,
            onValueChange = onQtyChange,
            label = { Text("Qty") },
            modifier = Modifier.weight(0.7f),
            singleLine = true
        )
        IconButton(onClick = onRemove, enabled = canRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
        }
    }
    if (line.color !in ProductColors.PRESETS && line.color.isNotBlank()) {
        TextButton(onClick = onCustomColor) {
            Text("Edit custom colour (${line.color})", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialHex: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var hex by remember { mutableStateOf(initialHex.removePrefix("#")) }
    val preview = runCatching {
        Color(android.graphics.Color.parseColor("#${hex.padEnd(6, '0').take(6)}"))
    }.getOrElse { Color(0xFFE11D48) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom colour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(preview, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )
                Text("Drag hue", style = MaterialTheme.typography.labelMedium)
                HueSlider(
                    hue = hue,
                    onHueChange = { h ->
                        hue = h
                        val hsvColor = android.graphics.Color.HSVToColor(floatArrayOf(h, 0.85f, 0.95f))
                        hex = String.format("%06X", 0xFFFFFF and hsvColor)
                    }
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = { v ->
                        hex = v.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
                    },
                    label = { Text("Hex code") },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val code = hex.padStart(6, '0').take(6).uppercase()
                onPick("#$code")
            }) { Text("Use colour") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun HueSlider(hue: Float, onHueChange: (Float) -> Unit) {
    val hues = (0..360 step 2).map { Color.hsv(it.toFloat(), 0.85f, 0.95f) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = ((offset.x / size.width) * 360f).coerceIn(0f, 360f)
                    onHueChange(h)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val h = ((change.position.x / size.width) * 360f).coerceIn(0f, 360f)
                    onHueChange(h)
                }
            }
    ) {
        drawRect(brush = Brush.horizontalGradient(hues))
        val x = (hue / 360f) * size.width
        drawCircle(
            color = Color.White,
            radius = 10.dp.toPx(),
            center = Offset(x, size.height / 2f)
        )
        drawCircle(
            color = Color.Black,
            radius = 10.dp.toPx(),
            center = Offset(x, size.height / 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
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
    onEditApprove: () -> Unit,
    onRejectAll: () -> Unit
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
                val hintColor = order.deliveryColor ?: order.color
                if (hintColor.isNotBlank()) DetailChip("Hint colour", hintColor)
            }
            order.deliveryNotes?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Notes: $it", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onEditApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit & approve")
                }
                OutlinedButton(onClick = onRejectAll, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject all")
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
            Text(
                "${record.kaarigerName} · Accepted ${record.batchQuantity} pcs" +
                    if (record.rejectedQuantity > 0) " · Rejected ${record.rejectedQuantity}" else "",
                style = MaterialTheme.typography.bodySmall
            )
            if (record.colorBreakdown.isNotBlank()) {
                Text(record.colorBreakdown, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
