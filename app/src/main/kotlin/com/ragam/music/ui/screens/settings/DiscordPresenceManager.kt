/*
 * Velune - by Nikhil
 * Ragam Mobile
 * Licensed Under GPL-3.0
 */

package com.ragam.music.ui.screens.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ragam.music.db.entities.Song

/**
 * Neutralized DiscordPresenceManager - Discord feature removed.
 */
object DiscordPresenceManager {
    private val _lastRpcStartTime = MutableStateFlow<Long?>(null)
    val lastRpcStartTimeFlow = _lastRpcStartTime.asStateFlow()
    val lastRpcStartTime: Long? get() = null

    private val _lastRpcEndTime = MutableStateFlow<Long?>(null)
    val lastRpcEndTimeFlow = _lastRpcEndTime.asStateFlow()
    val lastRpcEndTime: Long? get() = null

    fun setLastRpcTimestamps(start: Long?, end: Long?) {}

    suspend fun updatePresence(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
    ): Boolean = true

    fun start(
        context: Context,
        token: String,
        songProvider: () -> Song?,
        positionProvider: () -> Long,
        isPausedProvider: () -> Boolean,
        intervalProvider: () -> Long,
    ) {}

    fun restart(): Boolean = false

    fun resetFailureCount() {}

    suspend fun updateNow(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
    ): Boolean = true

    fun stop() {}

    fun isRunning(): Boolean = false
}
