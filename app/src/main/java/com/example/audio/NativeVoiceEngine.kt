package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

class NativeVoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val initDeferred = CompletableDeferred<Boolean>()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            initDeferred.complete(true)
        } else {
            isInitialized = false
            initDeferred.complete(false)
        }
    }

    suspend fun awaitInitialization(): Boolean {
        return if (isInitialized) true else initDeferred.await()
    }

    /**
     * Synthesize given text into an audio file (.wav) using the specified acoustic profile
     * and language locale.
     */
    suspend fun synthesizeSpeechToFile(
        text: String,
        languageCode: String,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f,
        genderPreference: String = "Neutral"
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!awaitInitialization()) {
            return@withContext Result.failure(Exception("Native Speech Engine failed to initialize."))
        }

        val engine = tts ?: return@withContext Result.failure(Exception("TTS engine unavailable"))

        // Set locale
        val targetLocale = when (languageCode.lowercase()) {
            "bn", "bengali" -> Locale("bn", "BD")
            "hi", "hindi" -> Locale("hi", "IN")
            "en", "english" -> Locale.US
            else -> Locale.getDefault()
        }

        val langResult = engine.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to generic Bengali or English if specific locale not installed
            if (languageCode == "bn") {
                engine.setLanguage(Locale("bn"))
            } else {
                engine.setLanguage(Locale.US)
            }
        }

        // Apply Voice Acoustic Characteristics
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))

        // Find best matching voice if available
        try {
            val availableVoices = engine.voices
            if (!availableVoices.isNullOrEmpty()) {
                val matched = availableVoices.find { voice ->
                    val voiceLang = voice.locale.language.equals(targetLocale.language, ignoreCase = true)
                    val matchesGender = when (genderPreference.lowercase()) {
                        "male" -> voice.name.contains("male", ignoreCase = true) && !voice.name.contains("female", ignoreCase = true)
                        "female" -> voice.name.contains("female", ignoreCase = true)
                        else -> true
                    }
                    voiceLang && matchesGender
                } ?: availableVoices.find { it.locale.language.equals(targetLocale.language, ignoreCase = true) }

                if (matched != null) {
                    engine.voice = matched
                }
            }
        } catch (_: Exception) {}

        // Output destination file
        val outputDir = File(context.filesDir, "generated_speech")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "speech_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.wav")

        val utteranceId = "utt_${System.currentTimeMillis()}"
        val synthesisDeferred = CompletableDeferred<Boolean>()

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) {}

            override fun onDone(uttId: String?) {
                if (uttId == utteranceId) {
                    synthesisDeferred.complete(true)
                }
            }

            override fun onError(uttId: String?) {
                if (uttId == utteranceId) {
                    synthesisDeferred.complete(false)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(uttId: String?, errorCode: Int) {
                if (uttId == utteranceId) {
                    synthesisDeferred.complete(false)
                }
            }
        })

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        // Apply natural conversational human prosody cadence
        val humanizedText = HumanVoiceEnhancer.humanizeTextProsody(text)

        val synthStatus = engine.synthesizeToFile(humanizedText, params, outputFile, utteranceId)
        if (synthStatus != TextToSpeech.SUCCESS) {
            return@withContext Result.failure(Exception("Failed to schedule speech synthesis"))
        }

        val completed = synthesisDeferred.await()
        if (completed && outputFile.exists() && outputFile.length() > 0) {
            // Apply DSP resonance filter to eliminate robotic metallic buzz & add human chest warmth
            val finalAudioFile = HumanVoiceEnhancer.enhanceWavFile(outputFile, pitch)
            Result.success(finalAudioFile)
        } else {
            Result.failure(Exception("Speech synthesis file generation incomplete"))
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
