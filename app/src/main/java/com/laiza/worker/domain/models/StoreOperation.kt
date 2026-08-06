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
    /** Total pcs = clarisQuantity + blissQuantity (kept for older docs / CSV). */
    val quantity: Int = 0,
    /** Qty under Claris entity. */
    val clarisQuantity: Int = 0,
    /** Qty under Bliss entity. */
    val blissQuantity: Int = 0,
    /** Marketplace / company — Amazon, Flipkart, Meesho, etc. */
    val partner: String = EcommercePlatform.FLIPKART,
    /** Courier / delivery partner — Amazon Delivery, eKart, etc. */
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

    val totalQuantity: Int
        get() {
            val split = clarisQuantity + blissQuantity
            if (split > 0) return split
            return lineItems.takeIf { it.isNotEmpty() }?.sumOf { it.quantity } ?: quantity
        }

    val qtyBreakdownLabel: String
        get() = OwnerQty.breakdownLabel(clarisQuantity, blissQuantity, totalQuantity)

    val productsLabel: String
        get() {
            val lines = lineItems
            return when {
                lines.isEmpty() -> productName.ifBlank { "Pickup" }
                lines.size == 1 -> lines.first().productName
                else -> "${lines.size} products"
            }
        }
}

data class ReturnRecord(
    val id: String = UUID.randomUUID().toString(),
    val productId: String = "",
    val productName: String = "",
    val color: String = "",
    val quantity: Int = 0,
    val clarisQuantity: Int = 0,
    val blissQuantity: Int = 0,
    val partner: String = EcommercePlatform.FLIPKART,
    val deliveryPartner: String = "",
    val returnType: ReturnType = ReturnType.RTO,
    val staffId: String = "",
    val staffName: String = "",
    val date: String = "",
    val time: String = "",
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalQuantity: Int
        get() {
            val split = clarisQuantity + blissQuantity
            return if (split > 0) split else quantity
        }

    val qtyBreakdownLabel: String
        get() = OwnerQty.breakdownLabel(clarisQuantity, blissQuantity, totalQuantity)
}

/** Shared Claris / Bliss quantity helpers for pickup & return. */
object OwnerQty {
    fun breakdownLabel(claris: Int, bliss: Int, total: Int): String {
        val parts = buildList {
            if (claris > 0) add("Claris $claris")
            if (bliss > 0) add("Bliss $bliss")
        }
        return when {
            parts.isNotEmpty() -> parts.joinToString(" · ")
            total > 0 -> "$total pcs"
            else -> "0 pcs"
        }
    }
}

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
    const val AMAZON_DELIVERY = "Amazon Delivery"
    const val EKART = "eKart"

    val ALL = listOf(
        AMAZON_DELIVERY,
        EKART,
        "BlueDart",
        "Shiprocket",
        "Delhivery",
        "DTDC",
        "Ecom Express",
        "Xpressbees",
        "Shadowfax",
        "India Post",
        "Valmo"
    )

    /** Normalize legacy courier names from older records. */
    fun normalize(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        return when (raw.lowercase()) {
            "amazon shipping", "amazon shipping services" -> AMAZON_DELIVERY
            "ekart", "e-kart", "e kart" -> EKART
            else -> raw
        }
    }
}

/**
 * Which couriers are commonly used by each marketplace / company.
 * Custom couriers staff typed in stay visible for every company.
 */
object PlatformDeliveryPartners {
    private val MAP: Map<String, List<String>> = mapOf(
        EcommercePlatform.AMAZON to listOf(
            DeliveryPartnerDefaults.AMAZON_DELIVERY, "BlueDart", "Xpressbees", "Ecom Express"
        ),
        EcommercePlatform.FLIPKART to listOf(
            DeliveryPartnerDefaults.EKART, "Delhivery", "Xpressbees", "Ecom Express"
        ),
        EcommercePlatform.MEESHO to listOf("Valmo", "Delhivery", "Xpressbees", "Ecom Express"),
        EcommercePlatform.SNAPDEAL to listOf("Delhivery", "Ecom Express", "DTDC"),
        EcommercePlatform.MYNTRA to listOf("Xpressbees", "Ecom Express", "Delhivery"),
        EcommercePlatform.AJIO to listOf("Delhivery", "Xpressbees"),
        EcommercePlatform.NYKAA to listOf("BlueDart", "Delhivery"),
        EcommercePlatform.OTHER to DeliveryPartnerDefaults.ALL
    )

    private val CLASSIFIED: Set<String> = MAP.values.flatten().map { it.lowercase() }.toSet() +
        setOf("amazon shipping", "ekart", "e-kart")

    fun forPlatform(platform: String): List<String> = MAP[platform] ?: DeliveryPartnerDefaults.ALL

    fun isRelevant(courierName: String, platform: String): Boolean {
        val normalized = DeliveryPartnerDefaults.normalize(courierName)
        val lower = normalized.lowercase()
        if (lower !in CLASSIFIED && courierName.trim().lowercase() !in CLASSIFIED) return true
        return forPlatform(platform).any { it.equals(normalized, ignoreCase = true) }
    }
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
