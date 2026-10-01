package com.example.audio

import android.content.Context
import android.media.MediaRecorder
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

class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _amplitudeLevel = MutableStateFlow(0f)
    val amplitudeLevel: StateFlow<Float> = _amplitudeLevel.asStateFlow()

    var onMaxDurationReached: ((File) -> Unit)? = null

    fun startRecording(): Result<File> {
        stopRecording()

        val outputDir = File(context.cacheDir, "voice_recordings")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val file = File(outputDir, "recording_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _recordingDurationSec.value = 0

            recordingJob = scope.launch {
                var seconds = 0
                while (isActive && _isRecording.value) {
                    delay(100)
                    // Sample amplitude
                    val amp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val normalized = (amp / 28000f).coerceIn(0.05f, 1f)
                    _amplitudeLevel.value = normalized

                    // Count seconds every 10 iterations (1 second)
                    if (System.currentTimeMillis() % 1000 < 120) {
                        seconds++
                        _recordingDurationSec.value = seconds
                        if (seconds >= 60) {
                            // Max 60 seconds reached
                            stopRecording()
                            onMaxDurationReached?.invoke(file)
                            break
                        }
                    }
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            currentOutputFile?.delete()
            currentOutputFile = null
            _isRecording.value = false
            Result.failure(e)
        }
    }

    fun stopRecording(): File? {
        recordingJob?.cancel()
        recordingJob = null
        _amplitudeLevel.value = 0f

        val recorded = currentOutputFile
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        _isRecording.value = false

        return recorded
    }

    fun release() {
        stopRecording()
    }
}
