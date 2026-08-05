package com.laiza.worker.core.utils

import java.text.NumberFormat
import java.util.Locale

private val indianNumberFormat: NumberFormat =
    NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

/** Format as ₹1,20,000 (Indian grouping). */
fun formatIndianRupee(amount: Number): String {
    val value = when (amount) {
        is Double -> amount.toLong()
        is Float -> amount.toLong()
        else -> amount.toLong()
    }
    return "₹${indianNumberFormat.format(value)}"
}
