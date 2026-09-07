package com.laiza.worker.domain.models

/** Product from Firestore `product_catalog` (name + optional ₹/pc). */
data class CatalogProduct(
    val id: String,
    val name: String,
    val price: Double = 0.0
)
