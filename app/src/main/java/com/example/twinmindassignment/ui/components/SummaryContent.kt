package com.example.twinmindassignment.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LightbulbCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.twinmindassignment.R
import com.example.twinmindassignment.util.SummaryUtils

@Composable
fun SummaryContent(
    summaryText: String,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val parsed = remember(summaryText) { SummaryUtils.parseSummary(summaryText) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        parsed.title?.let { title ->
            item {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        parsed.summary?.let { summary ->
            item {
                SummaryCard(
                    icon = Icons.AutoMirrored.Filled.TextSnippet,
                    title = stringResource(R.string.label_summary),
                    content = summary
                )
            }
        }

        parsed.actionItems?.let { items ->
            item {
                SummaryCard(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.label_action_items),
                    content = items
                )
            }
        }

        parsed.keyPoints?.let { points ->
            item {
                SummaryCard(
                    icon = Icons.Default.LightbulbCircle,
                    title = stringResource(R.string.label_key_points),
                    content = points
                )
            }
        }

        if (isGenerating) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.generating_dot),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}