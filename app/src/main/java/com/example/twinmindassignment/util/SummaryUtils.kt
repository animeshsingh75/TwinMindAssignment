package com.example.twinmindassignment.util

import com.example.twinmindassignment.model.ParsedSummary
import okhttp3.ResponseBody
import org.json.JSONObject

object SummaryUtils {
    fun parseSummary(text: String): ParsedSummary {
        val sections = mutableMapOf<String, StringBuilder>()
        var current: String? = null
        for (line in text.lines()) {
            if (line.startsWith("## ")) {
                current = line.removePrefix("## ").trim()
                sections[current] = StringBuilder()
            } else {
                current?.let { sections[it]?.append(line)?.append('\n') }
            }
        }
        fun get(key: String) = sections[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        return ParsedSummary(
            title       = get("Title"),
            summary     = get("Summary"),
            actionItems = get("Action Items"),
            keyPoints   = get("Key Points")
        )
    }

    fun extractTitle(summaryText: String): String? {
        var inTitle = false
        for (line in summaryText.lines()) {
            when {
                line.startsWith("## ") -> inTitle = line.removePrefix("## ").trim() == "Title"
                inTitle && line.isNotBlank() -> return line.trim()
            }
        }
        return null
    }

    fun parseTokenFromStreamingJson(data: String): String? {
        return try {
            val json = JSONObject(data)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val delta = choices.getJSONObject(0).optJSONObject("delta")
                delta?.optString("content")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun streamTokens(
        responseBody: ResponseBody,
        onTokenReceived: suspend (String) -> Unit
    ) {
        responseBody.use { respBody ->
            val source = respBody.source()
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                parseTokenFromStreamingJson(data)?.let { token ->
                    if (token.isNotEmpty()) {
                        onTokenReceived(token)
                    }
                }
            }
        }
    }
}
