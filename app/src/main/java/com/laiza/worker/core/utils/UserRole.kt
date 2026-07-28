package com.laiza.worker.core.utils

object UserRole {
    const val ADMIN = "Admin"
    const val WORKER = "Worker"
    const val SUPERVISOR = "Supervisor"
    const val KARIGAR = "Karigar"
    const val STORE_MANAGER = "Store Manager"

    fun isAdmin(role: String): Boolean = role.equals(ADMIN, ignoreCase = true)
}
