package com.example.twinmindassignment.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.twinmindassignment.R
import com.example.twinmindassignment.ui.theme.TwinMindAssignmentTheme

@Composable
fun RecordButtonWithPulse(
    isRecording: Boolean,
    isActivelyRecording: Boolean,
    pulseColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale1 by transition.animateFloat(1f, 1.75f, infiniteRepeatable(tween(1100), RepeatMode.Restart), "ps1")
    val alpha1 by transition.animateFloat(0.45f, 0f, infiniteRepeatable(tween(1100), RepeatMode.Restart), "pa1")
    val scale2 by transition.animateFloat(1f, 1.75f, infiniteRepeatable(tween(1100), RepeatMode.Restart, StartOffset(370)), "ps2")
    val alpha2 by transition.animateFloat(0.45f, 0f, infiniteRepeatable(tween(1100), RepeatMode.Restart, StartOffset(370)), "pa2")
    val scale3 by transition.animateFloat(1f, 1.75f, infiniteRepeatable(tween(1100), RepeatMode.Restart, StartOffset(740)), "ps3")
    val alpha3 by transition.animateFloat(0.45f, 0f, infiniteRepeatable(tween(1100), RepeatMode.Restart, StartOffset(740)), "pa3")

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(160.dp)) {
        if (isActivelyRecording) {
            Box(
                modifier = Modifier
                    .size(88.dp * scale1)
                    .clip(CircleShape)
                    .background(pulseColor.copy(alpha = alpha1 * 0.5f))
            )
            Box(
                modifier = Modifier
                    .size(88.dp * scale2)
                    .clip(CircleShape)
                    .background(pulseColor.copy(alpha = alpha2 * 0.5f))
            )
            Box(
                modifier = Modifier
                    .size(88.dp * scale3)
                    .clip(CircleShape)
                    .background(pulseColor.copy(alpha = alpha3 * 0.5f))
            )
        }
        Button(
            onClick = onClick,
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) stringResource(R.string.cd_stop_recording) else stringResource(R.string.cd_start_recording),
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordButtonIdlePreview() {
    TwinMindAssignmentTheme {
        Surface {
            RecordButtonWithPulse(
                isRecording = false,
                isActivelyRecording = false,
                pulseColor = Color.Red,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordButtonRecordingPreview() {
    TwinMindAssignmentTheme {
        Surface {
            RecordButtonWithPulse(
                isRecording = true,
                isActivelyRecording = true,
                pulseColor = Color.Red,
                onClick = {}
            )
        }
    }
}
