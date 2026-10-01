package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerHelper
import com.example.audio.AudioRecorderHelper
import com.example.data.local.ClonedVoiceEntity
import com.example.data.local.GeneratedAudioEntity
import com.example.data.model.CartesiaVoice
import com.example.data.repository.CartesiaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class AppScreen {
    HOME,
    VOICE_CLONE,
    SPEECH_STUDIO,
    VOICE_LIBRARY,
    HISTORY
}

sealed class CloneUiState {
    object Idle : CloneUiState()
    object Recording : CloneUiState()
    data class Recorded(val audioFile: File) : CloneUiState()
    object Cloning : CloneUiState()
    data class Success(val clonedVoice: ClonedVoiceEntity) : CloneUiState()
    data class Error(val message: String) : CloneUiState()
}

sealed class TtsUiState {
    object Idle : TtsUiState()
    object Generating : TtsUiState()
    data class Success(val generatedAudio: GeneratedAudioEntity) : TtsUiState()
    data class Error(val message: String) : TtsUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = CartesiaRepository(application)
    val recorderHelper = AudioRecorderHelper(application)
    val playerHelper = AudioPlayerHelper(application)

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // API Key dialog state
    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    // Database Flows
    val clonedVoices: StateFlow<List<ClonedVoiceEntity>> = repository.allClonedVoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generatedAudios: StateFlow<List<GeneratedAudioEntity>> = repository.allGeneratedAudios
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultSonicVoices: List<CartesiaVoice> = repository.getDefaultSonicVoices()

    // Active voice for TTS
    private val _selectedVoice = MutableStateFlow<CartesiaVoice>(defaultSonicVoices.first())
    val selectedVoice: StateFlow<CartesiaVoice> = _selectedVoice.asStateFlow()

    // Clone Wizard state
    private val _cloneState = MutableStateFlow<CloneUiState>(CloneUiState.Idle)
    val cloneState: StateFlow<CloneUiState> = _cloneState.asStateFlow()

    var cloneVoiceName = MutableStateFlow("")
    var cloneVoiceLanguage = MutableStateFlow("bn") // default Bengali
    var cloneVoiceDescription = MutableStateFlow("")

    // TTS Studio state
    private val _ttsState = MutableStateFlow<TtsUiState>(TtsUiState.Idle)
    val ttsState: StateFlow<TtsUiState> = _ttsState.asStateFlow()

    val ttsInputText = MutableStateFlow("হ্যালো, আমি কার্তেসিয়া সোনিক এআই দিয়ে কথা বলছি। এটি একটি উন্নত ভয়েস ক্লোনিং প্ল্যাটফর্ম।")
    val ttsSelectedModel = MutableStateFlow("sonic-3.6")
    val ttsSelectedLanguage = MutableStateFlow("bn") // Bengali or en

    init {
        recorderHelper.onMaxDurationReached = { recordedFile ->
            _cloneState.value = CloneUiState.Recorded(recordedFile)
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setShowApiKeyDialog(show: Boolean) {
        _showApiKeyDialog.value = show
    }

    fun selectVoiceForTts(voice: CartesiaVoice) {
        _selectedVoice.value = voice
        _currentScreen.value = AppScreen.SPEECH_STUDIO
    }

    fun selectClonedVoiceForTts(cloned: ClonedVoiceEntity) {
        val voice = CartesiaVoice(
            id = cloned.cartesiaVoiceId,
            name = cloned.name,
            description = cloned.description,
            language = cloned.language,
            isCustom = true
        )
        _selectedVoice.value = voice
        _currentScreen.value = AppScreen.SPEECH_STUDIO
    }

    // Voice Clone actions
    fun startRecording() {
        val res = recorderHelper.startRecording()
        if (res.isSuccess) {
            _cloneState.value = CloneUiState.Recording
        } else {
            _cloneState.value = CloneUiState.Error(res.exceptionOrNull()?.localizedMessage ?: "Failed to start recording")
        }
    }

    fun stopRecording() {
        val file = recorderHelper.stopRecording()
        if (file != null && file.exists()) {
            _cloneState.value = CloneUiState.Recorded(file)
        } else {
            _cloneState.value = CloneUiState.Idle
        }
    }

    fun setCustomAudioFile(file: File) {
        _cloneState.value = CloneUiState.Recorded(file)
    }

    fun resetCloneWizard() {
        recorderHelper.stopRecording()
        playerHelper.stopAudio()
        _cloneState.value = CloneUiState.Idle
        cloneVoiceName.value = ""
        cloneVoiceDescription.value = ""
    }

    fun executeVoiceClone() {
        val currentState = _cloneState.value
        val file = when (currentState) {
            is CloneUiState.Recorded -> currentState.audioFile
            else -> return
        }

        val name = cloneVoiceName.value.ifBlank { "My Cloned Voice" }
        val lang = cloneVoiceLanguage.value
        val desc = cloneVoiceDescription.value

        if (!repository.isApiKeyConfigured()) {
            _cloneState.value = CloneUiState.Error("Cartesia API Key required. Please set it in settings.")
            _showApiKeyDialog.value = true
            return
        }

        _cloneState.value = CloneUiState.Cloning

        viewModelScope.launch {
            val result = repository.cloneVoice(
                clipFile = file,
                name = name,
                language = lang,
                description = desc
            )

            if (result.isSuccess) {
                val clonedVoice = result.getOrThrow()
                _cloneState.value = CloneUiState.Success(clonedVoice)
                // Select this new voice for TTS
                selectClonedVoiceForTts(clonedVoice)
            } else {
                _cloneState.value = CloneUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Cloning failed"
                )
            }
        }
    }

    // Speech generation
    fun generateSpeech() {
        val text = ttsInputText.value.trim()
        if (text.isBlank()) return

        if (!repository.isApiKeyConfigured()) {
            _ttsState.value = TtsUiState.Error("Cartesia API Key required. Please set your key.")
            _showApiKeyDialog.value = true
            return
        }

        val voice = _selectedVoice.value
        val model = ttsSelectedModel.value
        val lang = ttsSelectedLanguage.value

        _ttsState.value = TtsUiState.Generating

        viewModelScope.launch {
            val result = repository.generateSpeech(
                voiceId = voice.id,
                voiceName = voice.name,
                text = text,
                modelId = model,
                language = lang
            )

            if (result.isSuccess) {
                val generated = result.getOrThrow()
                _ttsState.value = TtsUiState.Success(generated)
                // Auto play the generated speech!
                playerHelper.playAudio(generated.filePath)
            } else {
                _ttsState.value = TtsUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Generation failed"
                )
            }
        }
    }

    fun deleteClonedVoice(voice: ClonedVoiceEntity) {
        viewModelScope.launch {
            repository.deleteClonedVoice(voice.id)
        }
    }

    fun deleteGeneratedAudio(audio: GeneratedAudioEntity) {
        viewModelScope.launch {
            if (playerHelper.currentPlayingPath.value == audio.filePath) {
                playerHelper.stopAudio()
            }
            repository.deleteGeneratedAudio(audio.id, audio.filePath)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderHelper.release()
        playerHelper.release()
    }
}
