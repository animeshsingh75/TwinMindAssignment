package com.example.twinmindassignment.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.twinmindassignment.service.AudioRecordingService
import com.example.twinmindassignment.model.PauseReason
import com.example.twinmindassignment.data.db.AudioChunk
import com.example.twinmindassignment.data.db.Recording
import com.example.twinmindassignment.data.db.SummaryStatus
import com.example.twinmindassignment.data.network.Resource
import com.example.twinmindassignment.data.repo.SummaryRepo
import com.example.twinmindassignment.data.repo.TranscriptRepo
import com.example.twinmindassignment.data.summary.SummaryWorker
import com.example.twinmindassignment.model.ChunkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    app: Application,
    private val transcriptRepo: TranscriptRepo,
    private val summaryRepo: SummaryRepo,
    private val workManager: WorkManager
) : AndroidViewModel(app) {

    val isRecording: StateFlow<Boolean> = AudioRecordingService.isRunning
    val pauseReason: StateFlow<PauseReason> = AudioRecordingService.pauseReason
    val silenceWarning: StateFlow<Boolean> = AudioRecordingService.silenceWarning
    val currentRecordingId: StateFlow<Long> = AudioRecordingService.currentRecordingIdFlow

    val recordings: StateFlow<List<Recording>> = summaryRepo.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRecordingId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transcriptResource: StateFlow<Resource<List<AudioChunk>>> =
        _selectedRecordingId.flatMapLatest { id ->
            if (id == null) {
                currentRecordingId.flatMapLatest { cId ->
                    if (cId != -1L) getChunksResource(cId)
                    else flowOf(Resource.Success(emptyList()))
                }
            } else {
                getChunksResource(id)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Resource.Loading()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedRecording: StateFlow<Recording?> =
        _selectedRecordingId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else summaryRepo.getRecordingByIdFlow(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun getChunksResource(recordingId: Long): Flow<Resource<List<AudioChunk>>> {
        return transcriptRepo.getChunksForRecording(recordingId).map { chunks ->
            when {
                chunks.any { it.status == ChunkStatus.ERROR } -> {
                    Resource.Error("Some transcriptions failed", chunks)
                }
                chunks.isEmpty() || chunks.any { it.status == ChunkStatus.TRANSCRIBING || it.status == ChunkStatus.PENDING } -> {
                    Resource.Loading(chunks)
                }
                else -> Resource.Success(chunks)
            }
        }
    }

    fun selectRecording(recordingId: Long?) {
        _selectedRecordingId.value = recordingId
    }


    fun generateSummary(recordingId: Long) {
        viewModelScope.launch {
            summaryRepo.updateSummaryStatus(recordingId, SummaryStatus.PENDING)
        }
        workManager.enqueue(SummaryWorker.buildRequest(recordingId))
    }

    fun startRecording() {
        val intent = Intent(getApplication(), AudioRecordingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun stopRecording() {
        getApplication<Application>().stopService(
            Intent(getApplication(), AudioRecordingService::class.java)
        )
    }
}
