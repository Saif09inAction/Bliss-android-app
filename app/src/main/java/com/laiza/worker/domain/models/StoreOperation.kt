package com.laiza.worker.domain.models

import java.util.UUID

data class PickupRecord(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val color: String,
    val quantity: Int,
    val partner: EcommercePartner,
    val staffId: String,
    val staffName: String,
    val date: String,
    val time: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ReturnRecord(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val color: String,
    val quantity: Int,
    val partner: EcommercePartner,
    val returnType: ReturnType,
    val staffId: String,
    val staffName: String,
    val date: String,
    val time: String,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class EcommercePartner(val displayName: String) {
    FLIPKART("Flipkart"),
    MYNTRA("Myntra"),
    AMAZON("Amazon"),
    MEESHO("Meesho"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): EcommercePartner {
            return entries.find { it.name == value || it.displayName.equals(value, ignoreCase = true) }
                ?: OTHER
        }
    }
}

enum class ReturnType(val displayName: String) {
    RTO("RTO"),
    DTO("DTO");

    companion object {
        fun fromString(value: String): ReturnType {
            return entries.find { it.name == value } ?: RTO
        }
    }
}
