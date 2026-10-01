package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.api.CartesiaApiClient
import com.example.data.local.AppDatabase
import com.example.data.local.ClonedVoiceEntity
import com.example.data.local.GeneratedAudioEntity
import com.example.data.model.CartesiaVoice
import com.example.data.model.VoiceCloneResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class CartesiaRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cartesia_settings", Context.MODE_PRIVATE)

    private val apiClient = CartesiaApiClient()
    private val db = AppDatabase.getDatabase(context)
    private val voiceDao = db.voiceDao()

    private val _apiKeyFlow = MutableStateFlow(getStoredApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    init {
        // If stored key is empty and BuildConfig has one, initialize it
        val current = getStoredApiKey()
        if (current.isBlank()) {
            val buildConfigKey = try {
                BuildConfig.CARTESIA_API_KEY
            } catch (e: Exception) {
                ""
            }
            if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "YOUR_CARTESIA_API_KEY") {
                saveApiKey(buildConfigKey)
            }
        }
    }

    fun getStoredApiKey(): String {
        val stored = prefs.getString("cartesia_api_key", "").orEmpty()
        if (stored.isNotBlank() && stored != "YOUR_CARTESIA_API_KEY") {
            return stored
        }
        val buildConfigKey = try {
            BuildConfig.CARTESIA_API_KEY
        } catch (e: Exception) {
            ""
        }
        return if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "YOUR_CARTESIA_API_KEY") {
            buildConfigKey
        } else {
            stored
        }
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString("cartesia_api_key", key.trim()).apply()
        _apiKeyFlow.value = key.trim()
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getStoredApiKey()
        return key.isNotBlank() && key != "YOUR_CARTESIA_API_KEY"
    }

    suspend fun testConnection(key: String? = null): Result<Int> {
        val activeKey = key ?: getStoredApiKey()
        if (activeKey.isBlank() || activeKey == "YOUR_CARTESIA_API_KEY") {
            return Result.failure(Exception("API Key is missing. Please enter your Cartesia API key."))
        }
        return apiClient.testApiKey(activeKey)
    }

    val allClonedVoices: Flow<List<ClonedVoiceEntity>> = voiceDao.getAllClonedVoices()
    val allGeneratedAudios: Flow<List<GeneratedAudioEntity>> = voiceDao.getAllGeneratedAudios()

    // Curated high quality Cartesia public Sonic voices
    fun getDefaultSonicVoices(): List<CartesiaVoice> {
        return listOf(
            CartesiaVoice(
                id = "694cae0b-222a-436d-9b57-61138a0f9b69",
                name = "Sonic Sarah",
                description = "Conversational, natural & expressive female voice",
                language = "bn",
                gender = "Female",
                isCustom = false
            ),
            CartesiaVoice(
                id = "a0e99841-438c-4a64-b679-ae501e7d6091",
                name = "Sonic Nathan",
                description = "Deep, clear, narrator & conversational male",
                language = "bn",
                gender = "Male",
                isCustom = false
            ),
            CartesiaVoice(
                id = "79a125e8-cd45-4c13-8a67-188112f4dd22",
                name = "Sonic Evelyn",
                description = "Warm, articulate, professional presenter",
                language = "en",
                gender = "Female",
                isCustom = false
            ),
            CartesiaVoice(
                id = "fb26447f-308b-471e-8b00-8e9f04284eb5",
                name = "Sonic Liam",
                description = "Energetic, engaging, friendly podcast host",
                language = "en",
                gender = "Male",
                isCustom = false
            ),
            CartesiaVoice(
                id = "846d0a7a-1721-4f11-aa95-95079a463584",
                name = "Sonic Multilingual",
                description = "Optimized for multilingual Bengali, Hindi, & English",
                language = "bn",
                gender = "Neutral",
                isCustom = false
            )
        )
    }

    suspend fun cloneVoice(
        clipFile: File,
        name: String,
        language: String,
        description: String
    ): Result<ClonedVoiceEntity> {
        val apiKey = getStoredApiKey()
        if (apiKey.isBlank() || apiKey == "YOUR_CARTESIA_API_KEY") {
            return Result.failure(Exception("Cartesia API Key missing. Please configure it in settings."))
        }

        val cloneResult = apiClient.cloneVoice(
            apiKey = apiKey,
            name = name,
            language = language,
            description = description,
            audioFile = clipFile
        )

        return cloneResult.mapCatching { response ->
            val voiceId = if (response.id.isNotBlank()) response.id else "clone_${System.currentTimeMillis()}"
            val voiceName = if (response.name.isNotBlank()) response.name else name

            val entity = ClonedVoiceEntity(
                cartesiaVoiceId = voiceId,
                name = voiceName,
                language = language,
                description = description.ifBlank { "Cloned from audio clip" },
                sampleAudioPath = clipFile.absolutePath,
                createdAt = System.currentTimeMillis()
            )
            val insertedId = voiceDao.insertClonedVoice(entity)
            entity.copy(id = insertedId)
        }
    }

    suspend fun deleteClonedVoice(id: Long) {
        voiceDao.deleteClonedVoice(id)
    }

    suspend fun generateSpeech(
        voiceId: String,
        voiceName: String,
        text: String,
        modelId: String = "sonic-3.6",
        language: String? = null
    ): Result<GeneratedAudioEntity> {
        val apiKey = getStoredApiKey()
        if (apiKey.isBlank() || apiKey == "YOUR_CARTESIA_API_KEY") {
            return Result.failure(Exception("Cartesia API Key missing. Please configure it in settings."))
        }

        val speechResult = apiClient.generateSpeech(
            apiKey = apiKey,
            voiceId = voiceId,
            transcript = text,
            modelId = modelId,
            language = language
        )

        return speechResult.mapCatching { bytes ->
            val outputDir = File(context.filesDir, "cartesia_speech")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val fileName = "speech_${System.currentTimeMillis()}.mp3"
            val outputFile = File(outputDir, fileName)
            FileOutputStream(outputFile).use { fos ->
                fos.write(bytes)
            }

            val entity = GeneratedAudioEntity(
                voiceId = voiceId,
                voiceName = voiceName,
                textPrompt = text,
                filePath = outputFile.absolutePath,
                durationMs = 0,
                createdAt = System.currentTimeMillis()
            )
            val insertedId = voiceDao.insertGeneratedAudio(entity)
            entity.copy(id = insertedId)
        }
    }

    suspend fun deleteGeneratedAudio(id: Long, filePath: String) {
        voiceDao.deleteGeneratedAudio(id)
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }
}
