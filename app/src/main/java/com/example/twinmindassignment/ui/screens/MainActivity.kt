package com.example.twinmindassignment.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.twinmindassignment.service.AudioRecordingService
import com.example.twinmindassignment.R
import com.example.twinmindassignment.ui.components.RecordingItem
import com.example.twinmindassignment.ui.theme.TwinMindAssignmentTheme
import com.example.twinmindassignment.ui.viewmodel.AudioViewModel
import com.example.twinmindassignment.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        if (AudioRecordingService.isRunning.value) {
            startService(Intent(this, AudioRecordingService::class.java).apply {
                action = Constants.ACTION_RESUME
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TwinMindAssignmentTheme {
                HomeScreen(
                    onRecordingClick = { recording ->
                        val intent = Intent(this, RecordingDetailsActivity::class.java).apply {
                            putExtra(Constants.EXTRA_RECORDING_ID, recording.id)
                        }
                        startActivity(intent)
                    },
                    onStartRecording = {
                        val intent = Intent(this, RecorderActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AudioViewModel = hiltViewModel(),
    onRecordingClick: (com.example.twinmindassignment.data.db.Recording) -> Unit,
    onStartRecording: () -> Unit
) {
    val recordings by viewModel.recordings.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    val groupedRecordings = remember(recordings) {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        recordings.groupBy { dateFormat.format(Date(it.startTime)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_transcriptions)) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartRecording,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_record)
                )
            }
        }
    ) { padding ->
        if (recordings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.empty_transcriptions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedRecordings.forEach { (date, recordingsInDate) ->
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                    items(recordingsInDate, key = { it.id }) { recording ->
                        RecordingItem(recording = recording, onClick = { onRecordingClick(recording) })
                    }
                }
            }
        }
    }
}