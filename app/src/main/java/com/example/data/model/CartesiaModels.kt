package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VoiceCloneResponse(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "language") val language: String? = null
)

@JsonClass(generateAdapter = true)
data class CartesiaVoice(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "language") val language: String? = "en",
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "is_custom") val isCustom: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TtsRequest(
    @Json(name = "model_id") val model_id: String = "sonic-3.6",
    @Json(name = "transcript") val transcript: String,
    @Json(name = "voice") val voice: VoiceRef,
    @Json(name = "output_format") val output_format: OutputFormat = OutputFormat(),
    @Json(name = "language") val language: String? = null
)

@JsonClass(generateAdapter = true)
data class VoiceRef(
    @Json(name = "mode") val mode: String = "id",
    @Json(name = "id") val id: String
)

@JsonClass(generateAdapter = true)
data class OutputFormat(
    @Json(name = "container") val container: String = "mp3",
    @Json(name = "sample_rate") val sample_rate: Int = 44100,
    @Json(name = "bit_rate") val bit_rate: Int = 128000
)
