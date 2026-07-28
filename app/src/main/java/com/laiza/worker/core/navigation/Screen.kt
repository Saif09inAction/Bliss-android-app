package com.laiza.worker.core.navigation

sealed class Screen(val route: String) {
    // Nested graphs
    object AuthGraph : Screen("auth_graph")
    object StaffGraph : Screen("staff_graph")
    object KaarigerGraph : Screen("kaariger_graph")

    // Legacy alias
    object MainGraph : Screen("staff_graph")

    // Auth Screens
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object ForgotPassword : Screen("forgot_password")

    // Main / Bottom Navigation Screens
    object Dashboard : Screen("dashboard")
    object KaarigerDashboard : Screen("kaariger_dashboard")
    object Attendance : Screen("attendance")
    object Production : Screen("production")
    object Salary : Screen("salary")
    object Profile : Screen("profile")
    object Inventory : Screen("inventory")
    object Notifications : Screen("notifications")
    object Help : Screen("help")
    object Advance : Screen("advance")
    object About : Screen("about")
    object Settings : Screen("settings")

    // Attendance Flow Sub-Screens
    object AttendanceCamera : Screen("attendance/camera/{punchType}")
    object AttendancePreview : Screen("attendance/preview/{punchType}")
    object AttendanceSuccess : Screen("attendance/success/{punchType}")

    // Work Assignment Sub-Screens
    object WorkDetail : Screen("work/detail/{workOrderId}")
    object WorkUpdateProgress : Screen("work/progress/{workOrderId}")
}
