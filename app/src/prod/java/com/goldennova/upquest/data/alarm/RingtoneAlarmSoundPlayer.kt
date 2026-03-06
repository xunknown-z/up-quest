package com.goldennova.upquest.data.alarm

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import com.goldennova.upquest.domain.alarm.AlarmSoundPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtoneAlarmSoundPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AlarmSoundPlayer {

    private var ringtone: Ringtone? = null

    override fun play(uri: Uri?) {
        // 이미 재생 중이면 중지 후 새로 시작
        ringtone?.stop()

        val targetUri = uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(context, targetUri)
        ringtone?.play()
    }

    override fun stop() {
        ringtone?.stop()
        ringtone = null
    }
}
