package com.nikhil.yt.ui.component

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.nikhil.yt.ui.screens.Screens

@Composable
fun FluidSlidingNavigationBar(
    modifier: Modifier = Modifier,
    items: List<Screens>,
    currentRoute: String,
    pureBlack: Boolean,
    onTabSelected: (Screens) -> Unit
) {
    val targetIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    var eagerSelectedIndex by remember(currentRoute) { mutableIntStateOf(targetIndex) }

    val isDark = pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glassStyle = GlassEffectDefaults.navigationBarStyle(isDark, pureBlack)
    val pillShape = RoundedCornerShape(28.dp)
    val density = LocalDensity.current

    val supportsBackdrop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val backdrop = rememberCanvasBackdrop {
        drawRect(
            color = glassStyle.backgroundDimColor.copy(alpha = glassStyle.backgroundDimAlpha),
            size = size
        )
    }

    val ragamAccent = Color(0xFFEF4903)

    BoxWithConstraints(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = pillShape,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(pillShape)
            .then(
                if (supportsBackdrop) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
                        effects = {
                            if (glassStyle.useVibrancy) vibrancy()
                            blur(with(density) { 28.dp.toPx() })
                        },
                        onDrawSurface = {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = if (isDark) listOf(
                                        if (pureBlack) Color(0xFF141418).copy(alpha = 0.84f) else Color(0xFF1C1C24).copy(alpha = 0.86f),
                                        if (pureBlack) Color(0xFF08080C).copy(alpha = 0.92f) else Color(0xFF101015).copy(alpha = 0.94f)
                                    ) else listOf(
                                        Color.White.copy(alpha = 0.88f),
                                        Color(0xFFF2F2F7).copy(alpha = 0.92f)
                                    )
                                )
                            )
                        }
                    )
                } else {
                    Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) listOf(
                                if (pureBlack) Color(0xFF141418).copy(alpha = 0.94f) else Color(0xFF1C1C24).copy(alpha = 0.95f),
                                if (pureBlack) Color(0xFF08080C).copy(alpha = 0.97f) else Color(0xFF101015).copy(alpha = 0.98f)
                            ) else listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color(0xFFF2F2F7).copy(alpha = 0.98f)
                            )
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.08f)
                    ) else listOf(
                        Color.Black.copy(alpha = 0.14f),
                        Color.Black.copy(alpha = 0.05f)
                    )
                ),
                shape = pillShape
            )
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val tabWidth = maxWidth / items.size
        val pillWidth = 46.dp
        val pillHeight = 32.dp

        val indicatorOffset by animateDpAsState(
            targetValue = (tabWidth * eagerSelectedIndex) + ((tabWidth - pillWidth) / 2),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "PillSlider"
        )

        val indicatorColor = ragamAccent
        val indicatorBorder = ragamAccent.copy(alpha = 0.6f)

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset, y = 14.dp)
                .width(pillWidth)
                .height(pillHeight)
                .shadow(elevation = 6.dp, shape = CircleShape, spotColor = ragamAccent, ambientColor = ragamAccent)
                .background(
                    color = indicatorColor,
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = indicatorBorder,
                    shape = CircleShape
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = eagerSelectedIndex == index

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                eagerSelectedIndex = index
                                onTabSelected(item)
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Icon(
                        painter = painterResource(id = if (isSelected) item.iconIdActive else item.iconIdInactive),
                        contentDescription = stringResource(id = item.titleId),
                        tint = if (isSelected) Color.White else if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(id = item.titleId),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) (if (isDark) Color.White else Color.Black) else if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}
