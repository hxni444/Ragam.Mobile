/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.ragam.music.ui.player

import androidx.compose.material3.CircularProgressIndicator
import com.ragam.music.ui.component.VeluneLoader
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.ragam.music.R
import com.ragam.music.constants.MiniPlayerHeight
import com.ragam.music.extensions.togglePlayPause
import com.ragam.music.models.MediaMetadata
import com.ragam.music.playback.queues.YouTubeQueue
import com.ragam.music.LocalMusicSyncRepository

import com.ragam.music.playback.PlayerConnection
import com.ragam.music.together.TogetherSessionState
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.ragam.music.LocalPlayerConnection
import com.ragam.music.LocalDatabase
import com.ragam.music.db.entities.ArtistEntity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer

@Composable
fun SwipeableMiniPlayerBox(
    modifier: Modifier = Modifier,
    swipeSensitivity: Float,
    swipeThumbnail: Boolean,
    playerConnection: PlayerConnection,
    layoutDirection: LayoutDirection,
    coroutineScope: CoroutineScope,
    pureBlack: Boolean = false,
    useLegacyBackground: Boolean = false,
    content: @Composable (Float) -> Unit
) {
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(swipeSensitivity)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .let { baseModifier ->
                if (useLegacyBackground) {
                    baseModifier.background(
                        if (pureBlack) Color.Black
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                } else {
                    baseModifier.padding(horizontal = 12.dp)
                }
            }
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                    kotlin.math.abs(currentOffset) > minDistanceThreshold &&
                                    velocity > velocityThreshold
                                ) || (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1

                                    if (isRightSwipe && canSkipPrevious) {
                                        playerConnection.player.seekToPreviousMediaItem()
                                        if (com.ragam.music.ui.screens.settings.DiscordPresenceManager.isRunning()) {
                                            try { com.ragam.music.ui.screens.settings.DiscordPresenceManager.restart() } catch (_: Exception) {}
                                        }
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerConnection.player.seekToNext()
                                        if (com.ragam.music.ui.screens.settings.DiscordPresenceManager.isRunning()) {
                                            try { com.ragam.music.ui.screens.settings.DiscordPresenceManager.restart() } catch (_: Exception) {}
                                        }
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            }
    ) {
        content(offsetXAnimatable.value)

        // Visual indicator
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = if (offsetXAnimatable.value > 0) "Previous" else "Next",
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MiniPlayerPlayPauseButton(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    playbackState: Int,
    isLoading: Boolean,
    playerConnection: PlayerConnection,
    isRemote: Boolean = false,
    onRemotePlayPause: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(48.dp)
    ) {
        // Circular progress indicator around the play button
        if (duration > 0) {
            CircularProgressIndicator(
                progress = { (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }

        // Play/Pause button with thumbnail background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable {
                    if (isRemote) {
                        onRemotePlayPause?.invoke()
                    } else if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                }
        ) {
            if (isLoading) {
                VeluneLoader(size = 20.dp)
            } else {
                Icon(
                    painter = painterResource(
                        if (!isRemote && playbackState == Player.STATE_ENDED) {
                            R.drawable.replay
                        } else if (isPlaying) {
                            R.drawable.pause
                        } else {
                            R.drawable.play
                        }
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RowScope.MiniPlayerInfo(
    mediaMetadata: MediaMetadata
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = mediaMetadata.title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "title"
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }

        AnimatedContent(
            targetState = mediaMetadata.artists,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "artist"
        ) { artists ->
            Text(
                text = artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
fun MiniPlayerActionButtons(
    isLiked: Boolean,
    onLikeClick: () -> Unit
) {
    IconButton(
        onClick = onLikeClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            painter = painterResource(
                if (isLiked) R.drawable.favorite else R.drawable.favorite_border
            ),
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun NewMiniPlayerContent(
    pureBlack: Boolean,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    
    val isLoading = playbackState == Player.STATE_BUFFERING
    val isLiked = currentSong?.song?.liked == true
    var showAddToPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    val syncRepo = LocalMusicSyncRepository.current
    val cloudNowPlaying by syncRepo.cloudNowPlaying.collectAsState()
    val isFromDesktop = cloudNowPlaying?.deviceName?.contains("Desktop", ignoreCase = true) == true ||
        (cloudNowPlaying?.deviceName?.isNotBlank() == true && !cloudNowPlaying?.deviceName.orEmpty().contains("Mobile", ignoreCase = true))
    val isRemote = (cloudNowPlaying?.track != null) && isFromDesktop && (!isPlaying || cloudNowPlaying?.isPlaying == true)
    val cloudTrack = cloudNowPlaying?.track
    val remoteDevice = cloudNowPlaying?.deviceName?.ifBlank { "Desktop" } ?: "Desktop"
    val effectiveIsPlaying = if (isRemote) (cloudNowPlaying?.isPlaying == true) else isPlaying

    var remoteTickerSec by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(isRemote, cloudNowPlaying?.isPlaying, cloudNowPlaying?.timestampMs, cloudNowPlaying?.positionSec) {
        if (isRemote && cloudNowPlaying?.isPlaying == true) {
            val baseTime = cloudNowPlaying?.timestampMs ?: System.currentTimeMillis()
            val basePos = cloudNowPlaying?.positionSec ?: 0.0
            val maxDur = (cloudTrack?.duration ?: 1L).toDouble()
            while (isActive) {
                val elapsed = ((System.currentTimeMillis() - baseTime) / 1000.0).coerceAtLeast(0.0)
                remoteTickerSec = (basePos + elapsed).coerceAtMost(maxDur)
                delay(250)
            }
        } else if (isRemote) {
            remoteTickerSec = cloudNowPlaying?.positionSec ?: 0.0
        }
    }
    val cloudPosSec = if (isRemote) remoteTickerSec else 0.0
    val cloudDurSec = if (isRemote) ((cloudTrack?.duration ?: 1L).coerceAtLeast(1L)).toDouble() else 1.0
    val effectivePosition = if (isRemote) (cloudPosSec * 1000.0).toLong() else position
    val effectiveDuration = if (isRemote) (cloudDurSec * 1000.0).toLong() else duration

    mediaMetadata?.let { meta ->
        if (showAddToPlaylistDialog) {
            com.ragam.music.ui.menu.AddToPlaylistDialog(
                isVisible = true,
                onGetSong = { playlist ->
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        playlist.playlist.browseId?.let { browseId ->
                            com.ragam.music.innertube.YouTube.addToPlaylist(browseId, meta.id)
                        }
                    }
                    listOf(meta.id)
                },
                onDismiss = { showAddToPlaylistDialog = false },
                onAddComplete = { _, playlistNames ->
                    val message = when {
                        playlistNames.size == 1 -> context.getString(R.string.added_to_playlist, playlistNames.first())
                        else -> context.getString(R.string.added_to_n_playlists, playlistNames.size)
                    }
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        // Play/Pause bubble button (left side)
        MiniPlayerPlayPauseButton(
            position = effectivePosition,
            duration = effectiveDuration,
            isPlaying = effectiveIsPlaying,
            playbackState = playbackState,
            isLoading = if (isRemote) false else isLoading,
            playerConnection = playerConnection,
            isRemote = isRemote,
            onRemotePlayPause = {
                coroutineScope.launch {
                    syncRepo.updateRemotePlayback(isPlaying = !effectiveIsPlaying)
                }
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist (Remote or Local)
        if (isRemote && cloudTrack != null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = cloudTrack.title.ifBlank { "Unknown Title" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cloudTrack.artist.ifBlank { "Unknown Artist" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).basicMarquee()
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• $remoteDevice",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        } else {
            mediaMetadata?.let {
                MiniPlayerInfo(mediaMetadata = it)
            } ?: Spacer(Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isRemote && cloudTrack != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(38.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.desktop_windows),
                    contentDescription = "Playing on Desktop",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            // + (Add to Playlist) Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.16f),
                        shape = CircleShape
                    )
                    .clickable { showAddToPlaylistDialog = true }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "Add to playlist",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Heart (Favorite) Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isLiked) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = if (isLiked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.16f),
                        shape = CircleShape
                    )
                    .clickable { playerConnection.toggleLike() }
            ) {
                Icon(
                    painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MiniPlayerSubscribeButton(mediaMetadata: MediaMetadata) {
    val database = LocalDatabase.current
    mediaMetadata.artists.firstOrNull()?.let { artistInfo ->
        artistInfo.id?.let { artistId ->
            val libraryArtist by database.artist(artistId).collectAsState(initial = null)
            val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = if (isSubscribed)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .background(
                        color = if (isSubscribed)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else
                            Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable {
                        database.transaction {
                            val artist = libraryArtist?.artist
                            if (artist != null) {
                                update(artist.toggleLike())
                            } else {
                                insert(
                                    ArtistEntity(
                                        id = artistId,
                                        name = artistInfo.name,
                                        channelId = null,
                                        thumbnailUrl = null,
                                    ).toggleLike()
                                )
                            }
                        }
                    }
            ) {
                Icon(
                    painter = painterResource(
                        if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe
                    ),
                    contentDescription = if (isSubscribed) "Unsubscribe" else "Subscribe",
                    tint = if (isSubscribed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
