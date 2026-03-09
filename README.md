# TwinMind Assignment — Audio Recording & Transcription App

An Android app built with Jetpack Compose that records audio in 30-second chunks, transcribes them in real time using OpenAI Whisper, and generates AI-powered summaries via GPT-4o-mini.

---

## Walkthrough Video

[Watch on Google Drive](https://drive.google.com/file/d/1iM6GPHyyLg_cfyJ4SSlLslQn3zW3wbax/view?usp=sharing)

---

## Download

[Download Debug APK](app/build/intermediates/apk/debug/app-debug.apk)

---

## Features

- **Continuous chunked recording** — audio is split into 30-second chunks (with 2-second overlap) using MediaCodec + MediaMuxer, encoded as AAC
- **Real-time transcription** — each chunk is queued and transcribed via OpenAI Whisper (`whisper-1`); transcript lines appear as recording progresses
- **Silence detection** — recording automatically pauses after 10 seconds of silence (RMS < 500) and resumes on audio detection
- **Audio interruption handling** — pauses on incoming calls and audio focus loss; resumes automatically
- **AI summary generation** — on-demand GPT-4o-mini summary streamed token-by-token over SSE; survives app kills via WorkManager
- **Summary sections** — Title, Summary, Action Items, Key Points rendered as formatted Markdown
- **Storage guard** — refuses to start recording if < 50 MB free
- **Foreground service** — recording continues in the background with a persistent notification; supports pause/resume/stop actions from the notification

---

## Architecture

```
TwinMindApplication
├── AudioRecordingService (Foreground Service)
│   └── AudioRecorder (MediaCodec + MediaMuxer, AAC, 44.1 kHz / 128 kbps)
│       └── onChunkFinalized → saves AudioChunk to Room DB
├── TranscriptionProcessor (Singleton coroutine loop)
│   └── Polls PENDING chunks → OpenAI Whisper → updates chunk transcript in Room
├── SummaryWorker (HiltWorker / WorkManager)
│   └── Streams OpenAI Chat Completions SSE → writes tokens to Recording.summary
└── UI (Jetpack Compose)
    ├── MainActivity — recording list
    ├── RecorderActivity — live recording + transcript feed
    └── RecordingDetailsActivity — transcript + summary panel
```
**Key libraries:** Hilt · Room · WorkManager · Retrofit · Jetpack Compose · Kotlin Coroutines

---

## Setup

1. Clone the repo and open in Android Studio.
2. In the `local.properties` file add your OpenAI API key:

```properties
OPENAI_API_KEY="....."
```

3. Build and run on a physical device (API 24+). Microphone permission is required.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| DI | Hilt + KSP |
| Database | Room |
| Background work | WorkManager |
| Audio encoding | MediaCodec + MediaMuxer (AAC) |
| Transcription | OpenAI Whisper API |
| Summarization | OpenAI GPT-4o-mini (SSE streaming) |
| Networking | Retrofit + OkHttp |
| Build | AGP  · Kotlin |