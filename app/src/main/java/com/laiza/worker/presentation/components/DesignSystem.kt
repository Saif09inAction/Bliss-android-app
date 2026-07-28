package com.laiza.worker.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiza.worker.core.theme.BlissGold
import com.laiza.worker.core.theme.BlissGreen
import com.laiza.worker.core.theme.BlissGreenLight

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.premiumClickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = BlissGold.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        border = border ?: BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    BlissGold.copy(alpha = 0.45f),
                    BlissGreenLight.copy(alpha = 0.15f),
                    BlissGold.copy(alpha = 0.25f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun M3StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, borderColor) = when (status.uppercase()) {
        "ON TIME", "PRESENT", "COMPLETED", "ACTIVE", "PAID", "APPROVED" -> Triple(
                    BlissGreen.copy(alpha = 0.15f), Color(0xFF14532D), BlissGreen.copy(alpha = 0.35f)
        )
        "LATE", "IN PROGRESS", "PENDING", "PENDING APPROVAL" -> Triple(
            BlissGold.copy(alpha = 0.18f), Color(0xFF7A5C00), BlissGold.copy(alpha = 0.45f)
        )
        "ABSENT", "LOW STOCK", "ERROR", "DUE", "UNPAID", "REJECTED" -> Triple(
            Color(0xFFFFEBEE), Color(0xFFB71C1C), Color(0xFFFFCDD2)
        )
        else -> Triple(Color(0xFFF5F3EF), Color(0xFF475569), BlissGold.copy(alpha = 0.2f))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.uppercase(),
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun M3SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = BlissGold.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BlissGreen.copy(alpha = 0.5f),
            unfocusedBorderColor = BlissGold.copy(alpha = 0.2f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BlissGold.copy(alpha = alpha * 0.12f))
    )
}

@Composable
fun M3EmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = BlissGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(1.dp, BlissGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = BlissGold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AnimatedEntranceContainer(
    index: Int,
    content: @Composable () -> Unit
) {
    val visible = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(minOf(index * 25L, 200L))
        visible.value = true
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible.value,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        ) + androidx.compose.animation.slideInVertically(
            initialOffsetY = { 30 },
            animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        ),
        exit = androidx.compose.animation.fadeOut()
    ) {
        content()
    }
}
