package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laiza.worker.core.navigation.Screen
import com.laiza.worker.domain.models.Attendance
import com.laiza.worker.presentation.uiState.AttendanceHistoryUiState
import com.laiza.worker.presentation.uiState.TodayPunchState
import com.laiza.worker.presentation.uiState.PunchSubmitState
import com.laiza.worker.presentation.viewmodels.AttendanceViewModel

@Composable
fun AttendanceHomeScreen(
    navController: NavController,
    viewModel: AttendanceViewModel = hiltViewModel(androidx.compose.ui.platform.LocalContext.current as androidx.activity.ComponentActivity)
) {
    val todayPunchState by viewModel.todayPunchState.collectAsState()
    val historyState by viewModel.historyUiState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    var showClockOutConfirm by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (hasLocationPermission) {
                showClockOutConfirm = true
            } else {
                android.widget.Toast.makeText(context, "Location permission is required to Clock Out", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.resetStates()
        viewModel.refreshAttendanceState()
    }

    LaunchedEffect(submitState) {
        if (submitState is PunchSubmitState.Success) {
            viewModel.resetStates()
            viewModel.refreshAttendanceState()
            navController.navigate("attendance/success/SIGN_OUT")
        }
    }

    if (showClockOutConfirm) {
        AlertDialog(
            onDismissRequest = { showClockOutConfirm = false },
            title = { Text("End Shift", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to end your shift? Your clock-out location and time will be recorded.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClockOutConfirm = false
                        viewModel.submitSignOutDirectly()
                    }
                ) {
                    Text("Yes, End Shift")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (submitState is PunchSubmitState.Submitting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Recording Shift End...") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = {}
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TodayStatusCard(
            state = todayPunchState,
            onPunchClick = {
                if (todayPunchState.clockInTime.isNullOrBlank()) {
                    navController.navigate("attendance/camera/SIGN_IN")
                } else {
                    if (hasLocationPermission) {
                        showClockOutConfirm = true
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Attendance Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            when (val state = historyState) {
                is AttendanceHistoryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AttendanceHistoryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is AttendanceHistoryUiState.Success -> {
                    if (state.history.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No attendance logs found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(state.history) { record ->
                                AttendanceHistoryItem(record = record)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodayStatusCard(
    state: TodayPunchState,
    onPunchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF14532D),
                            Color(0xFF15803D)
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val todayDisplayStr = remember {
                java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            }
            Text(
                text = "ATTENDANCE FOR $todayDisplayStr".uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeSlot(
                    label = "Punch In",
                    time = if (state.clockInTime.isNullOrBlank()) "-- : --" else state.clockInTime,
                    iconColor = MaterialTheme.colorScheme.secondary
                )
                Divider(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )
                TimeSlot(
                    label = "Punch Out",
                    time = if (state.clockOutTime.isNullOrBlank()) "-- : --" else state.clockOutTime,
                    iconColor = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(100.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (state.attendanceStatus) {
                                    "ON_TIME", "PRESENT" -> Color.Green
                                    "LATE", "LEFT_EARLY" -> Color(0xFFFFD600)
                                    else -> Color.White.copy(alpha = 0.4f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Status: ${state.attendanceStatus.replace("_", " ")}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.clockOutTime.isNullOrBlank()) {
                Button(
                    onClick = onPunchClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color(0xFF0A0A0A)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.clockInTime.isNullOrBlank()) "Clock In" else "Clock Out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Shift Completed (${state.workingHours ?: ""})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSlot(label: String, time: String, iconColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = time,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun AttendanceHistoryItem(record: Attendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when (record.status.name) {
                            "ON_TIME", "PRESENT" -> Color.Green.copy(alpha = 0.1f)
                            "LATE", "LEFT_EARLY" -> Color(0xFFFFD600).copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = when (record.status.name) {
                        "ON_TIME", "PRESENT" -> Color(0xFF2E7D32)
                        "LATE", "LEFT_EARLY" -> Color(0xFFE65100)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.date,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "In: ${record.signInTime ?: "--"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Out: ${record.signOutTime ?: "--"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = when (record.status.name) {
                        "ON_TIME", "PRESENT" -> Color.Green.copy(alpha = 0.15f)
                        "LATE", "LEFT_EARLY" -> Color(0xFFFFD600).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    },
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = record.status.name.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status.name) {
                            "ON_TIME", "PRESENT" -> Color(0xFF2E7D32)
                            "LATE", "LEFT_EARLY" -> Color(0xFFE65100)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (record.workingHours > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${record.workingHours} hrs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
