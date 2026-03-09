package com.example.twinmindassignment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.twinmindassignment.R
import com.example.twinmindassignment.data.db.AudioChunk
import com.example.twinmindassignment.data.db.Recording
import com.example.twinmindassignment.data.db.SummaryStatus
import com.example.twinmindassignment.data.network.Resource

@Composable
fun SummarySection(
    recording: Recording?,
    transcriptResource: Resource<List<AudioChunk>>,
    onRetrySummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = recording?.summaryStatus ?: SummaryStatus.NONE

    when (status) {
        SummaryStatus.NONE -> SummaryEmptyState(
            message = stringResource(R.string.summary_will_generate_automatically_once_transcription_is_complete),
            modifier = modifier
        )

        SummaryStatus.PENDING -> SummaryLoadingState(modifier = modifier)

        SummaryStatus.GENERATING -> {
            val partial = recording?.summary
            if (partial.isNullOrEmpty()) {
                SummaryLoadingState(modifier = modifier)
            } else {
                SummaryContent(
                    summaryText = partial,
                    isGenerating = true,
                    modifier = modifier
                )
            }
        }

        SummaryStatus.DONE -> SummaryContent(
            summaryText = recording?.summary.orEmpty(),
            isGenerating = false,
            modifier = modifier
        )

        SummaryStatus.ERROR -> SummaryErrorState(
            message = recording?.summaryError ?: stringResource(R.string.summary_generation_failed),
            showRetry = transcriptResource is Resource.Success,
            onRetry = onRetrySummary,
            modifier = modifier
        )
    }
}
