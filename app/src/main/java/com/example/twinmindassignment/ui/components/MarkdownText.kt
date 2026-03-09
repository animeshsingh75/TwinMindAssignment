package com.example.twinmindassignment.ui.components

import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.twinmindassignment.ui.theme.TwinMindAssignmentTheme
import io.noties.markwon.Markwon

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val bodyStyle = MaterialTheme.typography.bodyMedium

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColor)
                textSize = bodyStyle.fontSize.value
                setLineSpacing(0f, 1.3f)
            }
        },
        update = { tv ->
            tv.setTextColor(textColor)
            markwon.setMarkdown(tv, markdown)
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MarkdownTextPreview() {
    TwinMindAssignmentTheme {
        MarkdownText(
            markdown = """
                # Markdown Preview
                
                This is a **bold** text and this is *italic*.
                
                * Item 1
                * Item 2
                
                > This is a quote.
            """.trimIndent()
        )
    }
}
