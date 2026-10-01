package com.example

import com.example.data.model.CartesiaVoice
import com.example.data.model.OutputFormat
import com.example.data.model.TtsRequest
import com.example.data.model.VoiceCloneResponse
import com.example.data.model.VoiceRef
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testTtsRequestSerialization() {
        val request = TtsRequest(
            model_id = "sonic-3.6",
            transcript = "হ্যালো বিশ্ব!",
            voice = VoiceRef(id = "test-voice-id"),
            output_format = OutputFormat(
                container = "mp3",
                sample_rate = 44100,
                bit_rate = 128000
            ),
            language = "bn"
        )

        val adapter = moshi.adapter(TtsRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("sonic-3.6"))
        assertTrue(json.contains("test-voice-id"))
        assertTrue(json.contains("হ্যালো বিশ্ব!"))
        assertTrue(json.contains("mp3"))
        assertTrue(json.contains("\"language\":\"bn\""))
    }

    @Test
    fun testVoiceCloneResponseDeserialization() {
        val json = """
            {
                "id": "cloned-12345",
                "name": "My Cloned Voice",
                "description": "Recorded sample",
                "language": "bn"
            }
        """.trimIndent()

        val adapter = moshi.adapter(VoiceCloneResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals("cloned-12345", response?.id)
        assertEquals("My Cloned Voice", response?.name)
        assertEquals("bn", response?.language)
    }

    @Test
    fun testCartesiaVoiceModel() {
        val voice = CartesiaVoice(
            id = "sonic-sarah-id",
            name = "Sonic Sarah",
            description = "Natural female voice",
            language = "bn",
            gender = "Female",
            isCustom = false
        )

        assertEquals("sonic-sarah-id", voice.id)
        assertEquals("Sonic Sarah", voice.name)
        assertEquals(false, voice.isCustom)
    }
}
