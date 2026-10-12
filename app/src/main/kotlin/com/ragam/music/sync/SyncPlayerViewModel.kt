package com.ragam.music.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncPlayerViewModel @Inject constructor(
    private val repository: MusicSyncRepository
) : ViewModel() {

    private val _nowPlaying = MutableStateFlow<CloudNowPlayingState?>(null)
    val nowPlaying: StateFlow<CloudNowPlayingState?> = _nowPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0.0)
    val currentPositionSec: StateFlow<Double> = _currentPositionSec.asStateFlow()

    private val _history = MutableStateFlow<List<CloudHistoryItem>>(emptyList())
    val history: StateFlow<List<CloudHistoryItem>> = _history.asStateFlow()

    private val _activeSyncId = MutableStateFlow<String?>(null)
    val activeSyncId: StateFlow<String?> = _activeSyncId.asStateFlow()

    private val _syncEmail = MutableStateFlow<String?>(null)
    val syncEmail: StateFlow<String?> = _syncEmail.asStateFlow()

    private var interpolatorJob: Job? = null
    private var nowPlayingJob: Job? = null
    private var historyJob: Job? = null

    init {
        startSync()
    }

    fun startSync() {
        viewModelScope.launch {
            _activeSyncId.value = repository.getActiveSyncId()
            _syncEmail.value = repository.getEffectiveSyncEmail()
        }

        nowPlayingJob?.cancel()
        nowPlayingJob = viewModelScope.launch {
            repository.observeNowPlaying().collect { state ->
                _nowPlaying.value = state
                startPlayheadInterpolator(state)
            }
        }

        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            repository.observeHistory().collect { list ->
                _history.value = list
            }
        }
    }

    /**
     * Interpolates playback position locally every 500ms when song is playing
     */
    private fun startPlayheadInterpolator(state: CloudNowPlayingState?) {
        interpolatorJob?.cancel()

        if (state == null || state.track == null) {
            _currentPositionSec.value = 0.0
            return
        }

        val basePosition = state.positionSec
        val timestamp = state.timestampMs
        val duration = state.track.duration.toDouble()
        val isPlaying = state.isPlaying

        if (!isPlaying) {
            _currentPositionSec.value = basePosition
            return
        }

        interpolatorJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsedSec = (now - timestamp) / 1000.0
                val calculated = (basePosition + elapsedSec).coerceIn(0.0, if (duration > 0) duration else 99999.0)
                _currentPositionSec.value = calculated
                delay(500)
            }
        }
    }

    fun togglePlayPause() {
        val current = _nowPlaying.value ?: return
        viewModelScope.launch {
            repository.updateRemotePlayback(isPlaying = !current.isPlaying)
        }
    }

    fun seekTo(positionSec: Double) {
        viewModelScope.launch {
            repository.updateRemotePlayback(positionSec = positionSec)
        }
    }

    fun updateSyncEmail(newEmail: String) {
        viewModelScope.launch {
            repository.setCustomSyncEmail(newEmail)
            startSync()
        }
    }
}
