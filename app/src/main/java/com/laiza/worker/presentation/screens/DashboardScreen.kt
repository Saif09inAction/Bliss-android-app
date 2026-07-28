package com.laiza.worker.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laiza.worker.domain.models.NotificationAlert
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.M3StatusChip
import com.laiza.worker.presentation.components.M3EmptyState
import com.laiza.worker.presentation.viewmodels.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    navController: NavController,
    pendingApprovalCount: Int = 0,
    onNavigateToApprovals: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val session by viewModel.employeeSession.collectAsState()
    val todayStatus by viewModel.employeeTodayAttendanceStatus.collectAsState()
    val stats by viewModel.employeeAttendanceStats.collectAsState()
    val alerts by viewModel.employeeNotifications.collectAsState()

    val presents = stats["presents"] ?: 0
    val lates = stats["lates"] ?: 0
    val earlyOuts = stats["earlyOuts"] ?: 0

    // Animations for premium feel
    var triggerStart by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        triggerStart = true
    }

    val animDuration = 400
    val opacityHero by animateFloatAsState(if (triggerStart) 1f else 0f, animationSpec = tween(animDuration, delayMillis = 50), label = "hero_op")
    val offsetHero by animateDpAsState(if (triggerStart) 0.dp else 12.dp, animationSpec = tween(animDuration, delayMillis = 50), label = "hero_y")

    val opacityShift by animateFloatAsState(if (triggerStart) 1f else 0f, animationSpec = tween(animDuration, delayMillis = 150), label = "shift_op")
    val offsetShift by animateDpAsState(if (triggerStart) 0.dp else 12.dp, animationSpec = tween(animDuration, delayMillis = 150), label = "shift_y")

    val opacityStats by animateFloatAsState(if (triggerStart) 1f else 0f, animationSpec = tween(animDuration, delayMillis = 250), label = "stats_op")
    val offsetStats by animateDpAsState(if (triggerStart) 0.dp else 12.dp, animationSpec = tween(animDuration, delayMillis = 250), label = "stats_y")

    val isCheckedIn = todayStatus.startsWith("Checked In")

    // Dynamic greeting based on current hour
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    val currentDayFormatted = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Executive Hero Card
        Box(
            modifier = Modifier
                .graphicsLayer(alpha = opacityHero)
                .offset(y = offsetHero)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0C1B40), // Dark Navy
                                    Color(0xFF1E3A8A)  // Slate Navy Blue
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$greeting • $currentDayFormatted",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = session?.name ?: "Employee Profile",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Console Connected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pending kaariger verifications
        if (pendingApprovalCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToApprovals() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Color(0xFFDC2626)) {
                                Text(pendingApprovalCount.toString())
                            }
                        }
                    ) {
                        Icon(Icons.Default.Task, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Verify Kaariger Orders", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                        Text(
                            "$pendingApprovalCount delivery batch(es) waiting for your approval",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C)
                        )
                    }
                    Text("→", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Today's Shift Card
        Box(
            modifier = Modifier
                .graphicsLayer(alpha = opacityShift)
                .offset(y = offsetShift)
        ) {
            PremiumCard(
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isCheckedIn) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (isCheckedIn) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCheckedIn) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isCheckedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "TODAY'S SHIFT STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = todayStatus,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    M3StatusChip(status = if (isCheckedIn) "ACTIVE" else "PENDING")
                }
            }
        }

        // Monthly Stats & Performance Details
        Column(
            modifier = Modifier
                .graphicsLayer(alpha = opacityStats)
                .offset(y = offsetStats),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmployeePremiumStatCard(
                    title = "Presents",
                    value = presents.toString(),
                    icon = Icons.Default.CheckCircle,
                    accentColor = Color(0xFF10B981),
                    bgColor = Color(0xFFECFDF5),
                    modifier = Modifier.weight(1f)
                )
                EmployeePremiumStatCard(
                    title = "Lates",
                    value = lates.toString(),
                    icon = Icons.Default.Schedule,
                    accentColor = Color(0xFFF59E0B),
                    bgColor = Color(0xFFFFFBEB),
                    modifier = Modifier.weight(1f)
                )
                EmployeePremiumStatCard(
                    title = "Leaves",
                    value = earlyOuts.toString(),
                    icon = Icons.Default.Badge,
                    accentColor = Color(0xFF8B5CF6),
                    bgColor = Color(0xFFF5F3FF),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Alerts & Broadcasts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            val unreadAlerts = alerts.filter { !it.isRead }
            if (unreadAlerts.isEmpty()) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    M3EmptyState(
                        title = "All Clear!",
                        description = "No active corporate alerts or broadcasts at the moment.",
                        icon = Icons.Default.Notifications
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    unreadAlerts.forEach { alert ->
                        NotificationItemRow(
                            alert = alert,
                            onClick = { viewModel.markNotificationRead(alert.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeePremiumStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F1115)
    val blockBg = if (isDark) MaterialTheme.colorScheme.surface else bgColor

    PremiumCard(
        modifier = modifier,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f) else accentColor.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            blockBg,
                            blockBg.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else accentColor
            )
        }
    }
}

@Composable
fun NotificationItemRow(
    alert: NotificationAlert,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = alert.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${alert.date}  •  ${alert.time}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
