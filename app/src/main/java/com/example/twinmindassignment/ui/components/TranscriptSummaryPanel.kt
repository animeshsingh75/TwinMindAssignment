package com.example.twinmindassignment.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.twinmindassignment.R
import com.example.twinmindassignment.data.db.AudioChunk
import com.example.twinmindassignment.data.db.Recording
import com.example.twinmindassignment.data.network.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptSummaryPanel(
    transcriptResource: Resource<List<AudioChunk>>,
    recording: Recording?,
    onRetrySummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.tab_transcript)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.tab_summary)) }
            )
        }

        when (selectedTab) {
            0 -> TranscriptPanel(
                resource = transcriptResource,
                modifier = Modifier.fillMaxSize()
            )
            1 -> SummarySection(
                recording = recording,
                transcriptResource = transcriptResource,
                onRetrySummary = onRetrySummary,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
