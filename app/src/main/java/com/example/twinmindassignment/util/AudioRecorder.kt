package com.example.twinmindassignment.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaRecorder

class AudioRecorder(
    private val silenceThresholdRms: Int = Constants.DEFAULT_SILENCE_THRESHOLD_RMS,
    private val silenceDurationMs: Long = Constants.DEFAULT_SILENCE_DURATION_MS,
    private val onSilent: () -> Unit,
    private val onAudioDetected: () -> Unit,
    private val onChunkFinalized: (filePath: String) -> Unit = {}
) {
    private val sampleRate = Constants.AUDIO_SAMPLE_RATE
    private val readSize = AudioUtils.getMinBufferSize(sampleRate)

    private var audioRecord: AudioRecord? = null
    private var encoder: MediaEncoder? = null

    private val overlapSamples = sampleRate * 2
    private val overlapBuffer = ShortArray(overlapSamples)
    private var overlapWritePos = 0
    private var overlapFilled = false

    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var pendingRotateFile: String? = null

    @Volatile var currentFile: String = ""
        private set

    private var silentSince = 0L
    private var warningSent = false
    private var thread: Thread? = null

    fun start(outputFile: String) {
        if (running) return
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                readSize * 4
            )
        } catch (_: SecurityException) {
            return
        }
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release(); audioRecord = null; return
        }
        currentFile = outputFile
        encoder = MediaEncoder(sampleRate, Constants.AUDIO_BIT_RATE).apply {
            init(outputFile)
        }
        audioRecord?.startRecording()
        running = true
        thread = Thread(::loop, "AudioRecorder").also { it.start() }
    }

    fun stop() {
        running = false
        audioRecord?.stop()
        thread?.join(3_000)
        thread = null
        encoder?.release()
        encoder = null
        audioRecord?.release()
        audioRecord = null
    }

    fun pause() { paused = true }
    fun resume() { paused = false }

    fun rotateChunk(newOutputFile: String) {
        pendingRotateFile = newOutputFile
    }

    fun resetSilence() {
        silentSince = 0L
        warningSent = false
    }

    private fun loop() {
        val pcm = ShortArray(readSize / 2)
        val info = MediaCodec.BufferInfo()
        while (running) {
            val read = audioRecord?.read(pcm, 0, pcm.size) ?: break
            if (read <= 0) continue

            checkSilence(pcm, read)

            if (!paused) {
                pendingRotateFile?.also { newFile ->
                    pendingRotateFile = null
                    encoder?.drain(info, endOfStream = true)
                    encoder?.release()

                    val completedFile = currentFile
                    currentFile = newFile
                    onChunkFinalized(completedFile)

                    encoder = MediaEncoder(sampleRate, Constants.AUDIO_BIT_RATE).apply {
                        init(newFile)
                        feedOverlapToEncoder(this, info)
                    }
                }
                encoder?.let {
                    it.feed(pcm, read)
                    it.drain(info, endOfStream = false)
                }
                updateOverlapBuffer(pcm, read)
            }
        }
        runCatching { encoder?.drain(MediaCodec.BufferInfo(), endOfStream = true) }
    }

    private fun updateOverlapBuffer(pcm: ShortArray, count: Int) {
        for (i in 0 until count) {
            overlapBuffer[overlapWritePos] = pcm[i]
            overlapWritePos = (overlapWritePos + 1) % overlapSamples
            if (overlapWritePos == 0 && !overlapFilled) overlapFilled = true
        }
    }

    private fun feedOverlapToEncoder(encoder: MediaEncoder, info: MediaCodec.BufferInfo) {
        val count = if (overlapFilled) overlapSamples else overlapWritePos
        if (count == 0) return
        val startPos = if (overlapFilled) overlapWritePos else 0
        val ordered = ShortArray(count) { overlapBuffer[(startPos + it) % overlapSamples] }
        encoder.feed(ordered, count)
        encoder.drain(info, endOfStream = false)
    }

    private fun checkSilence(buffer: ShortArray, count: Int) {
        val rms = AudioUtils.calculateRms(buffer, count)
        if (rms < silenceThresholdRms) {
            if (silentSince == 0L) silentSince = System.currentTimeMillis()
            if (System.currentTimeMillis() - silentSince >= silenceDurationMs && !warningSent) {
                warningSent = true
                onSilent()
            }
        } else if (warningSent || silentSince != 0L) {
            silentSince = 0L
            warningSent = false
            onAudioDetected()
        }
    }
}