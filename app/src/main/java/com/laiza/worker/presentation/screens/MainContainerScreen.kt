package com.laiza.worker.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Factory
import androidx.compose.ui.Alignment
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.laiza.worker.core.navigation.Screen
import com.laiza.worker.presentation.components.ConfirmationDialog
import com.laiza.worker.presentation.screens.AttendanceHomeScreen
import com.laiza.worker.presentation.screens.SalaryLedgerScreen
import com.laiza.worker.presentation.screens.DashboardScreen
import com.laiza.worker.presentation.screens.StoreInventoryScreen
import com.laiza.worker.presentation.screens.StaffPendingApprovalsScreen
import com.laiza.worker.presentation.screens.StaffDispatchScreen
import androidx.compose.material.icons.filled.LocalShipping
import com.laiza.worker.presentation.screens.EmployeeProfileScreen
import com.laiza.worker.presentation.components.DrawerHeader
import com.laiza.worker.presentation.components.DrawerItem
import com.laiza.worker.presentation.components.LaizaTopAppBar
import com.laiza.worker.presentation.components.SessionGuard
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffContainerScreen(
    rootNavController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val childNavController = rememberNavController()
    val session by authViewModel.userSession.collectAsState()
    val pendingApprovals by orderViewModel.pendingApprovals.collectAsState()
    val pendingCount = pendingApprovals.size
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(session?.phone) {
        val phone = session?.phone
        if (phone != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("employees").document(phone)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null && snapshot.exists()) {
                        profilePhotoUrl = snapshot.getString("profilePhotoUrl")
                    }
                }
        }
    }

    SessionGuard(session = session, rootNavController = rootNavController)

    val navBackStackEntry by childNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavItem.Home.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                DrawerHeader(session = session, profilePhotoUrl = profilePhotoUrl)
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Options
                    DrawerItem(
                        title = "Dashboard",
                        icon = Icons.Default.Home,
                        selected = currentRoute == BottomNavItem.Home.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            childNavController.navigate(BottomNavItem.Home.route) {
                                popUpTo(childNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                    DrawerItem(
                        title = "Attendance",
                        icon = Icons.Default.Badge,
                        selected = currentRoute == BottomNavItem.Attendance.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            childNavController.navigate(BottomNavItem.Attendance.route) {
                                popUpTo(childNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                    DrawerItem(
                        title = "Verify Orders",
                        icon = Icons.Default.Task,
                        selected = currentRoute == BottomNavItem.Approvals.route,
                        badgeCount = pendingCount,
                        onClick = {
                            scope.launch { drawerState.close() }
                            childNavController.navigate(BottomNavItem.Approvals.route) {
                                popUpTo(childNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                    DrawerItem(
                        title = "Salary Ledger",
                        icon = Icons.Default.Payments,
                        selected = currentRoute == "salary_tab",
                        onClick = {
                            scope.launch { drawerState.close() }
                            childNavController.navigate("salary_tab") {
                                popUpTo(childNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                    DrawerItem(
                        title = "My Profile",
                        icon = Icons.Default.Person,
                        selected = currentRoute == "employee_profile",
                        onClick = {
                            scope.launch { drawerState.close() }
                            childNavController.navigate("employee_profile") {
                                popUpTo(childNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    DrawerItem(
                        title = "Logout",
                        icon = Icons.Default.ExitToApp,
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showLogoutDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    val onHome = currentRoute == BottomNavItem.Home.route
                    LaizaTopAppBar(
                        title = when (currentRoute) {
                            BottomNavItem.Home.route -> "Staff Dashboard"
                            BottomNavItem.Inventory.route -> "Store Inventory"
                            BottomNavItem.Dispatch.route -> "Pickup & Return"
                            BottomNavItem.Attendance.route -> "Attendance"
                            BottomNavItem.Approvals.route -> "Verify Orders"
                            "employee_profile" -> "My Profile"
                            else -> "Bliss Bombay"
                        },
                        subtitle = when (currentRoute) {
                            BottomNavItem.Home.route -> "Good Morning • ${java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(java.util.Date())}"
                            BottomNavItem.Inventory.route -> "Approved products at store"
                            BottomNavItem.Dispatch.route -> "E-commerce partner handoffs"
                            BottomNavItem.Attendance.route -> "Today's Shift"
                            BottomNavItem.Approvals.route -> if (pendingCount > 0) "$pendingCount pending verification(s)" else "Kaariger delivery approvals"
                            "employee_profile" -> "Manage Bank & Account Settings"
                            else -> null
                        },
                        showBackButton = !onHome && currentRoute != "employee_profile",
                        showMenuButton = true,
                        onBackClick = {
                            if (currentRoute == "employee_profile") {
                                childNavController.popBackStack()
                            } else {
                                childNavController.navigate(BottomNavItem.Home.route) {
                                    popUpTo(childNavController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        }
                    )
                },
                bottomBar = {
                    val interactionSource1 = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val interactionSource2 = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val interactionSource3 = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val interactionSource4 = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

                    val pressed1 by interactionSource1.collectIsPressedAsState()
                    val pressed2 by interactionSource2.collectIsPressedAsState()
                    val pressed3 by interactionSource3.collectIsPressedAsState()
                    val pressed4 by interactionSource4.collectIsPressedAsState()

                    val isAnyPressed = pressed1 || pressed2 || pressed3 || pressed4

                    val navbarScale by animateFloatAsState(
                        targetValue = if (isAnyPressed) 1.03f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "navbar_scale"
                    )
                    val navbarOffsetY by animateDpAsState(
                        targetValue = if (isAnyPressed) (-3).dp else 0.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "navbar_offset"
                    )
                    val navbarElevation by animateDpAsState(
                        targetValue = if (isAnyPressed) 12.dp else 6.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "navbar_elevation"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .graphicsLayer {
                                    scaleX = navbarScale
                                    scaleY = navbarScale
                                    translationY = navbarOffsetY.toPx()
                                },
                            shape = RoundedCornerShape(100.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A).copy(alpha = 0.88f)),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFD4AF37).copy(alpha = 0.5f),
                                        Color(0xFF15803D).copy(alpha = 0.25f)
                                    )
                                )
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = navbarElevation)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val normalNavItems = remember {
                                    listOf(
                                        BottomNavItem.Home,
                                        BottomNavItem.Inventory,
                                        BottomNavItem.Dispatch,
                                        BottomNavItem.Attendance
                                    )
                                }
                                val selectedIndex = remember(currentRoute) {
                                    normalNavItems.indexOfFirst { currentRoute == it.route }
                                }
                                val numItems = normalNavItems.size

                                val animIndex by animateFloatAsState(
                                    targetValue = if (selectedIndex != -1) selectedIndex.toFloat() else 0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "nav_indicator"
                                )

                                if (selectedIndex != -1) {
                                    val activePillScale by animateFloatAsState(
                                        targetValue = if (isAnyPressed) 0.95f else 1.0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                        label = "pill_scale"
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (animIndex > 0f) {
                                            Spacer(modifier = Modifier.weight(animIndex))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .padding(horizontal = 2.dp)
                                                .graphicsLayer {
                                                    scaleX = activePillScale
                                                    scaleY = activePillScale
                                                }
                                                .background(
                                                    color = Color(0xFF15803D).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(100.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFFD4AF37).copy(alpha = 0.45f),
                                                    shape = RoundedCornerShape(100.dp)
                                                )
                                        )
                                        val remainingWeight = numItems - 1f - animIndex
                                        if (remainingWeight > 0f) {
                                            Spacer(modifier = Modifier.weight(remainingWeight))
                                        }
                                    }
                                }

                                val view = androidx.compose.ui.platform.LocalView.current
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for ((index, item) in normalNavItems.withIndex()) {
                                        val selected = index == selectedIndex
                                        val interactionSource = when(index) {
                                            0 -> interactionSource1
                                            1 -> interactionSource2
                                            2 -> interactionSource3
                                            else -> interactionSource4
                                        }
                                        val tabPressed by interactionSource.collectIsPressedAsState()
                                        val tabScale by animateFloatAsState(if (tabPressed) 0.95f else 1f, label = "tab_scale")

                                        val iconScale by animateFloatAsState(
                                            targetValue = if (selected) 1.15f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "icon_scale"
                                        )
                                        val labelColor by androidx.compose.animation.animateColorAsState(
                                            targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                            animationSpec = tween(150),
                                            label = "label_color"
                                        )
                                        val iconColor by androidx.compose.animation.animateColorAsState(
                                            targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.8f),
                                            animationSpec = tween(150),
                                            label = "icon_color"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .graphicsLayer(scaleX = tabScale, scaleY = tabScale)
                                                .clip(RoundedCornerShape(100.dp))
                                                .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null
                                                ) {
                                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_PRESS)
                                                    if (currentRoute != item.route) {
                                                        childNavController.navigate(item.route) {
                                                            popUpTo(childNavController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = false
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.title,
                                                    tint = iconColor,
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = item.title,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = labelColor,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = paddingValues.calculateTopPadding(),
                            bottom = 0.dp
                        )
                ) {
                    NavHost(
                        navController = childNavController,
                        startDestination = BottomNavItem.Home.route,
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { it / 6 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                        },
                        popEnterTransition = {
                            slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(targetOffsetX = { it / 6 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        composable(route = BottomNavItem.Home.route) {
                            DashboardScreen(
                                navController = rootNavController,
                                pendingApprovalCount = pendingCount,
                                onNavigateToApprovals = {
                                    childNavController.navigate(BottomNavItem.Approvals.route) {
                                        popUpTo(childNavController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            )
                        }
                        composable(route = BottomNavItem.Inventory.route) {
                            StoreInventoryScreen(readOnly = true)
                        }
                        composable(route = BottomNavItem.Dispatch.route) {
                            StaffDispatchScreen()
                        }
                        composable(route = BottomNavItem.Approvals.route) {
                            StaffPendingApprovalsScreen(viewModel = orderViewModel)
                        }
                        composable(route = BottomNavItem.Attendance.route) {
                            AttendanceHomeScreen(navController = rootNavController)
                        }
                        composable("salary_tab") {
                            SalaryLedgerScreen()
                        }
                        composable("employee_profile") {
                            EmployeeProfileScreen(
                                employeePhone = session?.phone ?: "",
                                onBackClick = { childNavController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Shared Logout Confirmation Dialog
    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Confirm Logout",
            message = "Are you sure you want to log out from the application?",
            confirmButtonText = "Logout",
            dismissButtonText = "Cancel",
            onConfirm = {
                showLogoutDialog = false
                authViewModel.logout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home_tab", "Home", Icons.Default.Home)
    object Inventory : BottomNavItem("inventory_tab", "Inventory", Icons.Default.Inventory)
    object Dispatch : BottomNavItem("dispatch_tab", "Dispatch", Icons.Default.LocalShipping)
    object Approvals : BottomNavItem("approvals_tab", "Verify", Icons.Default.Task)
    object Attendance : BottomNavItem("attendance_tab", "Attendance", Icons.Default.Badge)
}
