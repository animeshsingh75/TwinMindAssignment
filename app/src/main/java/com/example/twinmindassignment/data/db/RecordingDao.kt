package com.example.twinmindassignment.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: Recording): Long

    @Query("SELECT * FROM recordings ORDER BY startTime DESC")
    fun getAllRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE id = :recordingId")
    suspend fun getRecordingById(recordingId: Long): Recording?

    @Query("SELECT * FROM recordings WHERE id = :recordingId")
    fun getRecordingByIdFlow(recordingId: Long): Flow<Recording?>

    @Query("UPDATE recordings SET duration = :duration WHERE id = :recordingId")
    suspend fun updateRecordingDuration(recordingId: Long, duration: Long)

    @Query("UPDATE recordings SET summaryStatus = :status, summaryError = :error WHERE id = :id")
    suspend fun updateSummaryStatus(id: Long, status: SummaryStatus, error: String? = null)

    @Query("UPDATE recordings SET summary = :summary, summaryStatus = :status WHERE id = :id")
    suspend fun updateSummary(id: Long, summary: String?, status: SummaryStatus)

    @Query("UPDATE recordings SET title = :title WHERE id = :id")
    suspend fun updateRecordingTitle(id: Long, title: String)

    @Query("UPDATE recordings SET summaryStatus = 'PENDING' WHERE summaryStatus = 'GENERATING'")
    suspend fun resetGeneratingSummariesToPending()
}
