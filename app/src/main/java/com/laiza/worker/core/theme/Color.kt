package com.laiza.worker.core.theme

import androidx.compose.ui.graphics.Color

// ── Bliss Bombay brand palette ──
val BlissBlack = Color(0xFF0A0A0A)
val BlissDark = Color(0xFF121417)
val BlissDarkElevated = Color(0xFF1A1D22)
val BlissLime = Color(0xFFC8FF00)
val BlissLimeMuted = Color(0xFF9ECC00)
val BlissGold = Color(0xFFD4AF37)
val BlissGoldLight = Color(0xFFF5E6A8)
val BlissCream = Color(0xFFFAFAF8)
val BlissWarmWhite = Color(0xFFF5F3EF)

// Light theme
val LightPrimary = BlissLime
val LightOnPrimary = BlissBlack
val LightSecondary = BlissGold
val LightOnSecondary = BlissBlack
val LightBackground = BlissCream
val LightOnBackground = BlissDark
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = BlissDark

// Dark theme
val DarkPrimary = BlissLime
val DarkOnPrimary = BlissBlack
val DarkSecondary = BlissGold
val DarkOnSecondary = BlissBlack
val DarkBackground = BlissBlack
val DarkOnBackground = Color(0xFFF5F5F5)
val DarkSurface = BlissDarkElevated
val DarkOnSurface = Color(0xFFF5F5F5)

// Semantic
val SuccessColor = Color(0xFF22C55E)
val WarningColor = Color(0xFFF59E0B)
val ErrorColor = Color(0xFFEF4444)
val InfoColor = BlissGold

// Gradients
val BlissHeroGradient = listOf(BlissBlack, BlissDark, Color(0xFF1E2418))
val BlissAccentGradient = listOf(BlissLime.copy(alpha = 0.15f), BlissGold.copy(alpha = 0.08f))
val BlissDrawerGradient = listOf(BlissBlack, Color(0xFF151A10))
