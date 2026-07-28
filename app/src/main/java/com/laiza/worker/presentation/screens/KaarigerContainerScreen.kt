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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.laiza.worker.R
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.components.ConfirmationDialog
import com.laiza.worker.presentation.components.DrawerHeader
import com.laiza.worker.presentation.components.DrawerItem
import com.laiza.worker.presentation.components.KaarigerLanguageSwitch
import com.laiza.worker.presentation.components.KaarigerLocalizedContent
import com.laiza.worker.presentation.components.LaizaTopAppBar
import com.laiza.worker.presentation.components.SessionGuard
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import com.laiza.worker.presentation.viewmodels.KaarigerLanguageViewModel
import com.laiza.worker.presentation.viewmodels.OrderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaarigerContainerScreen(
    rootNavController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel(),
    languageViewModel: KaarigerLanguageViewModel = hiltViewModel()
) {
    val language by languageViewModel.language.collectAsState()

    KaarigerLocalizedContent(languageCode = language) {
        KaarigerContainerContent(
            rootNavController = rootNavController,
            authViewModel = authViewModel,
            orderViewModel = orderViewModel,
            language = language,
            onLanguageSelected = languageViewModel::setLanguage
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KaarigerContainerContent(
    rootNavController: NavController,
    authViewModel: AuthViewModel,
    orderViewModel: OrderViewModel,
    language: String,
    onLanguageSelected: (String) -> Unit
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
                    title = stringResource(R.string.kaariger_nav_home),
                    icon = Icons.Default.Home,
                    selected = currentRoute == KaarigerNav.Home.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        childNavController.navigate(KaarigerNav.Home.route)
                    }
                )
                DrawerItem(
                    title = stringResource(R.string.kaariger_nav_orders),
                    icon = Icons.Default.Task,
                    selected = currentRoute == KaarigerNav.Orders.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        childNavController.navigate(KaarigerNav.Orders.route)
                    }
                )
                DrawerItem(
                    title = stringResource(R.string.kaariger_nav_payments),
                    icon = Icons.Default.Payments,
                    selected = currentRoute == KaarigerNav.Payments.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        childNavController.navigate(KaarigerNav.Payments.route)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                KaarigerLanguageSwitch(
                    selectedLanguage = language,
                    onLanguageSelected = onLanguageSelected
                )
                Spacer(modifier = Modifier.height(8.dp))
                DrawerItem(
                    title = stringResource(R.string.kaariger_nav_logout),
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
                        KaarigerNav.Home.route -> stringResource(R.string.kaariger_title_dashboard)
                        KaarigerNav.Orders.route -> stringResource(R.string.kaariger_title_orders)
                        KaarigerNav.Payments.route -> stringResource(R.string.kaariger_title_payments)
                        else -> stringResource(R.string.kaariger_title_app)
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
                            icon = { Icon(item.icon, contentDescription = stringResource(item.titleRes)) },
                            label = { Text(stringResource(item.titleRes), fontSize = 11.sp) }
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
                    KaarigerDashboardContent(
                        name = session?.name ?: stringResource(R.string.kaariger_default_name),
                        orders = orders,
                        onViewAllOrders = {
                            childNavController.navigate(KaarigerNav.Orders.route) {
                                popUpTo(KaarigerNav.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(KaarigerNav.Orders.route) {
                    KaarigerOrdersScreen(orderViewModel = orderViewModel, authViewModel = authViewModel)
                }
                composable(KaarigerNav.Payments.route) {
                    KaarigerPaymentsScreen(orderViewModel = orderViewModel, authViewModel = authViewModel)
                }
            }
        }
    }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.kaariger_logout_title),
            message = stringResource(R.string.kaariger_logout_message),
            confirmButtonText = stringResource(R.string.kaariger_logout_confirm),
            dismissButtonText = stringResource(R.string.kaariger_cancel),
            onConfirm = { showLogoutDialog = false; authViewModel.logout() },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun KaarigerDashboardContent(
    name: String,
    orders: List<KaarigerOrder>,
    onViewAllOrders: () -> Unit
) {
    val activeOrders = orders.count { it.status != OrderStatus.COMPLETED }
    val pendingBatches = orders.count { it.status == OrderStatus.PENDING_APPROVAL }
    val totalRemaining = orders.sumOf { it.remainingQuantity() }
    val recentOrders = orders.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.kaariger_welcome, name),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.kaariger_active_summary, activeOrders, totalRemaining),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pendingBatches > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.kaariger_pending_batches, pendingBatches),
                        color = Color(0xFFB45309),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (recentOrders.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.kaariger_recent_orders), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onViewAllOrders) { Text(stringResource(R.string.kaariger_view_all)) }
            }
            recentOrders.forEach { order ->
                KaarigerOrderCard(
                    order = order,
                    onClick = onViewAllOrders,
                    onSubmitDelivery = onViewAllOrders,
                    onReportMaterials = onViewAllOrders,
                    compact = true
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.kaariger_how_it_works), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.kaariger_step_1))
                Text(stringResource(R.string.kaariger_step_2))
                Text(stringResource(R.string.kaariger_step_3))
                Text(stringResource(R.string.kaariger_step_4))
            }
        }
    }
}

private sealed class KaarigerNav(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Home : KaarigerNav("kaariger_home", R.string.kaariger_nav_home, Icons.Default.Home)
    object Orders : KaarigerNav("kaariger_orders", R.string.kaariger_nav_orders, Icons.Default.Task)
    object Payments : KaarigerNav("kaariger_payments", R.string.kaariger_nav_payments, Icons.Default.Payments)
}
