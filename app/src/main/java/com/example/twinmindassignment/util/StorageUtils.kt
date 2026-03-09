package com.example.twinmindassignment.util

import android.os.Environment
import android.os.StatFs

fun hasEnoughStorage(minBytes: Long): Boolean {
    val stat = StatFs(Environment.getExternalStorageDirectory().path)
    val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
    return availableBytes >= minBytes
}
