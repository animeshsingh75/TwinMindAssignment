package com.example.twinmindassignment.data.summary

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.twinmindassignment.data.db.SummaryStatus
import com.example.twinmindassignment.data.network.Resource
import com.example.twinmindassignment.data.repo.SummaryRepo
import com.example.twinmindassignment.util.Constants
import com.example.twinmindassignment.util.SummaryUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val summaryRepo: SummaryRepo
) : CoroutineWorker(appContext, params) {

    companion object {
        fun buildRequest(recordingId: Long): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SummaryWorker>()
                .setInputData(workDataOf(Constants.KEY_RECORDING_ID to recordingId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
    }

    override suspend fun doWork(): Result {
        val recordingId = inputData.getLong(Constants.KEY_RECORDING_ID, -1L)
        if (recordingId == -1L) return Result.failure(workDataOf("error" to "Invalid recording ID"))

        val existing = summaryRepo.getRecordingById(recordingId)
        if (existing?.summaryStatus == SummaryStatus.DONE) return Result.success()

        return withContext(Dispatchers.IO) {
            val transcript = summaryRepo.getTranscript(recordingId)
            if (transcript.isEmpty()) {
                summaryRepo.updateSummaryStatus(recordingId, SummaryStatus.ERROR, "No transcript available to summarize.")
                return@withContext Result.failure(workDataOf("error" to "No transcript"))
            }

            summaryRepo.updateSummaryStatus(recordingId, SummaryStatus.GENERATING)

            when (val resource = streamSummary(recordingId, transcript)) {
                is Resource.Success -> Result.success()
                is Resource.Error -> {
                    val msg = resource.message ?: "Summary generation failed."
                    summaryRepo.updateSummaryStatus(recordingId, SummaryStatus.ERROR, msg)
                    Result.failure(workDataOf("error" to msg))
                }
                else -> Result.failure()
            }
        }
    }

    private suspend fun streamSummary(recordingId: Long, transcript: String): Resource<Unit> {
        val resource = summaryRepo.getChatCompletionsStream(transcript)

        if (resource is Resource.Error) {
            return Resource.Error(resource.message ?: "Summary generation failed.")
        }

        val summaryBuilder = StringBuilder()
        val responseBody = resource.data ?: return Resource.Error("Empty response body from OpenAI.")

        SummaryUtils.streamTokens(responseBody) { token ->
            summaryBuilder.append(token)
            summaryRepo.updateSummary(recordingId, summaryBuilder.toString(), SummaryStatus.GENERATING)
        }

        val fullSummary = summaryBuilder.toString()
        summaryRepo.updateSummary(recordingId, fullSummary, SummaryStatus.DONE)

        SummaryUtils.extractTitle(fullSummary)?.let { title ->
            summaryRepo.updateRecordingTitle(recordingId, title)
        }
        
        return Resource.Success(Unit)
    }
}