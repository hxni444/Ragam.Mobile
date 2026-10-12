package com.ragam.music.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ragam.music.LocalPlayerAwareWindowInsets
import com.ragam.music.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    navController: NavController? = null,
    viewModel: SyncPlayerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val positionSec by viewModel.currentPositionSec.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val activeSyncId by viewModel.activeSyncId.collectAsStateWithLifecycle()
    val syncEmail by viewModel.syncEmail.collectAsStateWithLifecycle()

    var showEditEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }

    if (showEditEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEditEmailDialog = false },
            title = { Text("Configure Sync Email") },
            text = {
                Column {
                    Text(
                        text = "Enter the Google/YouTube email used on Ragam Desktop to link your playback session.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSyncEmail(emailInput)
                        showEditEmailDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditEmailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = null,
                            tint = Color(0xFFFF5400),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ragam Desktop Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    if (navController != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startSync() }) {
                        Icon(
                            painter = painterResource(R.drawable.cached),
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F0F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0F0F)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .padding(horizontal = 16.dp)
        ) {
            // Status and sync ID info banner
            val isLinked = !syncEmail.isNullOrBlank() || activeSyncId?.startsWith("u_") == true
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF191919)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        emailInput = syncEmail.orEmpty()
                        showEditEmailDialog = true
                    }
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (nowPlaying != null) Color(0xFF00E676)
                                else if (isLinked) Color(0xFFFF9100)
                                else Color(0xFFFF5252)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nowPlaying != null) "Connected (${nowPlaying?.deviceName ?: "Desktop"})"
                                   else if (isLinked) "Listening for Desktop..."
                                   else "Desktop Email Not Linked",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isLinked) "Sync ID: ${activeSyncId ?: "Resolving..."}"
                                   else "Tap to enter the email used on Ragam Desktop",
                            fontSize = 11.sp,
                            color = if (isLinked) Color(0xFF888888) else Color(0xFFFF9100),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            emailInput = syncEmail.orEmpty()
                            showEditEmailDialog = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = "Edit Sync Email",
                            tint = Color(0xFFFF5400),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Now Playing Card
            AnimatedVisibility(visible = nowPlaying?.track != null) {
                nowPlaying?.track?.let { track ->
                    Column {
                        Text(
                            text = "Now Playing",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE0E0E0),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        NowPlayingCard(
                            track = track,
                            isPlaying = nowPlaying?.isPlaying == true,
                            deviceName = nowPlaying?.deviceName?.ifBlank { "Desktop" } ?: "Desktop",
                            currentPositionSec = positionSec,
                            onTogglePlayPause = { viewModel.togglePlayPause() }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            if (nowPlaying?.track == null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.desktop_windows),
                            contentDescription = null,
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Active Desktop Playback",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start playing a song on Ragam Desktop to see real-time sync here.",
                            color = Color(0xFF777777),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Text(
                text = "Listening History",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE0E0E0),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (history.isEmpty()) {
                Text(
                    text = "No history synced yet",
                    color = Color(0xFF666666),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(history, key = { it.docId.ifBlank { it.playedAtMs.toString() } }) { item ->
                        item.track?.let { historyTrack ->
                            HistoryItemRow(track = historyTrack)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingCard(
    track: TrackDto,
    isPlaying: Boolean,
    deviceName: String,
    currentPositionSec: Double,
    onTogglePlayPause: () -> Unit = {}
) {
    val duration = track.duration.coerceAtLeast(1L).toFloat()
    val progress = (currentPositionSec.toFloat() / duration).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Device Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.desktop_windows),
                    contentDescription = null,
                    tint = Color(0xFFFF5400),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Playing on $deviceName",
                    fontSize = 12.sp,
                    color = Color(0xFFFF5400),
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = if (isPlaying) "Playing" else "Paused",
                        tint = Color(0xFFFF5400),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFFF5400),
                trackColor = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatSeconds(currentPositionSec.toLong()), fontSize = 11.sp, color = Color(0xFF888888))
                Text(text = formatSeconds(track.duration), fontSize = 11.sp, color = Color(0xFF888888))
            }
        }
    }
}

@Composable
fun HistoryItemRow(track: TrackDto) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF181818))
            .padding(10.dp)
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = Color(0xFF888888),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatSeconds(track.duration),
            color = Color(0xFF666666),
            fontSize = 12.sp
        )
    }
}

fun formatSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
