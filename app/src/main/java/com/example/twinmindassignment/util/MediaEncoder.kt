package com.example.twinmindassignment.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer

class MediaEncoder(private val sampleRate: Int, private val bitRate: Int) {
    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    var totalSamplesRead = 0L
        private set

    fun init(outputFile: String) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        }
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }
        muxer = MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        trackIndex = -1
        muxerStarted = false
        totalSamplesRead = 0L
    }

    fun release() {
        runCatching { if (muxerStarted) muxer?.stop() }
        muxer?.release(); muxer = null
        codec?.stop(); codec?.release(); codec = null
        trackIndex = -1; muxerStarted = false
    }

    fun feed(pcm: ShortArray, count: Int) {
        val codec = codec ?: return
        var offset = 0
        while (offset < count) {
            val inputIdx = codec.dequeueInputBuffer(10_000)
            if (inputIdx < 0) break
            val buf = codec.getInputBuffer(inputIdx) ?: break
            buf.clear()
            val maxSamples = buf.remaining() / 2
            val samples = minOf(count - offset, maxSamples)
            for (i in offset until offset + samples) {
                val s = pcm[i].toInt()
                buf.put((s and 0xFF).toByte())
                buf.put((s shr 8 and 0xFF).toByte())
            }
            val ptsUs = totalSamplesRead * 1_000_000L / sampleRate
            totalSamplesRead += samples
            codec.queueInputBuffer(inputIdx, 0, samples * 2, ptsUs, 0)
            offset += samples
        }
    }

    fun drain(info: MediaCodec.BufferInfo, endOfStream: Boolean) {
        val codec = codec ?: return
        val muxer = muxer ?: return
        if (endOfStream) {
            val idx = codec.dequeueInputBuffer(10_000)
            if (idx >= 0) {
                codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
        }
        while (true) {
            val outIdx = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outIdx >= 0 -> {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (!isConfig && muxerStarted && info.size > 0 && outBuf != null) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        muxer.writeSampleData(trackIndex, outBuf, info)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
                else -> return
            }
        }
    }
}
