package com.nikhil.yt.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikhil.yt.ui.screens.Screens

@Composable
fun FluidSlidingNavigationBar(
    modifier: Modifier = Modifier,
    items: List<Screens>,
    currentRoute: String,
    pureBlack: Boolean,
    onTabSelected: (Screens) -> Unit
) {
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val ragamAccent = Color(0xFFEF4903)
    val trayBg = Color.White.copy(alpha = 0.12f)
    val trayBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    val pillShape = RoundedCornerShape(28.dp)

    BoxWithConstraints(
        modifier = modifier
            .clip(pillShape)
            .border(trayBorder, pillShape)
            .background(trayBg)
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val tabWidth = maxWidth / items.size

        val pillWidth = 52.dp
        val pillHeight = 32.dp

        val indicatorOffset by animateDpAsState(
            targetValue = (tabWidth * selectedIndex) + ((tabWidth - pillWidth) / 2),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "PillSlider"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset, y = 14.dp)
                .width(pillWidth)
                .height(pillHeight)
                .background(
                    color = ragamAccent,
                    shape = CircleShape
                )
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(item) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Icon(
                        painter = painterResource(id = if (isSelected) item.iconIdActive else item.iconIdInactive),
                        contentDescription = stringResource(id = item.titleId),
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(id = item.titleId),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}
