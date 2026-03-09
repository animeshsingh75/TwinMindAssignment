package com.example.twinmindassignment.util

import android.media.AudioFormat
import android.media.AudioRecord
import kotlin.math.sqrt

object AudioUtils {
    fun calculateRms(buffer: ShortArray, count: Int): Int {
        if (count <= 0) return 0
        var sum = 0.0
        var sumSq = 0.0
        for (i in 0 until count) {
            val s = buffer[i].toDouble()
            sum += s
            sumSq += s * s
        }
        val avg = sum / count
        return sqrt(maxOf(0.0, (sumSq / count) - (avg * avg))).toInt()
    }

    fun getMinBufferSize(sampleRate: Int): Int {
        return maxOf(
            AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            ), 4096
        )
    }
}
