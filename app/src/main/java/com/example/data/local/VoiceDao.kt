package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    @Query("SELECT * FROM cloned_voices ORDER BY createdAt DESC")
    fun getAllClonedVoices(): Flow<List<ClonedVoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClonedVoice(voice: ClonedVoiceEntity): Long

    @Query("DELETE FROM cloned_voices WHERE id = :id")
    suspend fun deleteClonedVoice(id: Long)

    @Query("SELECT * FROM generated_audios ORDER BY createdAt DESC")
    fun getAllGeneratedAudios(): Flow<List<GeneratedAudioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedAudio(audio: GeneratedAudioEntity): Long

    @Query("DELETE FROM generated_audios WHERE id = :id")
    suspend fun deleteGeneratedAudio(id: Long)
}
