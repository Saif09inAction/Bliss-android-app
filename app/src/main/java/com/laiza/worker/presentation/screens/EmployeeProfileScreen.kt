package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.laiza.worker.domain.models.*
import com.laiza.worker.presentation.components.CustomTextField
import com.laiza.worker.presentation.components.PasswordField
import com.laiza.worker.presentation.components.PrimaryButton
import com.laiza.worker.presentation.components.PremiumCard
import com.laiza.worker.presentation.components.M3StatusChip
import com.laiza.worker.presentation.components.premiumClickable
import com.laiza.worker.presentation.viewmodels.EmployeeViewModel
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.request.ImageRequest
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeProfileScreen(
    employeePhone: String,
    onBackClick: () -> Unit,
    viewModel: EmployeeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val employee by viewModel.selectedEmployee.collectAsState()
    val attendance by viewModel.selectedEmployeeAttendance.collectAsState()
    val payments by viewModel.selectedEmployeePayments.collectAsState()
    val balance by viewModel.selectedEmployeeBalance.collectAsState()
    val extraProfile by viewModel.selectedEmployeeExtraProfile.collectAsState()
    val activeSession by authViewModel.userSession.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Attendance, 2: Salary, 3: Payments, 4: Profile
    var showPaymentDialog by remember { mutableStateOf(false) }

    // Forms Edit States
    var isEditingProfile by remember { mutableStateOf(false) }
    var isEditingBank by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }

    // Local input states for extra fields
    var emailInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var designationInput by remember { mutableStateOf("") }
    var employeeIdInput by remember { mutableStateOf("") }

    var accountHolderInput by remember { mutableStateOf("") }
    var bankNameInput by remember { mutableStateOf("") }
    var accountNumberInput by remember { mutableStateOf("") }
    var ifscCodeInput by remember { mutableStateOf("") }
    var branchInput by remember { mutableStateOf("") }
    var upiIdInput by remember { mutableStateOf("") }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    val isAdmin = remember(activeSession) {
        activeSession?.role == Role.ADMIN
    }

    LaunchedEffect(employeePhone) {
        viewModel.selectEmployee(employeePhone)
        viewModel.loadEmployeeExtraProfile(employeePhone)
    }

    // Sync input fields when Firestore data arrives
    LaunchedEffect(extraProfile) {
        extraProfile?.let {
            emailInput = it.email
            addressInput = it.address
            designationInput = it.designation
            employeeIdInput = it.employeeId
            accountHolderInput = it.accountHolder
            bankNameInput = it.bankName
            accountNumberInput = it.accountNumber
            ifscCodeInput = it.ifscCode
            branchInput = it.branch
            upiIdInput = it.upiId
        }
    }

    if (employee == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val emp = employee!!
    val currentMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val currentMonthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }

    // Math metrics
    val currentMonthPaid = remember(payments) {
        payments.filter {
            it.type == PaymentType.SALARY_PAYMENT && it.date.startsWith(currentMonthKey)
        }.sumOf { it.amount }
    }
    val currentMonthPending = maxOf(0.0, emp.monthlySalary - currentMonthPaid)
    val isPaidThisMonth = currentMonthPending <= 0.0

    val totalPaidAllTime = remember(payments) {
        payments.filter { it.type == PaymentType.SALARY_PAYMENT }.sumOf { it.amount }
    }
    val totalAdvanceTaken = remember(payments) {
        payments.filter { it.type == PaymentType.ADVANCE }.sumOf { it.amount }
    }
    val totalAdvanceDeducted = remember(payments) {
        payments.filter { it.type == PaymentType.DEDUCTION }.sumOf { it.amount }
    }
    val advanceRemaining = maxOf(0.0, totalAdvanceTaken - totalAdvanceDeducted)

    // Image Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Update Firestore URL
                FirebaseFirestore.getInstance().collection("employees").document(emp.phone)
                    .update("profilePhotoUrl", uri.toString())
                    .addOnSuccessListener {
                        viewModel.selectEmployee(emp.phone)
                        Toast.makeText(context, "Profile Photo Updated!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = emp.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    val view = androidx.compose.ui.platform.LocalView.current
                    IconButton(onClick = {
                        com.laiza.worker.core.haptics.HapticManager.light(view)
                        onBackClick()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                val view = androidx.compose.ui.platform.LocalView.current
                FloatingActionButton(
                    onClick = {
                        com.laiza.worker.core.haptics.HapticManager.light(view)
                        showPaymentDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Record Payment")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Summary Header Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(
                                2.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.primary
                                    )
                                ),
                                CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!emp.profilePhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = emp.profilePhotoUrl,
                                contentDescription = emp.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = emp.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!emailInput.isBlank()) "${emp.phone} • $emailInput" else emp.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Joined: ${emp.joiningDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Scrollable pill tabs row directly below the header
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                divider = {},
                indicator = {}
            ) {
                val tabNames = listOf("Overview", "Attendance", "Salary", "Payments", "Profile")
                tabNames.forEachIndexed { index, name ->
                    val selected = activeTab == index
                    Tab(
                        selected = selected,
                        onClick = {
                            com.laiza.worker.core.haptics.HapticManager.light(view)
                            activeTab = index
                        },
                        text = {
                            Text(
                                text = name,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }

            // Tab contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> OverviewTabContent(
                        emp = emp,
                        currentMonthPaid = currentMonthPaid,
                        currentMonthPending = currentMonthPending,
                        isPaidThisMonth = isPaidThisMonth,
                        totalPaid = totalPaidAllTime,
                        advanceRemaining = advanceRemaining,
                        attendanceList = attendance,
                        currentMonthName = currentMonthName
                    )
                    1 -> AttendanceTabContent(attendance = attendance)
                    2 -> SalaryTabContent(
                        emp = emp,
                        payments = payments,
                        currentMonthPaid = currentMonthPaid,
                        currentMonthPending = currentMonthPending,
                        advanceRemaining = advanceRemaining,
                        currentMonthName = currentMonthName
                    )
                    3 -> PaymentsTabContent(payments = payments)
                    4 -> ProfileTabContent(
                        emp = emp,
                        extraProfile = extraProfile,
                        isAdmin = isAdmin,
                        photoPickerLauncher = photoPickerLauncher,
                        authViewModel = authViewModel,
                        emailInput = emailInput,
                        onEmailChange = { emailInput = it },
                        addressInput = addressInput,
                        onAddressChange = { addressInput = it },
                        designationInput = designationInput,
                        onDesignationChange = { designationInput = it },
                        employeeIdInput = employeeIdInput,
                        onEmployeeIdChange = { employeeIdInput = it },
                        accountHolderInput = accountHolderInput,
                        onAccountHolderChange = { accountHolderInput = it },
                        bankNameInput = bankNameInput,
                        onBankNameChange = { bankNameInput = it },
                        accountNumberInput = accountNumberInput,
                        onAccountNumberChange = { accountNumberInput = it },
                        ifscCodeInput = ifscCodeInput,
                        onIfscCodeChange = { ifscCodeInput = it },
                        branchInput = branchInput,
                        onBranchChange = { branchInput = it },
                        upiIdInput = upiIdInput,
                        onUpiIdChange = { upiIdInput = it },
                        oldPassword = oldPassword,
                        onOldPasswordChange = { oldPassword = it },
                        newPassword = newPassword,
                        onNewPasswordChange = { newPassword = it },
                        isEditingProfile = isEditingProfile,
                        onEditProfileToggle = { isEditingProfile = !isEditingProfile },
                        isEditingBank = isEditingBank,
                        onEditBankToggle = { isEditingBank = !isEditingBank },
                        showChangePassword = showChangePassword,
                        onPasswordToggle = { showChangePassword = !showChangePassword },
                        onSaveProfile = {
                            val updated = EmployeeExtraProfile(
                                phone = emp.phone,
                                email = emailInput,
                                address = addressInput,
                                designation = designationInput,
                                employeeId = employeeIdInput,
                                accountHolder = accountHolderInput,
                                bankName = bankNameInput,
                                accountNumber = accountNumberInput,
                                ifscCode = ifscCodeInput,
                                branch = branchInput,
                                upiId = upiIdInput
                            )
                            viewModel.saveEmployeeExtraProfile(updated, {
                                isEditingProfile = false
                                Toast.makeText(context, "Profile details updated!", Toast.LENGTH_SHORT).show()
                            }, { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            })
                        },
                        onSaveBank = {
                            if (accountHolderInput.isBlank() || bankNameInput.isBlank() || accountNumberInput.isBlank() || ifscCodeInput.isBlank()) {
                                Toast.makeText(context, "Required fields (*Account Holder Name, *Bank Name, *Account Number, *IFSC Code) must be filled", Toast.LENGTH_LONG).show()
                            } else {
                                val updated = EmployeeExtraProfile(
                                    phone = emp.phone,
                                    email = emailInput,
                                    address = addressInput,
                                    designation = designationInput,
                                    employeeId = employeeIdInput,
                                    accountHolder = accountHolderInput,
                                    bankName = bankNameInput,
                                    accountNumber = accountNumberInput,
                                    ifscCode = ifscCodeInput,
                                    branch = branchInput,
                                    upiId = upiIdInput
                                )
                                viewModel.saveEmployeeExtraProfile(updated, {
                                    isEditingBank = false
                                    Toast.makeText(context, "Bank details saved successfully!", Toast.LENGTH_SHORT).show()
                                }, { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                })
                            }
                        },
                        onUpdatePassword = {
                            if (newPassword.length < 6) {
                                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                            } else {
                                FirebaseFirestore.getInstance().collection("employees").document(emp.phone)
                                    .update("password", newPassword)
                                    .addOnSuccessListener {
                                        showChangePassword = false
                                        oldPassword = ""
                                        newPassword = ""
                                        Toast.makeText(context, "Credentials updated successfully!", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    )
                }
            }
        }

        if (showPaymentDialog && isAdmin) {
            AddPaymentDialog(
                employeePhone = employeePhone,
                onDismiss = { showPaymentDialog = false },
                onSave = { payment ->
                    viewModel.addPayment(
                        payment = payment,
                        onSuccess = { showPaymentDialog = false },
                        onError = {}
                    )
                }
            )
        }
    }
}

// ---------------- SUB TAB CONTENTS ----------------

@Composable
fun OverviewTabContent(
    emp: Employee,
    currentMonthPaid: Double,
    currentMonthPending: Double,
    isPaidThisMonth: Boolean,
    totalPaid: Double,
    advanceRemaining: Double,
    attendanceList: List<Attendance>,
    currentMonthName: String
) {
    val totalWorkDays = attendanceList.size
    val presentDays = attendanceList.count { it.signInTime != null }
    val attendanceRate = if (totalWorkDays > 0) (presentDays.toDouble() / totalWorkDays * 100).toInt() else 100

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Summary Quick Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Summary Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "$currentMonthName Payout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "₹${currentMonthPaid.toInt()} Paid", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isPaidThisMonth) {
                                M3StatusChip(status = "FULLY PAID")
                            } else {
                                M3StatusChip(status = "₹${currentMonthPending.toInt()} REMAINING")
                            }
                        }
                    }
                }
            }
        }

        // Metrics row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Attendance Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "$attendanceRate%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$presentDays of $totalWorkDays days present", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Advance Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "₹${advanceRemaining.toInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = if (advanceRemaining > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Repaid via deductions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // Total Paid all time
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "All-Time Earnings Paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "₹${totalPaid.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    Icon(imageVector = Icons.Default.Wallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

@Composable
fun AttendanceTabContent(attendance: List<Attendance>) {
    val totalWorkDays = attendance.size
    val presentDays = attendance.count { it.signInTime != null }
    val lateDays = attendance.count { it.signInTime != null && it.signInTime!!.substringBefore(":").toIntOrNull()?.let { hr -> hr >= 10 } == true }
    val absentDays = maxOf(0, totalWorkDays - presentDays)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Stats grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Present", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = presentDays.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Late", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = lateDays.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Absent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = absentDays.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Calendar visual block representation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Shift Calendar View", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Simple 30 day grid layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..30).chunked(6).forEach { chunk ->
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                chunk.forEach { day ->
                                    val attendanceRecord = attendance.find { it.date.endsWith(String.format("-%02d", day)) || it.date.endsWith("-$day") }
                                    val isP = attendanceRecord != null
                                    val isL = attendanceRecord != null && attendanceRecord.signInTime != null && attendanceRecord.signInTime!!.substringBefore(":").toIntOrNull()?.let { hr -> hr >= 10 } == true
                                    
                                    val cellColor = when {
                                        isL -> Color(0xFFFFFBEB)
                                        isP -> Color(0xFFECFDF5)
                                        else -> Color(0xFFFEF2F2)
                                    }
                                    val cellBorderColor = when {
                                        isL -> Color(0xFFF59E0B)
                                        isP -> Color(0xFF10B981)
                                        else -> Color(0xFFEF4444).copy(alpha = 0.5f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .background(cellColor, RoundedCornerShape(6.dp))
                                            .border(1.dp, cellBorderColor, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = day.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cellBorderColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Attendance punches logs
        items(attendance.sortedByDescending { it.date }) { record ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = record.date, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Punch In: ${record.signInTime ?: "--"} | Punch Out: ${record.signOutTime ?: "--"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SalaryTabContent(
    emp: Employee,
    payments: List<PaymentTransaction>,
    currentMonthPaid: Double,
    currentMonthPending: Double,
    advanceRemaining: Double,
    currentMonthName: String
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Summary breakdown card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Salary Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Base Monthly Payout", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "₹${emp.monthlySalary.toInt()}", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Paid this Month", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "₹${currentMonthPaid.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Outstanding Advance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "₹${advanceRemaining.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Pending Payout", fontWeight = FontWeight.Bold)
                        Text(text = "₹${currentMonthPending.toInt()}", fontWeight = FontWeight.ExtraBold, color = if (currentMonthPending > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981))
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentsTabContent(payments: List<PaymentTransaction>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        if (payments.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No payout history found", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        } else {
            items(payments.sortedByDescending { "${it.date} ${it.time}" }) { transaction ->
                PaymentItemRow(transaction = transaction)
            }
        }
    }
}

@Composable
fun PaymentItemRow(transaction: PaymentTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typeColor = when (transaction.type) {
                PaymentType.SALARY_PAYMENT -> Color(0xFFECFDF5)
                PaymentType.ADVANCE -> Color(0xFFFFFBEB)
                PaymentType.DEDUCTION -> Color(0xFFFEF2F2)
                PaymentType.EXTRA_PAYMENT -> Color(0xFFEFF6FF)
            }
            val accentColor = when (transaction.type) {
                PaymentType.SALARY_PAYMENT -> Color(0xFF10B981)
                PaymentType.ADVANCE -> Color(0xFFF59E0B)
                PaymentType.DEDUCTION -> Color(0xFFEF4444)
                PaymentType.EXTRA_PAYMENT -> Color(0xFF3B82F6)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(typeColor, CircleShape)
                    .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.type) {
                        PaymentType.SALARY_PAYMENT -> Icons.Default.Payments
                        PaymentType.ADVANCE -> Icons.Default.Wallet
                        PaymentType.DEDUCTION -> Icons.Default.RemoveCircle
                        PaymentType.EXTRA_PAYMENT -> Icons.Default.AddCircle
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (transaction.type) {
                        PaymentType.SALARY_PAYMENT -> "Salary Payout"
                        PaymentType.ADVANCE -> "Advance Taken"
                        PaymentType.DEDUCTION -> "Advance Deduction"
                        PaymentType.EXTRA_PAYMENT -> "Bonus Payout"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${transaction.date} • ${transaction.time}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (!transaction.remarks.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.remarks!!,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.type == PaymentType.DEDUCTION) "-" else "+"}₹${transaction.amount.toInt()}",
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "By: ${transaction.createdBy}",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun ProfileTabContent(
    emp: Employee,
    extraProfile: EmployeeExtraProfile?,
    isAdmin: Boolean,
    photoPickerLauncher: androidx.activity.compose.ManagedActivityResultLauncher<PickVisualMediaRequest, android.net.Uri?>,
    authViewModel: AuthViewModel,
    emailInput: String,
    onEmailChange: (String) -> Unit,
    addressInput: String,
    onAddressChange: (String) -> Unit,
    designationInput: String,
    onDesignationChange: (String) -> Unit,
    employeeIdInput: String,
    onEmployeeIdChange: (String) -> Unit,
    accountHolderInput: String,
    onAccountHolderChange: (String) -> Unit,
    bankNameInput: String,
    onBankNameChange: (String) -> Unit,
    accountNumberInput: String,
    onAccountNumberChange: (String) -> Unit,
    ifscCodeInput: String,
    onIfscCodeChange: (String) -> Unit,
    branchInput: String,
    onBranchChange: (String) -> Unit,
    upiIdInput: String,
    onUpiIdChange: (String) -> Unit,
    oldPassword: String,
    onOldPasswordChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    isEditingProfile: Boolean,
    onEditProfileToggle: () -> Unit,
    isEditingBank: Boolean,
    onEditBankToggle: () -> Unit,
    showChangePassword: Boolean,
    onPasswordToggle: () -> Unit,
    onSaveProfile: () -> Unit,
    onSaveBank: () -> Unit,
    onUpdatePassword: () -> Unit
) {
    val view = androidx.compose.ui.platform.LocalView.current
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {


        // BANK DETAILS CARD (Editable for employee, read-only for admin)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "BANK DETAILS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (!isAdmin) {
                            TextButton(onClick = {
                                if (isEditingBank) {
                                    com.laiza.worker.core.haptics.HapticManager.medium(view)
                                    onSaveBank()
                                } else {
                                    com.laiza.worker.core.haptics.HapticManager.light(view)
                                    onEditBankToggle()
                                }
                            }) {
                                Text(text = if (isEditingBank) "Save" else "Edit")
                            }
                        }
                    }

                    if (isEditingBank && !isAdmin) {
                        CustomTextField(value = accountHolderInput, onValueChange = onAccountHolderChange, label = "*Account Holder Name")
                        CustomTextField(value = bankNameInput, onValueChange = onBankNameChange, label = "*Bank Name")
                        CustomTextField(value = accountNumberInput, onValueChange = onAccountNumberChange, label = "*Account Number")
                        CustomTextField(value = ifscCodeInput, onValueChange = onIfscCodeChange, label = "*IFSC Code")
                        CustomTextField(value = branchInput, onValueChange = onBranchChange, label = "Branch Name (optional)")
                        CustomTextField(value = upiIdInput, onValueChange = onUpiIdChange, label = "UPI ID (optional)")
                    } else {
                        ProfileInfoRow(icon = Icons.Default.Person, title = "Account Holder", value = if (!accountHolderInput.isBlank()) accountHolderInput else emp.name)
                        ProfileInfoRow(icon = Icons.Default.AccountBalance, title = "Bank Name", value = if (!bankNameInput.isBlank()) bankNameInput else "Not set")
                        ProfileInfoRow(icon = Icons.Default.Fingerprint, title = "Account Number", value = if (!accountNumberInput.isBlank()) accountNumberInput else "Not set")
                        ProfileInfoRow(icon = Icons.Default.Info, title = "IFSC Code", value = if (!ifscCodeInput.isBlank()) ifscCodeInput else "Not set")
                        ProfileInfoRow(icon = Icons.Default.LocationOn, title = "Branch", value = if (!branchInput.isBlank()) branchInput else "Not set")
                        ProfileInfoRow(icon = Icons.Default.QrCode, title = "UPI ID", value = if (!upiIdInput.isBlank()) upiIdInput else "Not set")
                    }
                }
            }
        }

        // ACCOUNT CONTROLS CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "ACCOUNT CONTROLS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    if (showChangePassword) {
                        PasswordField(
                            value = newPassword,
                            onValueChange = onNewPasswordChange,
                            label = "New Password"
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = {
                                com.laiza.worker.core.haptics.HapticManager.light(view)
                                onPasswordToggle()
                            }, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }
                            Button(onClick = {
                                com.laiza.worker.core.haptics.HapticManager.medium(view)
                                onUpdatePassword()
                            }, modifier = Modifier.weight(1f)) {
                                Text("Update")
                            }
                        }
                    } else {
                        AccountActionRow(icon = Icons.Default.Lock, title = "Change Security Password", onClick = onPasswordToggle)
                        AccountActionRow(icon = Icons.Default.PhotoCamera, title = "Update Profile Photo", onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                        AccountActionRow(icon = Icons.Default.ExitToApp, title = "Logout", tint = MaterialTheme.colorScheme.error) {
                            authViewModel.logout()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AccountActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .premiumClickable(hapticType = "light") { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun AddPaymentDialog(
    employeePhone: String,
    onDismiss: () -> Unit,
    onSave: (PaymentTransaction) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PaymentType.SALARY_PAYMENT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payout Transaction", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dropdown Selection or Custom Segment
                Text(text = "Transaction Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentType.values().forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { selectedType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.name.split("_").first(),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                CustomTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount (₹)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                CustomTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = "Remarks / Notes"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                    if (amtVal > 0.0) {
                        onSave(
                            PaymentTransaction(
                                id = UUID.randomUUID().toString(),
                                employeeId = employeePhone,
                                amount = amtVal,
                                type = selectedType,
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                remarks = remarks,
                                createdBy = "Admin"
                            )
                        )
                    }
                }
            ) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
