package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloned_voices")
data class ClonedVoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cartesiaVoiceId: String,
    val name: String,
    val language: String = "bn",
    val description: String = "",
    val sampleAudioPath: String? = null,
    val pitchFactor: Float = 1.0f,
    val speedFactor: Float = 1.0f,
    val gender: String = "Neutral",
    val toneStyle: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "generated_audios")
data class GeneratedAudioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val voiceId: String,
    val voiceName: String,
    val textPrompt: String,
    val filePath: String,
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
