package com.example.twinmindassignment.util

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

fun requestAudioFocus(
    audioManager: AudioManager,
    listener: AudioManager.OnAudioFocusChangeListener
): AudioFocusRequest? {
    val mainHandler = Handler(Looper.getMainLooper())
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(listener, mainHandler)
            .build()
        audioManager.requestAudioFocus(req)
        req
    } else {
        audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        null
    }
}

fun abandonAudioFocus(
    audioManager: AudioManager,
    request: AudioFocusRequest?,
    listener: AudioManager.OnAudioFocusChangeListener
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        request?.let { audioManager.abandonAudioFocusRequest(it) }
    } else {
        audioManager.abandonAudioFocus(listener)
    }
}
