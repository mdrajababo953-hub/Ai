package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun playAudio(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        if (_currentPlayingPath.value == filePath && mediaPlayer != null) {
            // Resume
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
            return
        }

        stopAudio()

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    stopProgressTracker()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    player.playbackParams = PlaybackParams().setSpeed(_playbackSpeed.value)
                } catch (_: Exception) {}
            }

            player.start()
            mediaPlayer = player
            _currentPlayingPath.value = filePath
            _durationMs.value = player.duration
            _isPlaying.value = true
            startProgressTracker()
        } catch (_: Exception) {
            stopAudio()
        }
    }

    fun pauseAudio() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopProgressTracker()
        } catch (_: Exception) {}
    }

    fun togglePlayPause(filePath: String) {
        if (_currentPlayingPath.value == filePath && _isPlaying.value) {
            pauseAudio()
        } else {
            playAudio(filePath)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        } catch (_: Exception) {}
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    val wasPlaying = player.isPlaying
                    player.playbackParams = PlaybackParams().setSpeed(speed)
                    if (!wasPlaying) {
                        player.pause()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun stopAudio() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPositionMs.value = 0
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                delay(100)
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopAudio()
    }
}
