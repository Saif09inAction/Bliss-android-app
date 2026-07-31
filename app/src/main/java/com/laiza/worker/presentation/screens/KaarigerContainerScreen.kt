package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import com.laiza.worker.core.theme.BlissBlack
import com.laiza.worker.core.theme.BlissGold
import com.laiza.worker.core.theme.BlissLime
import com.laiza.worker.presentation.components.KaarigerLocalizedContent
import com.laiza.worker.presentation.components.LaizaTopAppBar
import com.laiza.worker.presentation.components.PremiumCard
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
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderStatus
import com.laiza.worker.presentation.components.ConfirmationDialog
import com.laiza.worker.presentation.components.DrawerHeader
import com.laiza.worker.presentation.components.DrawerItem
import com.laiza.worker.presentation.components.KaarigerLanguageSwitch
import com.laiza.worker.presentation.components.BlissFloatingBottomNav
import com.laiza.worker.presentation.components.BlissNavTab
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
    val payments by orderViewModel.kaarigerPayments.collectAsState()
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
                val onHome = currentRoute == KaarigerNav.Home.route
                LaizaTopAppBar(
                    title = when (currentRoute) {
                        KaarigerNav.Home.route -> stringResource(R.string.kaariger_title_dashboard)
                        KaarigerNav.Orders.route -> stringResource(R.string.kaariger_title_orders)
                        KaarigerNav.Payments.route -> stringResource(R.string.kaariger_title_payments)
                        else -> stringResource(R.string.kaariger_title_app)
                    },
                    subtitle = session?.name,
                    showBackButton = !onHome,
                    showMenuButton = true,
                    onBackClick = {
                        childNavController.navigate(KaarigerNav.Home.route) {
                            popUpTo(KaarigerNav.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                BlissFloatingBottomNav(
                    tabs = listOf(
                        BlissNavTab(KaarigerNav.Home.route, stringResource(R.string.kaariger_nav_home), Icons.Default.Home),
                        BlissNavTab(KaarigerNav.Orders.route, stringResource(R.string.kaariger_nav_orders), Icons.Default.Task),
                        BlissNavTab(KaarigerNav.Payments.route, stringResource(R.string.kaariger_nav_payments), Icons.Default.Payments)
                    ),
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        if (currentRoute != route) {
                            childNavController.navigate(route) {
                                popUpTo(KaarigerNav.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = childNavController,
                startDestination = KaarigerNav.Home.route,
                modifier = Modifier.padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                )
            ) {
                composable(KaarigerNav.Home.route) {
                    KaarigerDashboardContent(
                        name = session?.name ?: stringResource(R.string.kaariger_default_name),
                        orders = orders,
                        payments = payments,
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
    payments: List<KaarigerOrderPayment>,
    onViewAllOrders: () -> Unit
) {
    val activeOrders = orders.count { it.status != OrderStatus.COMPLETED }
    val pendingBatches = orders.count { it.status == OrderStatus.PENDING_APPROVAL }
    val totalRemaining = orders.sumOf { it.remainingQuantity() }
    val recentOrders = orders.take(3)

    // Runner/Fitting/Astar/Material quantities given by admin, only for bills not yet fully
    // paid off — resets to zero automatically once an order's whole payment is received.
    val pendingDeductions = remember(orders, payments) {
        val unsettled = orders.filter { order ->
            if (order.status == OrderStatus.REJECTED) return@filter false
            val netDeal = (order.originalDealAmount ?: order.totalDealAmount) - order.repairDeductionTotal
            val totalPaid = payments.filter { it.orderId == order.id }.sumOf { it.amount }
            (netDeal - totalPaid) > 0.0
        }
        val allDeductions = unsettled.flatMap { it.materialDeductions }
        val materialsByName = allDeductions
            .filter { it.type == "MATERIAL" }
            .groupBy { it.label.ifBlank { "Material" } }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
            .filter { it.value > 0 }
            .toList()
            .sortedByDescending { it.second }
        DeductionsSummary(
            runner = allDeductions.filter { it.type == "RUNNER" }.sumOf { it.quantity },
            fitting = allDeductions.filter { it.type == "FITTING" }.sumOf { it.quantity },
            astar = allDeductions.filter { it.type == "ASTAR" }.sumOf { it.quantity },
            materials = materialsByName
        )
    }
    val hasPendingDeductions = pendingDeductions.runner > 0 || pendingDeductions.fitting > 0 ||
        pendingDeductions.astar > 0 || pendingDeductions.materials.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(BlissBlack, Color(0xFF1A1F14)))
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        stringResource(R.string.kaariger_welcome, name),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = BlissLime
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.kaariger_active_summary, activeOrders, totalRemaining),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    if (pendingBatches > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.kaariger_pending_batches, pendingBatches),
                            color = BlissGold,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (hasPendingDeductions) {
            PendingDeductionsCard(pendingDeductions)
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
                    onReportMaterials = onViewAllOrders,
                    compact = true
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
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

private data class DeductionsSummary(
    val runner: Int,
    val fitting: Int,
    val astar: Int,
    /** Material name → quantity. Only materials actually given are listed — nothing generic. */
    val materials: List<Pair<String, Int>>
)

@Composable
private fun PendingDeductionsCard(summary: DeductionsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Runner / Fitting / Astar / Material",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC2410C),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DeductionChip("Runner", summary.runner, Modifier.weight(1f))
                DeductionChip("Fitting", summary.fitting, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DeductionChip("Astar", summary.astar, Modifier.weight(1f))
            }
            if (summary.materials.isNotEmpty()) {
                summary.materials.chunked(2).forEach { pair ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { (name, qty) ->
                            DeductionChip(name, qty, Modifier.weight(1f))
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeductionChip(label: String, quantity: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9A3412))
            Text("$quantity pcs", fontWeight = FontWeight.Bold, color = Color(0xFFC2410C), style = MaterialTheme.typography.titleSmall)
        }
    }
}

private sealed class KaarigerNav(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Home : KaarigerNav("kaariger_home", R.string.kaariger_nav_home, Icons.Default.Home)
    object Orders : KaarigerNav("kaariger_orders", R.string.kaariger_nav_orders, Icons.Default.Task)
    object Payments : KaarigerNav("kaariger_payments", R.string.kaariger_nav_payments, Icons.Default.Payments)
}
