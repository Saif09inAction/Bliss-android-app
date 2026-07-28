package com.laiza.worker.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.laiza.worker.presentation.viewmodels.AttendanceViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.laiza.worker.presentation.screens.AttendanceCameraScreen
import com.laiza.worker.presentation.screens.AttendanceHomeScreen
import com.laiza.worker.presentation.screens.AttendancePreviewScreen
import com.laiza.worker.presentation.screens.AttendanceSuccessScreen
import com.laiza.worker.presentation.screens.LoginScreen
import com.laiza.worker.presentation.screens.WelcomeScreen
import com.laiza.worker.presentation.screens.StaffContainerScreen
import com.laiza.worker.presentation.screens.KaarigerContainerScreen
import com.laiza.worker.presentation.screens.PlaceholderScreen
import com.laiza.worker.presentation.screens.SplashScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.AuthGraph.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it / 6 }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 6 }, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350))
        }
    ) {
        navigation(
            startDestination = Screen.Splash.route,
            route = Screen.AuthGraph.route
        ) {
            composable(route = Screen.Splash.route) {
                SplashScreen(navController = navController)
            }
            composable(route = Screen.Welcome.route) {
                WelcomeScreen(navController = navController)
            }
            composable(route = Screen.Login.route) {
                LoginScreen(navController = navController)
            }
            composable(route = Screen.ForgotPassword.route) {
                MockScreen(name = "ForgotPassword Screen (Placeholder)")
            }
        }

        navigation(
            startDestination = Screen.Dashboard.route,
            route = Screen.StaffGraph.route
        ) {
            composable(route = Screen.Dashboard.route) {
                StaffContainerScreen(rootNavController = navController)
            }
        }

        navigation(
            startDestination = Screen.KaarigerDashboard.route,
            route = Screen.KaarigerGraph.route
        ) {
            composable(route = Screen.KaarigerDashboard.route) {
                KaarigerContainerScreen(rootNavController = navController)
            }
        }

        composable(route = Screen.Attendance.route) {
            AttendanceHomeScreen(navController = navController)
        }
        composable(route = Screen.AttendanceCamera.route) { backStackEntry ->
            val punchType = backStackEntry.arguments?.getString("punchType") ?: ""
            AttendanceCameraScreen(navController = navController, punchType = punchType)
        }
        composable(route = Screen.AttendancePreview.route) { backStackEntry ->
            val punchType = backStackEntry.arguments?.getString("punchType") ?: ""
            AttendancePreviewScreen(navController = navController, punchType = punchType)
        }
        composable(route = Screen.AttendanceSuccess.route) { backStackEntry ->
            val punchType = backStackEntry.arguments?.getString("punchType") ?: ""
            AttendanceSuccessScreen(navController = navController, punchType = punchType)
        }
        composable(route = Screen.Production.route) {
            PlaceholderScreen(title = "Work Assignment", icon = Icons.Default.Task)
        }
        composable(route = Screen.Salary.route) {
            PlaceholderScreen(title = "Salary Summary", icon = Icons.Default.Payments)
        }
        composable(route = Screen.Profile.route) {
            PlaceholderScreen(title = "Employee Profile", icon = Icons.Default.Person)
        }
        composable(route = Screen.Inventory.route) {
            PlaceholderScreen(title = "Inventory Details", icon = Icons.Default.Inventory)
        }
        composable(route = Screen.Notifications.route) {
            PlaceholderScreen(title = "Notifications Hub", icon = Icons.Default.Notifications)
        }
        composable(route = Screen.Help.route) {
            PlaceholderScreen(title = "Help & Support", icon = Icons.Default.Help)
        }
        composable(route = Screen.Advance.route) {
            PlaceholderScreen(title = "Advance Payments", icon = Icons.Default.AccountBalanceWallet)
        }
        composable(route = Screen.About.route) {
            PlaceholderScreen(title = "About Bliss Bombay", icon = Icons.Default.Info)
        }
        composable(route = Screen.Settings.route) {
            PlaceholderScreen(title = "Settings", icon = Icons.Default.Settings)
        }
    }
}

@Composable
private fun MockScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name)
    }
}
