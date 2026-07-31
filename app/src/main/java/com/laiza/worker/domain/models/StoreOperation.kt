package com.laiza.worker.domain.models

import java.util.UUID

data class PickupLineItem(
    val productId: String,
    val productName: String,
    val color: String,
    val quantity: Int
)

data class PickupRecord(
    val id: String = UUID.randomUUID().toString(),
    val items: List<PickupLineItem> = emptyList(),
    val productId: String = "",
    val productName: String = "",
    val color: String = "",
    val quantity: Int = 0,
    /** Marketplace / platform — Amazon, Flipkart, Meesho, etc. */
    val partner: String = EcommercePlatform.FLIPKART,
    /** Courier / delivery partner — BlueDart, Shiprocket, etc. */
    val deliveryPartner: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val date: String = "",
    val time: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val lineItems: List<PickupLineItem>
        get() = if (items.isNotEmpty()) items
        else if (productId.isNotBlank() || productName.isNotBlank()) {
            listOf(
                PickupLineItem(
                    productId = productId,
                    productName = productName,
                    color = color,
                    quantity = quantity
                )
            )
        } else emptyList()

    val totalQuantity: Int get() = lineItems.sumOf { it.quantity }

    val productsLabel: String
        get() {
            val lines = lineItems
            return when {
                lines.isEmpty() -> productName.ifBlank { "—" }
                lines.size == 1 -> lines.first().productName
                else -> "${lines.size} products"
            }
        }
}

data class ReturnRecord(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val color: String,
    val quantity: Int,
    val partner: String = EcommercePlatform.FLIPKART,
    val deliveryPartner: String = "",
    val returnType: ReturnType,
    val staffId: String,
    val staffName: String,
    val date: String,
    val time: String,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/** Marketplace / e-commerce platforms staff hand off to. */
object EcommercePlatform {
    const val FLIPKART = "Flipkart"
    const val MYNTRA = "Myntra"
    const val AMAZON = "Amazon"
    const val MEESHO = "Meesho"
    const val SNAPDEAL = "Snapdeal"
    const val AJIO = "Ajio"
    const val NYKAA = "Nykaa"
    const val OTHER = "Other"

    val DEFAULTS = listOf(
        AMAZON, FLIPKART, MYNTRA, MEESHO, SNAPDEAL, AJIO, NYKAA, OTHER
    )

    fun normalize(value: String?): String {
        if (value.isNullOrBlank()) return FLIPKART
        // Legacy enum names stored as FLIPKART / AMAZON
        return when (value.trim().uppercase()) {
            "FLIPKART" -> FLIPKART
            "MYNTRA" -> MYNTRA
            "AMAZON" -> AMAZON
            "MEESHO" -> MEESHO
            "SNAPDEAL" -> SNAPDEAL
            "AJIO" -> AJIO
            "NYKAA" -> NYKAA
            "OTHER" -> OTHER
            else -> value.trim()
        }
    }
}

/** Built-in courier suggestions; staff can also add custom ones to Firestore. */
object DeliveryPartnerDefaults {
    val ALL = listOf(
        "BlueDart",
        "Shiprocket",
        "Delhivery",
        "DTDC",
        "Ecom Express",
        "Xpressbees",
        "Shadowfax",
        "India Post",
        "Ekart",
        "Amazon Shipping"
    )
}

data class DeliveryPartner(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReturnType(val displayName: String) {
    RTO("RTO"),
    DTO("DTO");

    companion object {
        fun fromString(value: String): ReturnType {
            return entries.find { it.name == value } ?: RTO
        }
    }
}
