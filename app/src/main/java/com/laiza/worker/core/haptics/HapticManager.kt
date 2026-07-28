package com.laiza.worker.core.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object HapticManager {
    fun light(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun medium(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun strong(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun success(view: View) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                view.postDelayed({
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }, 100L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun error(view: View) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
