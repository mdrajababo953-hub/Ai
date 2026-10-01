package com.example.data.api

import com.example.data.model.CartesiaVoice
import com.example.data.model.OutputFormat
import com.example.data.model.TtsRequest
import com.example.data.model.VoiceCloneResponse
import com.example.data.model.VoiceRef
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class CartesiaApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val baseUrl = "https://api.cartesia.ai"
    private val apiVersion = "2024-06-10"

    suspend fun testApiKey(apiKey: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/voices")
                .addHeader("X-API-Key", apiKey.trim())
                .addHeader("Cartesia-Version", apiVersion)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string().orEmpty()
                    val voiceCount = try {
                        val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                        val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                        val list = adapter.fromJson(bodyString)
                        list?.size ?: 1
                    } catch (e: Exception) {
                        1
                    }
                    Result.success(voiceCount)
                } else {
                    val errorMsg = response.body?.string() ?: response.message
                    Result.failure(Exception("Cartesia API Error (${response.code}): $errorMsg"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVoices(apiKey: String): Result<List<CartesiaVoice>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/voices")
                .addHeader("X-API-Key", apiKey.trim())
                .addHeader("Cartesia-Version", apiVersion)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string().orEmpty()
                    val listType = Types.newParameterizedType(List::class.java, CartesiaVoice::class.java)
                    val adapter = moshi.adapter<List<CartesiaVoice>>(listType)
                    val list = adapter.fromJson(bodyString) ?: emptyList()
                    Result.success(list)
                } else {
                    Result.failure(Exception("Failed to fetch voices: ${response.code} ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cloneVoice(
        apiKey: String,
        name: String,
        language: String,
        description: String,
        audioFile: File
    ): Result<VoiceCloneResponse> = withContext(Dispatchers.IO) {
        try {
            val mimeType = when {
                audioFile.name.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                audioFile.name.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
                audioFile.name.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
                else -> "audio/m4a"
            }

            val fileRequestBody = audioFile.asRequestBody(mimeType.toMediaTypeOrNull())

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("clip", audioFile.name, fileRequestBody)
                .addFormDataPart("name", name)
                .addFormDataPart("language", language)
                .apply {
                    if (description.isNotBlank()) {
                        addFormDataPart("description", description)
                    }
                }
                .build()

            val request = Request.Builder()
                .url("$baseUrl/voices/clone")
                .addHeader("X-API-Key", apiKey.trim())
                .addHeader("Cartesia-Version", apiVersion)
                .post(multipartBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val adapter = moshi.adapter(VoiceCloneResponse::class.java)
                    val result = adapter.fromJson(bodyString)
                        ?: VoiceCloneResponse(id = "voice_${System.currentTimeMillis()}", name = name)
                    Result.success(result)
                } else {
                    Result.failure(Exception("Voice clone failed (${response.code}): $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateSpeech(
        apiKey: String,
        voiceId: String,
        transcript: String,
        modelId: String = "sonic-3.6",
        language: String? = null
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val ttsRequest = TtsRequest(
                model_id = modelId,
                transcript = transcript,
                voice = VoiceRef(id = voiceId),
                output_format = OutputFormat(
                    container = "mp3",
                    sample_rate = 44100,
                    bit_rate = 128000
                ),
                language = language
            )

            val adapter = moshi.adapter(TtsRequest::class.java)
            val jsonString = adapter.toJson(ttsRequest)

            val requestBody = jsonString.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/tts/bytes")
                .addHeader("X-API-Key", apiKey.trim())
                .addHeader("Cartesia-Version", apiVersion)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        Result.success(bytes)
                    } else {
                        Result.failure(Exception("Cartesia returned empty audio response"))
                    }
                } else {
                    val err = response.body?.string().orEmpty()
                    Result.failure(Exception("TTS generation failed (${response.code}): $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
