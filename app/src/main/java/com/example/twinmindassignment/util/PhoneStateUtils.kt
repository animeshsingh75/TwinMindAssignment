package com.example.twinmindassignment.util

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

fun registerPhoneStateListener(
    telephonyManager: TelephonyManager,
    context: Context,
    onCallStateChanged: (Int) -> Unit
): Any {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) = onCallStateChanged(state)
        }
        telephonyManager.registerTelephonyCallback(ContextCompat.getMainExecutor(context), callback)
        callback
    } else {
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) =
                onCallStateChanged(state)
        }
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        listener
    }
}

fun unregisterPhoneStateListener(telephonyManager: TelephonyManager, callback: Any) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (callback as? TelephonyCallback)?.let { telephonyManager.unregisterTelephonyCallback(it) }
    } else {
        (callback as? PhoneStateListener)?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
    }
}
