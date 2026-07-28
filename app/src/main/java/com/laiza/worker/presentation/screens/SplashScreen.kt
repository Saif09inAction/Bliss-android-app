package com.laiza.worker.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laiza.worker.core.navigation.Screen
import com.laiza.worker.core.theme.BlissGold
import com.laiza.worker.core.theme.BlissLime
import com.laiza.worker.presentation.components.BlissLogoImage
import com.laiza.worker.presentation.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(key1 = true) {
        alpha.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        scale.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        pulseScale.animateTo(
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(key1 = true) {
        delay(2600)
        val activeSession = viewModel.resolveStartupSession()
        if (activeSession != null) {
            navController.navigate(viewModel.homeRouteForRole(activeSession.role)) {
                popUpTo(Screen.AuthGraph.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Welcome.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A0A), Color(0xFF121417), Color(0xFF1A1F14))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value * pulseScale.value)
                .alpha(alpha.value)
        ) {
            BlissLogoImage(size = 120.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BLISS",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = BlissLime,
                letterSpacing = 6.sp
            )
            Text(
                text = "BOMBAY",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BlissGold,
                letterSpacing = 8.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Premium Quality & Style",
                fontSize = 13.sp,
                color = BlissGold.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }
    }
}
