package com.ragam.music.sync

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class TrackDto(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L, // in seconds
    val thumbnailUrl: String = ""
)

@IgnoreExtraProperties
data class CloudNowPlayingState(
    val userId: String = "",
    val track: TrackDto? = null,
    @get:PropertyName("isPlaying")
    @set:PropertyName("isPlaying")
    var isPlaying: Boolean = false,
    val positionSec: Double = 0.0,
    val timestampMs: Long = 0L,
    val deviceName: String = "",
    val volume: Double = 1.0,
    val repeatMode: String = "off",
    @get:PropertyName("isShuffle")
    @set:PropertyName("isShuffle")
    var isShuffle: Boolean = false
)

@IgnoreExtraProperties
data class CloudHistoryItem(
    var docId: String = "",
    val userId: String = "",
    val track: TrackDto? = null,
    val playedAtMs: Long = 0L,
    val device: String = ""
)
