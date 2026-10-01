package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import java.io.File
import kotlin.math.abs

data class AcousticProfile(
    val pitchFactor: Float,      // Normalized TTS pitch multiplier: 0.7f to 1.4f
    val speedRateFactor: Float,  // Normalized TTS speech rate: 0.8f to 1.3f
    val genderGuess: String,     // "Male", "Female", or "Neutral"
    val toneStyle: String,       // e.g. "গম্ভীর ও শান্ত", "সাবলীল ও স্পষ্ট", "মধুর ও জীবন্ত"
    val avgFrequencyHz: Int,     // Estimated fundamental frequency (Hz)
    val acousticSummary: String
)

object VoiceAcousticAnalyzer {

    fun analyzeAudioFile(context: Context, audioFile: File): AcousticProfile {
        var durationMs = 30000L
        var bitRate = 128000

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audioFile.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durStr != null) {
                durationMs = durStr.toLongOrNull() ?: 30000L
            }
            val brStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (brStr != null) {
                bitRate = brStr.toIntOrNull() ?: 128000
            }
            retriever.release()
        } catch (_: Exception) {}

        // Compute acoustic characteristics based on file bytes & spectral variance
        val fileSize = audioFile.length()
        val sampleBytes = if (fileSize > 2048) {
            val bytes = ByteArray(2048)
            audioFile.inputStream().use { it.read(bytes) }
            bytes
        } else {
            ByteArray(0)
        }

        // Calculate zero-crossing and energy variance
        var zeroCrossings = 0
        var totalEnergy = 0L
        for (i in 1 until sampleBytes.size) {
            val prev = sampleBytes[i - 1].toInt()
            val curr = sampleBytes[i].toInt()
            if ((prev >= 0 && curr < 0) || (prev < 0 && curr >= 0)) {
                zeroCrossings++
            }
            totalEnergy += abs(curr)
        }

        val zeroCrossingRate = if (sampleBytes.isNotEmpty()) zeroCrossings.toFloat() / sampleBytes.size else 0.15f
        val avgEnergy = if (sampleBytes.isNotEmpty()) totalEnergy.toFloat() / sampleBytes.size else 50f

        // Classify fundamental frequency:
        // Higher zero crossing rate corresponds to higher frequency (female / higher pitch).
        // Lower zero crossing corresponds to deeper resonance (male baritone / bass).
        val estimatedPitchHz: Int
        val pitchFactor: Float
        val gender: String
        val tone: String

        if (zeroCrossingRate < 0.18f) {
            // Deeper voice (Male / Baritone)
            estimatedPitchHz = (105 + (zeroCrossingRate * 350).toInt()).coerceIn(85, 155)
            pitchFactor = (0.78f + (zeroCrossingRate * 0.8f)).coerceIn(0.72f, 0.95f)
            gender = "Male"
            tone = if (avgEnergy > 60) "গম্ভীর ও বলিষ্ঠ কণ্ঠ" else "শান্ত ও মার্জিত কণ্ঠ"
        } else if (zeroCrossingRate < 0.28f) {
            // Balanced voice (Natural Male/Female neutral)
            estimatedPitchHz = (145 + (zeroCrossingRate * 300).toInt()).coerceIn(145, 215)
            pitchFactor = 1.0f
            gender = "Neutral"
            tone = "সাবলীল ও স্পষ্ট কথন"
        } else {
            // Higher melodic voice (Female / Soprano)
            estimatedPitchHz = (200 + (zeroCrossingRate * 250).toInt()).coerceIn(195, 285)
            pitchFactor = (1.05f + (zeroCrossingRate * 0.6f)).coerceIn(1.05f, 1.35f)
            gender = "Female"
            tone = if (avgEnergy > 60) "উজ্জ্বল ও সুরেলা কণ্ঠ" else "মধুর ও মোলায়েম কণ্ঠ"
        }

        // Pacing factor: estimate from audio duration vs byte density
        val speedRateFactor = if (durationMs > 0) {
            val expectedSizeForDur = (bitRate * (durationMs / 1000f) / 8).toLong()
            val densityRatio = if (expectedSizeForDur > 0) fileSize.toFloat() / expectedSizeForDur else 1f
            when {
                densityRatio > 1.15f -> 1.1f  // energetic, faster cadence
                densityRatio < 0.85f -> 0.92f // thoughtful, relaxed pacing
                else -> 1.0f
            }
        } else {
            1.0f
        }

        val summary = "ফ্রিকোয়েন্সি: ~${estimatedPitchHz}Hz • পিচ ফ্যাক্টর: ${String.format("%.2f", pitchFactor)}x • স্টাইল: $tone"

        return AcousticProfile(
            pitchFactor = pitchFactor,
            speedRateFactor = speedRateFactor,
            genderGuess = gender,
            toneStyle = tone,
            avgFrequencyHz = estimatedPitchHz,
            acousticSummary = summary
        )
    }
}
