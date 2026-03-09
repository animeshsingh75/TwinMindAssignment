package com.example.twinmindassignment.util

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.example.twinmindassignment.R

fun recordingSourceLabel(context: Context, audioManager: AudioManager?): String {
    if (audioManager == null) return context.getString(R.string.source_mic)
    val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
    return when {
        inputs.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        } -> context.getString(R.string.source_bluetooth)

        inputs.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
        } -> context.getString(R.string.source_wired)

        else -> context.getString(R.string.source_mic)
    }
}
