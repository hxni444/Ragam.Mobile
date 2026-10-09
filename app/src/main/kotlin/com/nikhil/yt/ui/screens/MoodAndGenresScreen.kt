/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.ui.screens

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.nikhil.yt.LocalPlayerAwareWindowInsets
import com.nikhil.yt.R
import com.nikhil.yt.ui.component.IconButton
import com.nikhil.yt.ui.component.NavigationTitle
import com.nikhil.yt.ui.component.shimmer.ListItemPlaceHolder
import com.nikhil.yt.ui.component.shimmer.ShimmerHost
import com.nikhil.yt.ui.utils.backToMain
import com.nikhil.yt.viewmodels.MoodAndGenresViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val localConfiguration = LocalConfiguration.current
    val itemsPerRow = if (localConfiguration.orientation == ORIENTATION_LANDSCAPE) 3 else 2

    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        if (moodAndGenresList == null) {
            item {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }

        moodAndGenresList?.forEach { moodAndGenres ->
            item {
                NavigationTitle(
                    title = moodAndGenres.title,
                )

                Column(
                    modifier = Modifier.padding(horizontal = 6.dp),
                ) {
                    moodAndGenres.items.chunked(itemsPerRow).forEach { row ->
                        Row {
                            row.forEach {
                                MoodAndGenresButton(
                                    title = it.title,
                                    onClick = {
                                        navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                                    },
                                    modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(6.dp),
                                )
                            }

                            repeat(itemsPerRow - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.mood_and_genres)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

fun getGenreAccentColor(title: String): Color {
    val normalized = title.trim().lowercase()
    return when {
        normalized.contains("malayalam") -> Color(0xFF00C853) // Green
        normalized.contains("energize") -> Color(0xFFFFD600) // Yellow
        normalized.contains("chill") -> Color(0xFF82B1FF) // Sky Blue
        normalized.contains("hindi") -> Color(0xFFFFAB00) // Amber
        normalized.contains("focus") -> Color(0xFF00E676) // Mint
        normalized.contains("feel good") -> Color(0xFF69F0AE) // Light Green
        normalized.contains("commute") -> Color(0xFFFFC400) // Warm Yellow
        normalized.contains("romance") -> Color(0xFFFF1744) // Crimson Red
        normalized.contains("gaming") -> Color(0xFF448AFF) // Blue
        normalized.contains("party") -> Color(0xFF7C4DFF) // Purple
        normalized.contains("sad") -> Color(0xFF90CAF9) // Soft Blue
        normalized.contains("sleep") -> Color(0xFF7C4DFF) // Purple
        normalized.contains("workout") -> Color(0xFFFF6D00) // Orange
        normalized.contains("african") -> Color(0xFF00E676) // Green
        normalized.contains("arabic") -> Color(0xFFFF6D00) // Rust Orange
        normalized.contains("bengali") -> Color(0xFFFFD700) // Gold
        normalized.contains("bhojpuri") -> Color(0xFFD500F9) // Magenta
        normalized.contains("carnatic") -> Color(0xFFFF9100) // Amber Orange
        normalized.contains("classical") -> Color(0xFFE0E0E0) // Silver White
        normalized.contains("country") -> Color(0xFF2979FF) // Blue
        normalized.contains("dance") || normalized.contains("electronic") -> Color(0xFF00E5FF) // Cyan
        normalized.contains("decade") || normalized.contains("19") || normalized.contains("20") -> Color(0xFF69F0AE) // Mint
        normalized.contains("desi") || normalized.contains("hip-hop") -> Color(0xFFFFAB00) // Gold
        normalized.contains("devotional") -> Color(0xFF40C4FF) // Light Blue
        normalized.contains("family") -> Color(0xFF00B0FF) // Cyan
        normalized.contains("folk") || normalized.contains("acoustic") -> Color(0xFF00E676) // Green
        normalized.contains("rock") -> Color(0xFFFF3D00) // Red-Orange
        normalized.contains("pop") -> Color(0xFFFF4081) // Pink
        normalized.contains("metal") -> Color(0xFFB0BEC5) // Steel
        normalized.contains("jazz") || normalized.contains("blues") -> Color(0xFF536DFE) // Indigo
        normalized.contains("k-pop") || normalized.contains("j-pop") -> Color(0xFFFF4081) // Pink
        normalized.contains("tamil") -> Color(0xFFFF9100) // Orange
        normalized.contains("telugu") -> Color(0xFFFFD600) // Yellow
        normalized.contains("punjabi") -> Color(0xFFFF6D00) // Orange
        else -> {
            val palette = listOf(
                Color(0xFF00C853),
                Color(0xFFFFD600),
                Color(0xFF82B1FF),
                Color(0xFFFFAB00),
                Color(0xFFFF1744),
                Color(0xFF7C4DFF),
                Color(0xFFFF6D00),
                Color(0xFF00E5FF),
                Color(0xFFD500F9),
                Color(0xFF2979FF),
                Color(0xFF69F0AE),
                Color(0xFFFF4081)
            )
            palette[abs(title.hashCode()) % palette.size]
        }
    }
}

@Composable
fun MoodAndGenresButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = remember(title) { getGenreAccentColor(title) }
    val cardShape = RoundedCornerShape(10.dp)

    Surface(
        onClick = onClick,
        shape = cardShape,
        color = Color(0xFF161618),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.height(MoodAndGenresButtonHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp)
        ) {
            // Left vertical accent stripe pill
            Box(
                modifier = Modifier
                    .padding(start = 5.dp, top = 6.dp, bottom = 6.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

val MoodAndGenresButtonHeight = 48.dp
