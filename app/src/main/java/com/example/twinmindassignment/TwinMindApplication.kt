package com.example.twinmindassignment

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.twinmindassignment.data.db.RecordingDao
import com.example.twinmindassignment.data.db.SummaryStatus
import com.example.twinmindassignment.data.summary.SummaryWorker
import com.example.twinmindassignment.data.transcription.TranscriptionProcessor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TwinMindApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var transcriptionProcessor: TranscriptionProcessor
    @Inject lateinit var recordingDao: RecordingDao

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        transcriptionProcessor.start()
        recoverPendingSummaries()
    }

    private fun recoverPendingSummaries() {
        CoroutineScope(Dispatchers.IO).launch {
            recordingDao.resetGeneratingSummariesToPending()
            val recordings = recordingDao.getAllRecordings().first()
            val workManager = androidx.work.WorkManager.getInstance(this@TwinMindApplication)
            recordings.filter { it.summaryStatus == SummaryStatus.PENDING }.forEach { recording ->
                workManager.enqueue(SummaryWorker.buildRequest(recording.id))
            }
        }
    }
}
