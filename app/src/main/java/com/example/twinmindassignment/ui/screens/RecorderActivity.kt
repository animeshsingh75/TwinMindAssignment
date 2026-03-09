package com.example.twinmindassignment.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.twinmindassignment.model.PauseReason
import com.example.twinmindassignment.R
import com.example.twinmindassignment.ui.components.RecordButtonWithPulse
import com.example.twinmindassignment.ui.components.TranscriptSummaryPanel
import com.example.twinmindassignment.ui.components.WaveformBars
import com.example.twinmindassignment.ui.theme.TwinMindAssignmentTheme
import com.example.twinmindassignment.ui.viewmodel.AudioViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class RecorderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TwinMindAssignmentTheme {
                RecorderScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val pauseReason by viewModel.pauseReason.collectAsState()
    val silenceWarning by viewModel.silenceWarning.collectAsState()
    val transcriptResource by viewModel.transcriptResource.collectAsState()
    val currentRecordingId by viewModel.currentRecordingId.collectAsState()
    val selectedRecording by viewModel.selectedRecording.collectAsState()
    var permissionDenied by remember { mutableStateOf(false) }

    LaunchedEffect(currentRecordingId) {
        if (currentRecordingId != -1L) viewModel.selectRecording(currentRecordingId)
    }

    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isRecording) {
        if (!isRecording) { elapsedSeconds = 0L; return@LaunchedEffect }
        while (true) {
            delay(1000)
            if (pauseReason == PauseReason.NONE) elapsedSeconds++
        }
    }
    val timerText = remember(elapsedSeconds) {
        "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
    }
    val isActivelyRecording = isRecording && pauseReason == PauseReason.NONE

    val statusText = when {
        !isRecording -> stringResource(R.string.status_ready)
        pauseReason == PauseReason.PHONE_CALL -> stringResource(R.string.status_paused_phone_call)
        pauseReason == PauseReason.AUDIO_FOCUS -> stringResource(R.string.status_paused_audio_focus)
        pauseReason == PauseReason.LOW_STORAGE -> stringResource(R.string.status_stopped_low_storage)
        pauseReason == PauseReason.USER -> stringResource(R.string.status_paused)
        silenceWarning -> stringResource(R.string.status_no_audio)
        else -> stringResource(R.string.status_recording)
    }
    val statusColor = when {
        !isRecording -> MaterialTheme.colorScheme.onSurfaceVariant
        silenceWarning -> MaterialTheme.colorScheme.error
        pauseReason != PauseReason.NONE -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.startRecording(); permissionDenied = false
        } else {
            permissionDenied = true
        }
    }

    fun onRecordClicked() {
        val needed = buildList {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isEmpty()) viewModel.startRecording()
        else permissionsLauncher.launch(needed.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_recording)) },
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
    ) { innerPadding ->
        Column(modifier = modifier.fillMaxSize().padding(innerPadding)) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timerText,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Thin,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(16.dp))

                WaveformBars(isActive = isActivelyRecording, color = statusColor)

                Spacer(Modifier.height(32.dp))

                RecordButtonWithPulse(
                    isRecording = isRecording,
                    isActivelyRecording = isActivelyRecording,
                    pulseColor = MaterialTheme.colorScheme.error,
                    onClick = { if (isRecording) viewModel.stopRecording() else onRecordClicked() }
                )

                Spacer(Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                    )
                }

                if (permissionDenied) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.error_mic_permission),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            TranscriptSummaryPanel(
                transcriptResource = transcriptResource,
                recording = selectedRecording,
                onRetrySummary = { selectedRecording?.id?.let { viewModel.generateSummary(it) } },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}