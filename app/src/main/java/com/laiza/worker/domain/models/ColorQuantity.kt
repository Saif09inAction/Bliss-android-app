package com.laiza.worker.domain.models

/** Color + quantity line for staff approval / inventory variants. */
data class ColorQuantity(
    val color: String,
    val quantity: Int
)

object ProductColors {
    val PRESETS = listOf(
        "Red",
        "Blue",
        "Green",
        "Black",
        "White",
        "Yellow",
        "Pink"
    )

    fun normalizeSku(name: String): String = name.trim().lowercase()

    fun normalizeColor(color: String): String = color.trim()
}
