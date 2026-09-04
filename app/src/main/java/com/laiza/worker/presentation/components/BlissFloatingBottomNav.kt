package com.laiza.worker.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laiza.worker.core.theme.BlissGold
import com.laiza.worker.core.theme.BlissGreen
import com.laiza.worker.core.theme.BlissGreenLight

data class BlissNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun BlissFloatingBottomNav(
    tabs: List<BlissNavTab>,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val animIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bliss_nav_index"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(100.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A).copy(alpha = 0.92f)),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(BlissGold.copy(alpha = 0.5f), BlissGreen.copy(alpha = 0.25f))
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (animIndex > 0f) Spacer(modifier = Modifier.weight(animIndex))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp)
                            .background(BlissGreen.copy(alpha = 0.22f), RoundedCornerShape(100.dp))
                            .border(1.dp, BlissGold.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                    )
                    val remaining = tabs.size - 1f - animIndex
                    if (remaining > 0f) Spacer(modifier = Modifier.weight(remaining))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = index == selectedIndex
                        val interactionSource = remember { MutableInteractionSource() }
                        val pressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "tab_scale")
                        val iconColor by animateColorAsState(
                            if (selected) BlissGreenLight else Color.White.copy(alpha = 0.45f),
                            tween(150),
                            label = "icon_color"
                        )
                        val labelColor by animateColorAsState(
                            if (selected) BlissGreenLight else Color.White.copy(alpha = 0.45f),
                            tween(150),
                            label = "label_color"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .clip(RoundedCornerShape(100.dp))
                                .clickable(interactionSource = interactionSource, indication = null) {
                                    if (currentRoute != tab.route) onTabSelected(tab.route)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(tab.icon, tab.label, tint = iconColor, modifier = Modifier.size(22.dp))
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = labelColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
