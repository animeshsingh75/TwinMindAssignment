package com.example.twinmindassignment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.twinmindassignment.R
import com.example.twinmindassignment.data.db.AudioChunk
import com.example.twinmindassignment.model.ChunkStatus

@Composable
internal fun ChunkRow(chunk: AudioChunk) {
    val label = stringResource(R.string.segment, chunk.sequenceNumber + 1)

    when (chunk.status) {
        ChunkStatus.DONE -> DoneChunkRow(label, chunk.transcript ?: "")
        ChunkStatus.TRANSCRIBING -> TranscribingChunkRow(label)
        ChunkStatus.PENDING -> PendingChunkRow(label)
        ChunkStatus.ERROR -> ErrorChunkRow(label, chunk.errorMessage ?: stringResource(R.string.transcription_failed))
    }
}
