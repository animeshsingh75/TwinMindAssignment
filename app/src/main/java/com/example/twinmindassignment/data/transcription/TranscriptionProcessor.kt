package com.example.twinmindassignment.data.transcription

import androidx.work.WorkManager
import com.example.twinmindassignment.data.db.SummaryStatus
import com.example.twinmindassignment.data.repo.SummaryRepo
import com.example.twinmindassignment.data.repo.TranscriptRepo
import com.example.twinmindassignment.data.summary.SummaryWorker
import com.example.twinmindassignment.model.ChunkStatus
import com.example.twinmindassignment.util.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionProcessor @Inject constructor(
    private val repo: TranscriptRepo,
    private val summaryRepo: SummaryRepo,
    private val workManager: WorkManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch { runLoop() }
    }

    private suspend fun runLoop() {
        repo.resetTranscribingToPending()

        while (currentCoroutineContext().isActive) {
            val chunk = repo.getNextPendingChunk()
            if (chunk == null) {
                delay(Constants.TRANSCRIPTION_POLL_INTERVAL_MS)
                continue
            }

            repo.updateChunkStatus(chunk.id, ChunkStatus.TRANSCRIBING)

            try {
                val text = repo.transcribe(chunk.filePath)
                repo.updateTranscript(chunk.id, text, ChunkStatus.DONE)
                maybeStartSummary(chunk.recordingId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                repo.updateTranscript(
                    id = chunk.id,
                    transcript = null,
                    status = ChunkStatus.ERROR,
                    error = e.localizedMessage ?: "Transcription failed"
                )
                delay(Constants.TRANSCRIPTION_RETRY_DELAY_MS)
            }
        }
    }

    private suspend fun maybeStartSummary(recordingId: Long) {
        val remaining = repo.getNonDoneChunkCount(recordingId)
        if (remaining > 0) return

        val recording = summaryRepo.getRecordingById(recordingId) ?: return
        if (recording.duration == 0L) return
        if (recording.summaryStatus != SummaryStatus.NONE) return

        summaryRepo.updateSummaryStatus(recordingId, SummaryStatus.PENDING)
        workManager.enqueue(SummaryWorker.buildRequest(recordingId))
    }
}