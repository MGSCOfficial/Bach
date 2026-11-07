package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val activeTrackId: String? = null
)

class AudioPreviewPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun playTrack(trackId: String, uriString: String, durationMs: Long = 0L) {
        if (_playerState.value.activeTrackId == trackId && mediaPlayer != null) {
            togglePlayPause()
            return
        }

        stop()
        try {
            val player = MediaPlayer().apply {
                val uri = Uri.parse(uriString)
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    mp.start()
                    val totalDuration = if (durationMs > 0) durationMs else mp.duration.toLong()
                    _playerState.value = PlayerState(
                        isPlaying = true,
                        currentPositionMs = 0L,
                        durationMs = totalDuration,
                        activeTrackId = trackId
                    )
                    startProgressTracking()
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(isPlaying = false, currentPositionMs = 0L)
                    stopProgressTracking()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPreviewPlayer", "MediaPlayer error: what=$what, extra=$extra")
                    stop()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("AudioPreviewPlayer", "Failed to start playback for $uriString", e)
            stop()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
            stopProgressTracking()
        } else {
            player.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun stop() {
        stopProgressTracking()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (ignored: Exception) {}
        mediaPlayer = null
        _playerState.value = PlayerState()
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = mp.currentPosition.toLong(),
                            durationMs = if (mp.duration > 0) mp.duration.toLong() else _playerState.value.durationMs
                        )
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
    }
}
