package com.laiza.worker.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiza.worker.R
import com.laiza.worker.core.theme.BlissGold
import com.laiza.worker.core.theme.BlissLime

@Composable
fun BlissLogoImage(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    small: Boolean = false
) {
    Image(
        painter = painterResource(if (small) R.drawable.bliss_logo_sm else R.drawable.bliss_logo),
        contentDescription = "Bliss Bombay",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun BlissBrandTitle(
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 28.sp,
    subtitleSize: TextUnit = 11.sp,
    titleColor: Color = BlissLime,
    subtitleColor: Color = BlissGold,
    showSubtitle: Boolean = true
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "BLISS",
            fontSize = titleSize,
            fontWeight = FontWeight.Black,
            color = titleColor,
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
fun BlissLogoWithTitle(
    modifier: Modifier = Modifier,
    logoSize: Dp = 88.dp,
    onDark: Boolean = true
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BlissLogoImage(size = logoSize)
        Spacer(modifier = Modifier.height(16.dp))
        BlissBrandTitle(
            titleColor = if (onDark) BlissLime else MaterialTheme.colorScheme.primary,
            subtitleColor = if (onDark) BlissGold else BlissGold
        )
    }
}
