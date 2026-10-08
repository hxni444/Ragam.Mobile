package com.nikhil.yt.ui.player

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.nikhil.yt.LocalPlayerConnection
import com.nikhil.yt.constants.MiniPlayerHeight
import com.nikhil.yt.constants.SwipeSensitivityKey
import com.nikhil.yt.ui.component.GlassEffectDefaults
import com.nikhil.yt.utils.rememberPreference
import kotlin.math.roundToInt

@Composable
fun GlassMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.nikhil.yt.constants.SwipeThumbnailKey, true)

    val isDark = pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glassStyle = GlassEffectDefaults.miniPlayerStyle(isDark, pureBlack)
    val pillShape = RoundedCornerShape(24.dp)
    val supportsBackdrop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supportsLens = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val deepGlassBackdrop = rememberCanvasBackdrop {
        drawRect(
            color = glassStyle.backgroundDimColor.copy(alpha = glassStyle.backgroundDimAlpha),
            size = size
        )
    }

    SwipeableMiniPlayerBox(
        modifier = modifier,
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .shadow(elevation = 16.dp, shape = pillShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(pillShape)
                .then(
                    if (supportsBackdrop) {
                        Modifier.drawBackdrop(
                            backdrop = deepGlassBackdrop,
                            shape = { pillShape },
                            effects = {
                                if (glassStyle.useVibrancy) vibrancy()
                                blur(with(density) { glassStyle.blurRadius.toPx() })
                                if (supportsLens && glassStyle.useLens) {
                                    lens(
                                        with(density) { glassStyle.lensHeight.toPx() },
                                        with(density) { glassStyle.lensAmount.toPx() }
                                    )
                                }
                            },
                            onDrawSurface = {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF1E1E26).copy(alpha = if (pureBlack) 0.85f else 0.92f),
                                            Color(0xFF101015).copy(alpha = if (pureBlack) 0.92f else 0.96f)
                                        )
                                    )
                                )
                            }
                        )
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E1E26).copy(alpha = 0.94f),
                                    Color(0xFF0F0F14).copy(alpha = 0.98f)
                                )
                            )
                        )
                    }
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = pillShape
                )
        ) {
            NewMiniPlayerContent(
                pureBlack = pureBlack,
                position = position,
                duration = duration,
                playerConnection = playerConnection
            )
        }
    }
}
