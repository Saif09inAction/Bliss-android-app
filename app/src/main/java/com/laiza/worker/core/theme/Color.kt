package com.laiza.worker.core.theme

import androidx.compose.ui.graphics.Color

// ── Bliss Bombay brand palette (readable green + gold, no neon) ──
val BlissBlack = Color(0xFF0A0A0A)
val BlissDark = Color(0xFF121417)
val BlissDarkElevated = Color(0xFF1A1D22)
val BlissGreen = Color(0xFF15803D)
val BlissGreenLight = Color(0xFF22C55E)
val BlissGreenDark = Color(0xFF14532D)
val BlissGreenSurface = Color(0xFFECFDF3)
/** @deprecated Use BlissGreenLight — kept for gradual migration */
val BlissLime = BlissGreenLight
val BlissLimeMuted = Color(0xFF166534)
val BlissGold = Color(0xFFD4AF37)
val BlissGoldLight = Color(0xFFF5E6A8)
val BlissGoldDark = Color(0xFFB8860B)
val BlissCream = Color(0xFFFAFAF8)
val BlissWarmWhite = Color(0xFFF5F3EF)

// Light theme
val LightPrimary = BlissGreen
val LightOnPrimary = Color(0xFFFFFFFF)
val LightSecondary = BlissGold
val LightOnSecondary = BlissBlack
val LightBackground = BlissCream
val LightOnBackground = BlissDark
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = BlissDark

// Dark theme
val DarkPrimary = BlissGreenLight
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
val BlissHeroGradient = listOf(BlissGreenDark, BlissGreen, Color(0xFF166534))
val BlissAccentGradient = listOf(BlissGreen.copy(alpha = 0.12f), BlissGold.copy(alpha = 0.08f))
val BlissDrawerGradient = listOf(BlissBlack, Color(0xFF0F1A12))
