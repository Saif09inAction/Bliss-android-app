package com.laiza.worker.domain.models

data class UserSession(
    val uid: String,
    val name: String,
    val phone: String,
    val role: Role,
    val token: String? = null
)

enum class Role {
    ADMIN, STAFF, KAARIGER;

    companion object {
        fun fromFirestore(value: String?): Role {
            return when (value?.uppercase()) {
                "ADMIN" -> ADMIN
                "KAARIGER", "KARIGAR" -> KAARIGER
                else -> STAFF
            }
        }
    }
}
