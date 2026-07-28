package com.laiza.worker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.laiza.worker.presentation.components.ConfirmationDialog
import com.laiza.worker.presentation.components.DrawerHeader
import com.laiza.worker.presentation.components.DrawerItem
import com.laiza.worker.presentation.components.LaizaTopAppBar
import com.laiza.worker.presentation.components.SessionGuard
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaarigerContainerScreen(
    rootNavController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val childNavController = rememberNavController()
    val session by authViewModel.userSession.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    SessionGuard(session = session, rootNavController = rootNavController)

    val navBackStackEntry by childNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: KaarigerNav.Home.route

    val orders by orderViewModel.kaarigerOrders.collectAsState()
    LaunchedEffect(session?.phone) {
        session?.phone?.let { orderViewModel.loadKaarigerData(it) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                DrawerHeader(session = session, profilePhotoUrl = null)
                Spacer(modifier = Modifier.height(12.dp))
                DrawerItem(
                    title = "Dashboard",
                    icon = Icons.Default.Home,
                    selected = currentRoute == KaarigerNav.Home.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        childNavController.navigate(KaarigerNav.Home.route)
                    }
                )
                DrawerItem(
                    title = "My Orders",
                    icon = Icons.Default.Task,
                    selected = currentRoute == KaarigerNav.Orders.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        childNavController.navigate(KaarigerNav.Orders.route)
                    }
                )
                DrawerItem(
                    title = "Payments",
                    icon = Icons.Default.Payments,
                    selected = currentRoute == KaarigerNav.Payments.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        childNavController.navigate(KaarigerNav.Payments.route)
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
            }
        }
    ) {
        Scaffold(
            topBar = {
                LaizaTopAppBar(
                    title = when (currentRoute) {
                        KaarigerNav.Home.route -> "Kaariger Dashboard"
                        KaarigerNav.Orders.route -> "My Orders"
                        KaarigerNav.Payments.route -> "Payments"
                        else -> "Laiza Bags"
                    },
                    subtitle = session?.name,
                    showMenuButton = true,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf(KaarigerNav.Home, KaarigerNav.Orders, KaarigerNav.Payments).forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    childNavController.navigate(item.route) {
                                        popUpTo(KaarigerNav.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = childNavController,
                startDestination = KaarigerNav.Home.route,
                modifier = Modifier.padding(top = padding.calculateTopPadding())
            ) {
                composable(KaarigerNav.Home.route) {
                    KaarigerDashboardContent(session?.name ?: "Kaariger", orders.size)
                }
                composable(KaarigerNav.Orders.route) {
                    KaarigerOrdersScreen()
                }
                composable(KaarigerNav.Payments.route) {
                    KaarigerPaymentsScreen()
                }
            }
        }
    }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Confirm Logout",
            message = "Are you sure you want to log out?",
            confirmButtonText = "Logout",
            dismissButtonText = "Cancel",
            onConfirm = { showLogoutDialog = false; authViewModel.logout() },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun KaarigerDashboardContent(name: String, activeOrders: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Welcome, $name", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("You have $activeOrders active order(s)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("How it works", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. Admin assigns you an order with raw materials")
                Text("2. You manufacture and submit delivery details")
                Text("3. Store staff verifies and adds to inventory")
                Text("4. Track your payments and remaining balance here")
            }
        }
    }
}

private sealed class KaarigerNav(val route: String, val title: String, val icon: ImageVector) {
    object Home : KaarigerNav("kaariger_home", "Home", Icons.Default.Home)
    object Orders : KaarigerNav("kaariger_orders", "Orders", Icons.Default.Task)
    object Payments : KaarigerNav("kaariger_payments", "Payments", Icons.Default.Payments)
}
