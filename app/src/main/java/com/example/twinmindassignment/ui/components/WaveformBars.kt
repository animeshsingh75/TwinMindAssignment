package com.example.twinmindassignment.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.twinmindassignment.ui.theme.TwinMindAssignmentTheme

@Composable
fun WaveformBars(isActive: Boolean, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val h0 by transition.animateFloat(3f, 28f, infiniteRepeatable(tween(380), RepeatMode.Reverse, StartOffset(0)), "h0")
    val h1 by transition.animateFloat(3f, 18f, infiniteRepeatable(tween(440), RepeatMode.Reverse, StartOffset(60)), "h1")
    val h2 by transition.animateFloat(3f, 36f, infiniteRepeatable(tween(360), RepeatMode.Reverse, StartOffset(120)), "h2")
    val h3 by transition.animateFloat(3f, 22f, infiniteRepeatable(tween(480), RepeatMode.Reverse, StartOffset(180)), "h3")
    val h4 by transition.animateFloat(3f, 32f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(240)), "h4")
    val h5 by transition.animateFloat(3f, 16f, infiniteRepeatable(tween(420), RepeatMode.Reverse, StartOffset(300)), "h5")
    val h6 by transition.animateFloat(3f, 26f, infiniteRepeatable(tween(460), RepeatMode.Reverse, StartOffset(360)), "h6")

    val heights = if (isActive) listOf(h0, h1, h2, h3, h4, h5, h6) else List(7) { 3f }
    val barColor = if (isActive) color else color.copy(alpha = 0.2f)

    Row(
        modifier = modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WaveformBarsActivePreview() {
    TwinMindAssignmentTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            WaveformBars(isActive = true, color = Color.Red)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WaveformBarsInactivePreview() {
    TwinMindAssignmentTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            WaveformBars(isActive = false, color = Color.Red)
        }
    }
}
