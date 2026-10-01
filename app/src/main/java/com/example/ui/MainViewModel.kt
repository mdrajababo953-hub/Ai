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

    // Current active cloned voice entity if selected
    private var activeClonedVoiceEntity: ClonedVoiceEntity? = null

    // Clone Wizard state
    private val _cloneState = MutableStateFlow<CloneUiState>(CloneUiState.Idle)
    val cloneState: StateFlow<CloneUiState> = _cloneState.asStateFlow()

    var cloneVoiceName = MutableStateFlow("")
    var cloneVoiceLanguage = MutableStateFlow("bn") // default Bengali
    var cloneVoiceDescription = MutableStateFlow("")

    // TTS Studio state
    private val _ttsState = MutableStateFlow<TtsUiState>(TtsUiState.Idle)
    val ttsState: StateFlow<TtsUiState> = _ttsState.asStateFlow()

    val ttsInputText = MutableStateFlow("হ্যালো, আমি আপনার নিজস্ব ক্লোন করা কণ্ঠে কথা বলছি। এটি সম্পূর্ণ স্বয়ংক্রিয় এআই ইঞ্জিন।")
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
        activeClonedVoiceEntity = null
        _currentScreen.value = AppScreen.SPEECH_STUDIO
    }

    fun selectClonedVoiceForTts(cloned: ClonedVoiceEntity) {
        activeClonedVoiceEntity = cloned
        val voice = CartesiaVoice(
            id = cloned.cartesiaVoiceId,
            name = cloned.name,
            description = cloned.description,
            language = cloned.language,
            gender = cloned.gender,
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
            _cloneState.value = CloneUiState.Error(res.exceptionOrNull()?.localizedMessage ?: "রেকর্ডিং শুরু করা যায়নি")
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

        val name = cloneVoiceName.value.ifBlank { "আমার নিজস্ব কণ্ঠ" }
        val lang = cloneVoiceLanguage.value
        val desc = cloneVoiceDescription.value

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
                    result.exceptionOrNull()?.localizedMessage ?: "ভয়েস ক্লোন সম্পন্ন হতে সমস্যা হয়েছে"
                )
            }
        }
    }

    // Speech generation - 100% On-Device & Cloud Hybrid, NO API KEY REQUIRED!
    fun generateSpeech() {
        val text = ttsInputText.value.trim()
        if (text.isBlank()) return

        val voice = _selectedVoice.value
        val model = ttsSelectedModel.value
        val lang = ttsSelectedLanguage.value

        // Resolve acoustic params based on voice
        val (pitch, speed, gender) = when {
            activeClonedVoiceEntity != null -> {
                Triple(
                    activeClonedVoiceEntity!!.pitchFactor,
                    activeClonedVoiceEntity!!.speedFactor,
                    activeClonedVoiceEntity!!.gender
                )
            }
            voice.id.contains("nathan") -> Triple(0.85f, 0.98f, "Male")
            voice.id.contains("sarah") -> Triple(1.15f, 1.02f, "Female")
            voice.id.contains("evelyn") -> Triple(1.10f, 1.0f, "Female")
            voice.id.contains("liam") -> Triple(0.92f, 1.05f, "Male")
            else -> Triple(1.0f, 1.0f, "Neutral")
        }

        _ttsState.value = TtsUiState.Generating

        viewModelScope.launch {
            val result = repository.generateSpeech(
                voiceId = voice.id,
                voiceName = voice.name,
                text = text,
                modelId = model,
                language = lang,
                pitchFactor = pitch,
                speedFactor = speed,
                genderPreference = gender
            )

            if (result.isSuccess) {
                val generated = result.getOrThrow()
                _ttsState.value = TtsUiState.Success(generated)
                // Auto play the generated speech!
                playerHelper.playAudio(generated.filePath)
            } else {
                _ttsState.value = TtsUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "কণ্ঠ তৈরিতে ব্যর্থ হয়েছে, অনুগ্রহ করে পুনরায় চেষ্টা করুন"
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
        repository.nativeVoiceEngine.release()
    }
}
