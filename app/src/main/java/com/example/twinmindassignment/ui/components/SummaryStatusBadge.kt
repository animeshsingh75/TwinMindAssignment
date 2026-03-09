package com.example.twinmindassignment.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.twinmindassignment.R
import com.example.twinmindassignment.data.db.SummaryStatus

@Composable
fun SummaryStatusBadge(status: SummaryStatus, modifier: Modifier = Modifier) {
    val (badgeText, badgeContainer, badgeTextColor) = when (status) {
        SummaryStatus.DONE -> Triple(
            stringResource(R.string.badge_summary_ready),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        SummaryStatus.GENERATING -> Triple(
            stringResource(R.string.badge_summarizing),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        SummaryStatus.PENDING -> Triple(
            stringResource(R.string.badge_summary_queued),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        SummaryStatus.ERROR -> Triple(
            stringResource(R.string.badge_summary_failed),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        SummaryStatus.NONE -> Triple(null, Color.Transparent, Color.Transparent)
    }

    if (badgeText != null) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            color = badgeContainer
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = badgeTextColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}