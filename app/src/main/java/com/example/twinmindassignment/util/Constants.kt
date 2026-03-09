package com.example.twinmindassignment.util

object Constants {
    // Service Actions
    const val ACTION_STOP = "com.example.twinmindassignment.ACTION_STOP"
    const val ACTION_PAUSE = "com.example.twinmindassignment.ACTION_PAUSE"
    const val ACTION_RESUME = "com.example.twinmindassignment.ACTION_RESUME"
    const val ACTION_DISMISS_SILENCE = "com.example.twinmindassignment.ACTION_DISMISS_SILENCE"

    // Recording Configuration
    const val CHUNK_DURATION_MS = 30_000L
    const val OVERLAP_DURATION_MS = 2_000L
    const val MIN_STORAGE_BYTES = 50L * 1024 * 1024  // 50 MB
    const val MIN_DEVICE_ROTATION_DELAY_MS = 5_000L
    const val AUDIO_SAMPLE_RATE = 44100
    const val AUDIO_BIT_RATE = 128_000
    
    // Silence Detection
    const val DEFAULT_SILENCE_THRESHOLD_RMS = 500
    const val DEFAULT_SILENCE_DURATION_MS = 10_000L

    // Notification
    const val RECORDING_CHANNEL_ID = "recording_channel"
    const val RECORDING_NOTIFICATION_ID = 1

    // WorkManager & Intent Keys
    const val KEY_RECORDING_ID = "recording_id"
    const val EXTRA_RECORDING_ID = "extra_recording_id"

    // OpenAI Configuration
    const val WHISPER_MODEL = "whisper-1"
    const val GPT_MODEL = "gpt-4o-mini"
    const val OPENAI_API_URL = "https://api.openai.com/v1/"
    const val SUMMARY_READ_TIMEOUT_SECONDS = 120L
    const val SUMMARY_CONNECT_TIMEOUT_SECONDS = 30L

    // Transcription Processor
    const val TRANSCRIPTION_POLL_INTERVAL_MS = 500L
    const val TRANSCRIPTION_RETRY_DELAY_MS = 5_000L

    const val SUMMARY_SYSTEM_PROMPT = """
        You are a helpful assistant that summarizes transcripts.
        Respond with ONLY the following four markdown sections, in this exact order, with no extra text before or after:

        ## Title
        (one concise title, max 10 words, on its own line below the heading)

        ## Summary
        (2-3 sentence overview, on lines below the heading)

        ## Action Items
        (bullet list below the heading; write "- None identified" if none)

        ## Key Points
        (bullet list below the heading)

        IMPORTANT: Put each section's content on the line(s) AFTER the ## heading, never on the same line as the heading.
    """
}
