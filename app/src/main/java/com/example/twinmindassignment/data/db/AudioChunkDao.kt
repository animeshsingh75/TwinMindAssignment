package com.example.twinmindassignment.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.twinmindassignment.model.ChunkStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: AudioChunk)

    @Query("SELECT * FROM audio_chunks WHERE recordingId = :recordingId ORDER BY sequenceNumber ASC")
    fun getChunksForRecording(recordingId: Long): Flow<List<AudioChunk>>

    @Query("SELECT * FROM audio_chunks ORDER BY sequenceNumber ASC")
    fun getAllChunks(): Flow<List<AudioChunk>>

    @Query("SELECT * FROM audio_chunks WHERE (status = 'PENDING' OR status = 'ERROR') ORDER BY id ASC LIMIT 1")
    suspend fun getNextPendingChunk(): AudioChunk?

    @Query("UPDATE audio_chunks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ChunkStatus)

    @Query("UPDATE audio_chunks SET status = :status, transcript = :transcript, errorMessage = :error WHERE id = :id")
    suspend fun updateTranscript(id: Long, transcript: String?, status: ChunkStatus, error: String? = null)

    @Query("UPDATE audio_chunks SET status = 'PENDING', transcript = NULL, errorMessage = NULL WHERE status != 'DONE'")
    suspend fun resetAllToPending()

    @Query("UPDATE audio_chunks SET status = 'PENDING' WHERE status = 'TRANSCRIBING'")
    suspend fun resetTranscribingToPending()

    @Query("SELECT * FROM audio_chunks WHERE recordingId = :recordingId AND status = 'DONE' ORDER BY sequenceNumber ASC")
    suspend fun getDoneChunksForRecording(recordingId: Long): List<AudioChunk>

    @Query("SELECT COUNT(*) FROM audio_chunks WHERE recordingId = :recordingId AND status != 'DONE'")
    suspend fun getNonDoneChunkCount(recordingId: Long): Int
}