package com.example.twinmindassignment.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.twinmindassignment.R
import com.example.twinmindassignment.ui.components.TranscriptSummaryPanel
import com.example.twinmindassignment.ui.theme.TwinMindAssignmentTheme
import com.example.twinmindassignment.ui.viewmodel.AudioViewModel
import com.example.twinmindassignment.util.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecordingDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recordingId = intent.getLongExtra(Constants.EXTRA_RECORDING_ID, -1L)
        if (recordingId == -1L) { finish(); return }

        enableEdgeToEdge()
        setContent {
            TwinMindAssignmentTheme {
                RecordingDetailsScreen(recordingId = recordingId, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailsScreen(
    recordingId: Long,
    viewModel: AudioViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val transcriptResource by viewModel.transcriptResource.collectAsState()
    val selectedRecording by viewModel.selectedRecording.collectAsState()

    LaunchedEffect(recordingId) { viewModel.selectRecording(recordingId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedRecording?.title ?: stringResource(R.string.title_recording)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        TranscriptSummaryPanel(
            transcriptResource = transcriptResource,
            recording = selectedRecording,
            onRetrySummary = { viewModel.generateSummary(recordingId) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
