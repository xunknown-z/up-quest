package com.goldennova.upquest.domain.alarm

import android.net.Uri

interface AlarmSoundPlayer {
    fun play(uri: Uri?)
    fun stop()
}
