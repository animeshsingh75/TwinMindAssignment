package com.example.twinmindassignment.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.LocusIdCompat
import com.example.twinmindassignment.service.AudioRecordingService
import com.example.twinmindassignment.model.PauseReason
import com.example.twinmindassignment.R
import com.example.twinmindassignment.ui.screens.RecorderActivity

fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(NotificationManager::class.java)
        
        val importance = NotificationManager.IMPORTANCE_HIGH
        
        val channel = NotificationChannel(
            Constants.RECORDING_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            importance
        ).apply { 
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }
}


fun buildRecordingNotification(
    context: Context,
    pauseReason: PauseReason,
    sourceLabel: String,
    effectiveStartMs: Long,
    elapsedRecordingMs: Long,
): Notification {
    ensureNotificationChannel(context)

    val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    val stopIntent = PendingIntent.getService(
        context, 0,
        Intent(context, AudioRecordingService::class.java)
            .apply { action = Constants.ACTION_STOP },
        flag
    )
    val pauseIntent = PendingIntent.getService(
        context, 1,
        Intent(context, AudioRecordingService::class.java)
            .apply { action = Constants.ACTION_PAUSE },
        flag
    )
    val resumeIntent = PendingIntent.getService(
        context, 2,
        Intent(context, AudioRecordingService::class.java)
            .apply { action = Constants.ACTION_RESUME },
        flag
    )
    val openAppIntent = PendingIntent.getActivity(
        context, 3,
        Intent(context, RecorderActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        flag
    )

    val isRecording = pauseReason == PauseReason.NONE

    val contentTitle = when (pauseReason) {
        PauseReason.NONE        -> context.getString(R.string.status_recording)
        PauseReason.USER        -> context.getString(R.string.status_paused)
        PauseReason.PHONE_CALL  -> context.getString(R.string.notification_paused_phone_call)
        PauseReason.AUDIO_FOCUS -> context.getString(R.string.notification_paused_audio_focus)
        PauseReason.LOW_STORAGE -> context.getString(R.string.notification_stopped_low_storage)
    }

    val elapsedSec = elapsedRecordingMs / 1_000
    val elapsedFormatted = "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)
    val contentText = if (isRecording) {
        context.getString(R.string.notification_via_source, sourceLabel)
    } else {
        context.getString(R.string.notification_recorded_at, elapsedFormatted)
    }

    val builder = NotificationCompat.Builder(context, Constants.RECORDING_CHANNEL_ID)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setOngoing(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setColor(if (isRecording) Color.RED else Color.DKGRAY)
        .setWhen(effectiveStartMs)
        .setUsesChronometer(isRecording)
        .setShowWhen(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(openAppIntent)
        .setLocusId(LocusIdCompat("audio_recording_session"))
        .apply {
            if (isRecording) {
                addAction(
                    android.R.drawable.ic_media_pause,
                    context.getString(R.string.notification_action_pause),
                    pauseIntent
                )
            } else if (pauseReason == PauseReason.USER) {
                addAction(
                    android.R.drawable.ic_media_play,
                    context.getString(R.string.notification_action_resume),
                    resumeIntent
                )
            }
        }
        .addAction(
            android.R.drawable.ic_delete,
            context.getString(R.string.notification_action_stop),
            stopIntent
        )

    if (Build.VERSION.SDK_INT >= 36) {
        val extras = Bundle()
        extras.putBoolean("android.requestPromotedOngoing", true)
        
        val criticalText = if (isRecording) {
            context.getString(R.string.notification_short_rec)
        } else {
            context.getString(R.string.notification_short_paused)
        }
        extras.putCharSequence("android.shortCriticalText", criticalText)
        
        builder.addExtras(extras)
    }

    return builder.build()
}