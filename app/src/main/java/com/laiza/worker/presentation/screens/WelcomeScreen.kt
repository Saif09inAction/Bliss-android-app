package com.laiza.worker.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.laiza.worker.core.navigation.Screen
import com.laiza.worker.presentation.components.BlissSplashBrand
import com.laiza.worker.core.theme.BlissGold
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WelcomeScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()

    // Slider bounds
    val density = LocalDensity.current
    val trackWidthDp = 280.dp
    val handleSizeDp = 56.dp
    val trackWidthPx = with(density) { trackWidthDp.toPx() }
    val handleSizePx = with(density) { handleSizeDp.toPx() }
    val maxDragOffset = trackWidthPx - handleSizePx

    // Custom slider dragging offset state
    val dragOffset = remember { Animatable(0f) }

    // Infinite transitions for micro-animations
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_animations")

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val arrowHintOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_hint"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF151A10)
                    )
                )
            )
    ) {
        // Centering Container for main visual hierarchy
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center)
        ) {
            BlissSplashBrand(
                modifier = Modifier.graphicsLayer(scaleX = logoScale, scaleY = logoScale),
                monogramSize = 100.dp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Premium Quality & Timeless Handbags",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = BlissGold.copy(alpha = 0.8f),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }

        // Bottom Container for action slider
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Premium Swipe to Continue Track
            Box(
                modifier = Modifier
                    .width(trackWidthDp)
                    .height(64.dp)
                    .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(100.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF22C55E).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(100.dp)
                    )
                    .clip(RoundedCornerShape(100.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                // Background Track text
                Text(
                    text = "Swipe to Continue",
                    color = Color(0xFF22C55E).copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Dragging Capsule handle
                Box(
                    modifier = Modifier
                        .offset {
                            val activeArrowOffset = if (dragOffset.value == 0f) arrowHintOffset else 0f
                            IntOffset((dragOffset.value + activeArrowOffset).roundToInt(), 0)
                        }
                        .size(handleSizeDp)
                        .padding(4.dp)
                        .background(Color(0xFF22C55E), shape = CircleShape)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragOffset.value >= maxDragOffset * 0.7f) {
                                        coroutineScope.launch {
                                            dragOffset.animateTo(maxDragOffset)
                                            navController.navigate(Screen.Login.route) {
                                                popUpTo(Screen.Welcome.route) { inclusive = true }
                                            }
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            dragOffset.animateTo(0f)
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(0f)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        val newOffset = (dragOffset.value + dragAmount).coerceIn(0f, maxDragOffset)
                                        dragOffset.snapTo(newOffset)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Swipe Arrow",
                        tint = Color(0xFF0A0A0A),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "built by Saif Salmani",
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF22C55E).copy(alpha = 0.35f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
