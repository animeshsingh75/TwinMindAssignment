package com.example.twinmindassignment.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.twinmindassignment.R
import com.example.twinmindassignment.data.db.AudioChunk
import com.example.twinmindassignment.data.network.Resource
import com.example.twinmindassignment.model.ChunkStatus
import com.example.twinmindassignment.ui.theme.TwinMindAssignmentTheme

@Composable
fun TranscriptPanel(resource: Resource<List<AudioChunk>>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val chunks = resource.data ?: emptyList()
    val doneCount = chunks.count { it.status == ChunkStatus.DONE }
    val totalCount = chunks.size

    LaunchedEffect(doneCount) {
        if (doneCount > 0) listState.animateScrollToItem(chunks.size - 1)
    }

    Column(modifier = modifier) {
        if (totalCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.segments_done, doneCount, totalCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            resource is Resource.Error && chunks.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "⚠",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = resource.message ?: stringResource(R.string.error_loading_transcripts),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            resource is Resource.Loading && chunks.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                        Text(
                            text = stringResource(R.string.loading_transcription),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            chunks.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.empty_transcript),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chunks, key = { it.id }) { chunk ->
                        ChunkRow(chunk)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranscriptPanelEmptyPreview() {
    TwinMindAssignmentTheme {
        Surface {
            TranscriptPanel(resource = Resource.Success(emptyList()))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranscriptPanelLoadingPreview() {
    TwinMindAssignmentTheme {
        Surface {
            TranscriptPanel(resource = Resource.Loading())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranscriptPanelErrorPreview() {
    TwinMindAssignmentTheme {
        Surface {
            TranscriptPanel(resource = Resource.Error("Failed to connect to server"))
        }
    }
}