package com.laiza.worker.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiza.worker.core.theme.BlissGold
import com.laiza.worker.core.theme.BlissGreen
import com.laiza.worker.core.theme.BlissGreenLight

/**
 * Vector replica of the interlocked Bliss Bombay "BB" monogram (gold strokes).
 */
@Composable
fun BlissBBMonogram(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val stroke = minOf(w, h) * 0.088f
            val inset = stroke * 0.9f
            val top = inset
            val bottom = h - inset
            val mid = h / 2f
            val gold = Brush.linearGradient(
                colors = listOf(Color(0xFFF5E6A8), Color(0xFFD4AF37), Color(0xFFB8860B))
            )

            fun bPath(offsetX: Float): Path {
                val p = Path()
                val left = offsetX
                val right = offsetX + w * 0.36f
                p.moveTo(left, top)
                p.lineTo(left, bottom)
                p.moveTo(left, top)
                p.cubicTo(right, top, right, mid - h * 0.05f, left + w * 0.015f, mid)
                p.cubicTo(right + w * 0.035f, mid, right, bottom, left, bottom)
                return p
            }

            drawPath(
                path = bPath(w * 0.11f),
                brush = gold,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawPath(
                path = bPath(w * 0.51f),
                brush = gold,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawRoundRect(
                brush = gold,
                topLeft = Offset(w * 0.37f, h * 0.43f),
                size = Size(w * 0.26f, stroke * 0.85f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 0.85f)
            )
        }
    }
}

@Composable
fun BlissLogoImage(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    small: Boolean = false
) {
    BlissBBMonogram(
        modifier = modifier,
        size = size
    )
}

@Composable
fun BlissBrandTitle(
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 28.sp,
    subtitleSize: TextUnit = 11.sp,
    titleColor: Color = BlissGreenLight,
    subtitleColor: Color = BlissGold,
    showSubtitle: Boolean = true,
    onDark: Boolean = true
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "BLISS",
            fontSize = titleSize,
            fontWeight = FontWeight.Black,
            color = if (onDark) BlissGreenLight else BlissGreen,
            letterSpacing = 4.sp
        )
        if (showSubtitle) {
            Text(
                text = "BOMBAY",
                fontSize = subtitleSize,
                fontWeight = FontWeight.Bold,
                color = subtitleColor,
                letterSpacing = 6.sp
            )
        }
    }
}

@Composable
fun BlissSplashBrand(
    modifier: Modifier = Modifier,
    monogramSize: Dp = 100.dp
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BlissBBMonogram(size = monogramSize)
        Spacer(modifier = Modifier.height(20.dp))
        BlissBrandTitle(
            titleSize = 36.sp,
            subtitleSize = 14.sp,
            onDark = true
        )
    }
}

@Composable
fun BlissLogoWithTitle(
    modifier: Modifier = Modifier,
    logoSize: Dp = 88.dp,
    onDark: Boolean = true
) {
    BlissSplashBrand(modifier = modifier, monogramSize = logoSize)
}
