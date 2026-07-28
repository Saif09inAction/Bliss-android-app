package com.laiza.worker.presentation.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

/**
 * Reusable premium tactile feedback and click physics modifier.
 * Automatically scales down to 97% on press, reduces opacity slightly,
 * triggers high-quality haptics on target devices, and releases with a snappy spring.
 */
fun Modifier.premiumClickable(
    hapticType: String = "light", // "light", "medium", "strong"
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 120Hz-like spring scale animation (120-180ms duration equivalent)
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "premium_click_scale"
    )

    // Slight opacity transition during press
    val opacity by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "premium_click_opacity"
    )

    // Haptics trigger on press start
    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            triggerHaptic(view, hapticType)
        }
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = opacity
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Trigger crisp, system-level haptic ticks using standard Android SDK HapticFeedbackConstants.
 */
fun triggerHaptic(view: View, type: String) {
    when (type.lowercase()) {
        "light" -> com.laiza.worker.core.haptics.HapticManager.light(view)
        "medium" -> com.laiza.worker.core.haptics.HapticManager.medium(view)
        "strong" -> com.laiza.worker.core.haptics.HapticManager.strong(view)
        "success" -> com.laiza.worker.core.haptics.HapticManager.success(view)
        "error" -> com.laiza.worker.core.haptics.HapticManager.error(view)
        else -> com.laiza.worker.core.haptics.HapticManager.light(view)
    }
}
