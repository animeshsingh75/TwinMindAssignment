package com.example.twinmindassignment.data.repo

import com.example.twinmindassignment.BuildConfig
import com.example.twinmindassignment.data.db.AudioChunkDao
import com.example.twinmindassignment.data.db.Recording
import com.example.twinmindassignment.data.db.RecordingDao
import com.example.twinmindassignment.data.db.SummaryStatus
import com.example.twinmindassignment.data.network.ApiService
import com.example.twinmindassignment.data.network.Resource
import com.example.twinmindassignment.util.Constants
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class SummaryRepo @Inject constructor(
    private val apiService: ApiService,
    private val recordingDao: RecordingDao,
    private val audioChunkDao: AudioChunkDao
) : BaseRepo() {

    fun getAllRecordings(): Flow<List<Recording>> = recordingDao.getAllRecordings()

    fun getRecordingByIdFlow(id: Long): Flow<Recording?> = recordingDao.getRecordingByIdFlow(id)

    suspend fun insertRecording(recording: Recording): Long = recordingDao.insertRecording(recording)

    suspend fun updateRecordingDuration(recordingId: Long, duration: Long) {
        recordingDao.updateRecordingDuration(recordingId, duration)
    }

    suspend fun getTranscript(recordingId: Long): String {
        val chunks = audioChunkDao.getDoneChunksForRecording(recordingId)
        return chunks.joinToString("\n") { it.transcript.orEmpty() }.trim()
    }

    suspend fun updateSummaryStatus(recordingId: Long, status: SummaryStatus, error: String? = null) {
        recordingDao.updateSummaryStatus(recordingId, status, error)
    }

    suspend fun updateSummary(recordingId: Long, summary: String?, status: SummaryStatus) {
        recordingDao.updateSummary(recordingId, summary, status)
    }

    suspend fun updateRecordingTitle(recordingId: Long, title: String) {
        recordingDao.updateRecordingTitle(recordingId, title)
    }

    suspend fun getRecordingById(recordingId: Long) = recordingDao.getRecordingById(recordingId)

    suspend fun getChatCompletionsStream(transcript: String): Resource<ResponseBody> {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank()) return Resource.Error("OpenAI API key is not configured.")

        val body = JSONObject().apply {
            put("model", Constants.GPT_MODEL)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", Constants.SUMMARY_SYSTEM_PROMPT.trimIndent())
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Transcript:\n$transcript")
                })
            })
        }.toString().toRequestBody("application/json".toMediaType())

        return safeApiCall {
            apiService.getChatCompletions(
                authHeader = "Bearer $apiKey",
                body = body
            )
        }
    }
}
