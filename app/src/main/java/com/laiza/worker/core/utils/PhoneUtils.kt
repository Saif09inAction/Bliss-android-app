package com.laiza.worker.core.utils

/**
 * Normalizes phone numbers by stripping whitespace, hyphens, and +91/91 country codes.
 */
fun normalizePhone(phone: String?): String {
    if (phone == null) return ""
    var p = phone.trim().replace(Regex("[\\s-]"), "")
    if (p.startsWith("+91")) p = p.removePrefix("+91")
    else if (p.startsWith("91") && p.length > 10) p = p.removePrefix("91")
    // Keep last 10 digits when value is an Indian mobile with extra prefix.
    if (p.length > 10 && p.all { it.isDigit() }) p = p.takeLast(10)
    return p
}

/**
 * Robust phone equality check that matches identical normalized numbers
 * and falls back to last-10-digit matching for Indian mobile numbers.
 */
fun phonesMatch(a: String?, b: String?): Boolean {
    val na = normalizePhone(a)
    val nb = normalizePhone(b)
    if (na.isEmpty() || nb.isEmpty()) return false
    if (na == nb) return true
    if (na.length >= 10 && nb.length >= 10) return na.takeLast(10) == nb.takeLast(10)
    return false
}
