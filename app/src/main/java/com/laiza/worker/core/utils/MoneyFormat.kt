package com.laiza.worker.core.utils

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val indianLocale = Locale("en", "IN")

/**
 * Format as ₹1,20,000 or ₹16,485.5 when there are paise.
 * Keeps up to 2 decimal places; omits trailing .00 for whole rupees.
 */
fun formatIndianRupee(amount: Number): String {
    val value = amount.toDouble()
    val rounded = (value * 100.0).roundToLong() / 100.0
    val isWhole = abs(rounded - rounded.roundToLong()) < 0.000_5
    val format = NumberFormat.getNumberInstance(indianLocale).apply {
        if (isWhole) {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        } else {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
    }
    return "₹${format.format(rounded)}"
}
