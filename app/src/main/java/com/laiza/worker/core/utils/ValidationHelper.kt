package com.laiza.worker.core.utils

import android.util.Patterns

object ValidationHelper {

    fun isValidPhoneNumber(phone: String): Boolean {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return false
        // Indian phone number pattern (10 digits starting with 6, 7, 8, or 9)
        val indianPhonePattern = "^[6-9]\\d{9}$"
        return trimmed.matches(Regex(indianPhonePattern))
    }

    fun isValidPassword(password: String): Boolean {
        // ERP production rule: minimum 6 characters for worker passwords
        return password.length >= 6
    }

    fun isRequiredFieldNotEmpty(value: String): Boolean {
        return value.trim().isNotEmpty()
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }
}
