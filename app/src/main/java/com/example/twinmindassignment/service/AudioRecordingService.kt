package com.example.twinmindassignment.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import com.example.twinmindassignment.R
import com.example.twinmindassignment.data.db.AudioChunk
import com.example.twinmindassignment.data.db.Recording
import com.example.twinmindassignment.data.repo.SummaryRepo
import com.example.twinmindassignment.data.repo.TranscriptRepo
import com.example.twinmindassignment.data.transcription.TranscriptionProcessor
import com.example.twinmindassignment.model.PauseReason
import com.example.twinmindassignment.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class AudioRecordingService : Service() {

    @Inject lateinit var transcriptRepo: TranscriptRepo
    @Inject lateinit var summaryRepo: SummaryRepo
    @Inject lateinit var transcriptionProcessor: TranscriptionProcessor

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val chunkSequence = AtomicInteger(0)
    
    private lateinit var audioRecorder: AudioRecorder
    private var recordingStartMs = 0L
    private var currentRecordingId: Long = -1
    private var recorderPaused = false
    
    private var pausedForPhoneCall = false
    private var pausedForAudioFocus = false
    private var pausedByUser = false
    private var totalPausedMs = 0L
    private var pauseStartMs  = 0L

    private lateinit var telephonyManager: TelephonyManager
    private var telephonyCallback: Any? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> { pausedForAudioFocus = false; refreshPauseState() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { pausedForAudioFocus = true; refreshPauseState() }
            AudioManager.AUDIOFOCUS_LOSS -> { pausedForAudioFocus = true; refreshPauseState(); teardownAudioFocus() }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) = handleAudioDeviceChange()
        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = handleAudioDeviceChange()
    }

    private val rotateRunnable = object : Runnable {
        override fun run() {
            if (!recorderPaused) audioRecorder.rotateChunk(chunkFile())
            handler.postDelayed(this, Constants.CHUNK_DURATION_MS - Constants.OVERLAP_DURATION_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _pauseReason.value = PauseReason.NONE
        if (!hasEnoughStorage(Constants.MIN_STORAGE_BYTES)) {
            _pauseReason.value = PauseReason.LOW_STORAGE
            startForeground(Constants.RECORDING_NOTIFICATION_ID, createNotification())
            stopSelf(); return
        }

        audioRecorder = AudioRecorder(
            onSilent = { _silenceWarning.value = true },
            onAudioDetected = { _silenceWarning.value = false },
            onChunkFinalized = { path ->
                val seq = chunkSequence.getAndIncrement()
                serviceScope.launch { if (currentRecordingId != -1L) transcriptRepo.insertChunk(AudioChunk(recordingId = currentRecordingId, sequenceNumber = seq, filePath = path)) }
            }
        )

        _isRunning.value = true
        recordingStartMs = System.currentTimeMillis()
        serviceScope.launch {
            currentRecordingId = summaryRepo.insertRecording(Recording(title = getString(R.string.recording_title_prefix, System.currentTimeMillis())))
            _currentRecordingIdFlow.value = currentRecordingId
        }

        startForeground(Constants.RECORDING_NOTIFICATION_ID, createNotification())
        setupAudioFocus()
        audioRecorder.start(chunkFile())
        handler.postDelayed(rotateRunnable, Constants.CHUNK_DURATION_MS)
        setupPhoneStateListener()
        getSystemService(AudioManager::class.java)?.registerAudioDeviceCallback(audioDeviceCallback, handler)
        transcriptionProcessor.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_STOP -> stopSelf()
            Constants.ACTION_PAUSE -> { pausedByUser = true; refreshPauseState() }
            Constants.ACTION_RESUME -> {
                pausedForAudioFocus = false; pausedByUser = false; refreshPauseState()
                if (!pausedForPhoneCall) setupAudioFocus()
            }
            Constants.ACTION_DISMISS_SILENCE -> { audioRecorder.resetSilence(); _silenceWarning.value = false }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        getSystemService(AudioManager::class.java)?.unregisterAudioDeviceCallback(audioDeviceCallback)
        teardownPhoneStateListener(); teardownAudioFocus()
        handler.removeCallbacks(rotateRunnable); handler.removeCallbacks(deviceChangeRunnable)

        val lastFile = audioRecorder.currentFile
        audioRecorder.stop()

        if (lastFile.isNotEmpty() && currentRecordingId != -1L) {
            runBlocking(Dispatchers.IO) {
                transcriptRepo.insertChunk(AudioChunk(recordingId = currentRecordingId, sequenceNumber = chunkSequence.getAndIncrement(), filePath = lastFile))
                summaryRepo.updateRecordingDuration(currentRecordingId, System.currentTimeMillis() - recordingStartMs)
            }
        }

        serviceScope.cancel()
        _isRunning.value = false; _currentRecordingIdFlow.value = -1L; _silenceWarning.value = false
        if (_pauseReason.value != PauseReason.LOW_STORAGE) _pauseReason.value = PauseReason.NONE
    }

    private fun chunkFile() = "${externalCacheDir?.absolutePath}/${System.currentTimeMillis()}.mp4"

    private fun refreshPauseState() {
        _pauseReason.value = when {
            pausedForPhoneCall  -> PauseReason.PHONE_CALL
            pausedForAudioFocus -> PauseReason.AUDIO_FOCUS
            pausedByUser        -> PauseReason.USER
            else                -> PauseReason.NONE
        }
        val shouldPause = _pauseReason.value != PauseReason.NONE
        if (shouldPause && !recorderPaused) {
            audioRecorder.pause(); recorderPaused = true; pauseStartMs = System.currentTimeMillis()
        } else if (!shouldPause && recorderPaused) {
            audioRecorder.resume(); recorderPaused = false
            if (pauseStartMs > 0) { totalPausedMs += System.currentTimeMillis() - pauseStartMs; pauseStartMs = 0 }
        }
        updateNotification()
    }

    private val deviceChangeRunnable = Runnable {
        if (!recorderPaused && (System.currentTimeMillis() - recordingStartMs) >= Constants.MIN_DEVICE_ROTATION_DELAY_MS) {
            if (hasEnoughStorage(Constants.MIN_STORAGE_BYTES)) audioRecorder.rotateChunk(chunkFile())
            else stopDueToLowStorage()
        }
        updateNotification()
    }

    private fun handleAudioDeviceChange() {
        handler.removeCallbacks(deviceChangeRunnable); handler.postDelayed(deviceChangeRunnable, 500)
    }

    private fun stopDueToLowStorage() {
        _pauseReason.value = PauseReason.LOW_STORAGE
        audioRecorder.stop(); updateNotification(); stopSelf()
    }

    private fun setupPhoneStateListener() {
        telephonyManager = getSystemService(TelephonyManager::class.java)
        telephonyCallback = registerPhoneStateListener(telephonyManager, this) { state ->
            pausedForPhoneCall = state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK
            refreshPauseState()
        }
    }

    private fun teardownPhoneStateListener() { telephonyCallback?.let { unregisterPhoneStateListener(telephonyManager, it) } }

    private fun setupAudioFocus() { audioFocusRequest = requestAudioFocus(getSystemService(AudioManager::class.java), audioFocusListener) }

    private fun teardownAudioFocus() { abandonAudioFocus(getSystemService(AudioManager::class.java), audioFocusRequest, audioFocusListener) }

    private fun updateNotification() { getSystemService(NotificationManager::class.java)?.notify(Constants.RECORDING_NOTIFICATION_ID, createNotification()) }

    private fun createNotification(): Notification {
        val start = recordingStartMs + totalPausedMs
        val elapsed = if (!recorderPaused) System.currentTimeMillis() - start else pauseStartMs - recordingStartMs - totalPausedMs
        return buildRecordingNotification(this, _pauseReason.value, recordingSourceLabel(this, getSystemService(AudioManager::class.java)), start, elapsed)
    }

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private val _pauseReason = MutableStateFlow(PauseReason.NONE)
        val pauseReason: StateFlow<PauseReason> = _pauseReason

        private val _silenceWarning = MutableStateFlow(false)
        val silenceWarning: StateFlow<Boolean> = _silenceWarning

        private val _currentRecordingIdFlow = MutableStateFlow(-1L)
        val currentRecordingIdFlow: StateFlow<Long> = _currentRecordingIdFlow
    }
}
