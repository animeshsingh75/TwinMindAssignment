package com.example.twinmindassignment.data.repo

import com.example.twinmindassignment.BuildConfig
import com.example.twinmindassignment.data.db.AudioChunk
import com.example.twinmindassignment.data.db.AudioChunkDao
import com.example.twinmindassignment.data.network.ApiService
import com.example.twinmindassignment.data.network.Resource
import com.example.twinmindassignment.model.ChunkStatus
import com.example.twinmindassignment.util.Constants
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class TranscriptRepo @Inject constructor(
    private val apiService: ApiService,
    private val audioChunkDao: AudioChunkDao
) : BaseRepo() {

    fun getChunksForRecording(recordingId: Long): Flow<List<AudioChunk>> =
        audioChunkDao.getChunksForRecording(recordingId)

    suspend fun insertChunk(chunk: AudioChunk) = audioChunkDao.insert(chunk)

    suspend fun updateChunkStatus(id: Long, status: ChunkStatus) =
        audioChunkDao.updateStatus(id, status)

    suspend fun updateTranscript(id: Long, transcript: String?, status: ChunkStatus, error: String? = null) =
        audioChunkDao.updateTranscript(id, transcript, status, error)

    suspend fun getNextPendingChunk() = audioChunkDao.getNextPendingChunk()

    suspend fun resetTranscribingToPending() = audioChunkDao.resetTranscribingToPending()

    suspend fun getNonDoneChunkCount(recordingId: Long) = audioChunkDao.getNonDoneChunkCount(recordingId)

    suspend fun transcribe(audioFilePath: String): String {
        val file = File(audioFilePath)
        
        val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val modelBody = Constants.WHISPER_MODEL.toRequestBody("text/plain".toMediaTypeOrNull())
        
        val response = safeApiCall { 
            apiService.transcribeAudio(
                authHeader = "Bearer ${BuildConfig.OPENAI_API_KEY}",
                file = body,
                model = modelBody
            ) 
        }

        return when (response) {
            is Resource.Success -> response.data?.text ?: ""
            is Resource.Error -> throw Exception(response.message)
            else -> ""
        }
    }
}
