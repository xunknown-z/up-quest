package com.goldennova.upquest.data.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
        ringtone = RingtoneManager.getRingtone(context, targetUri)?.apply {
            // USAGE_ALARM으로 설정해야 알람 오디오 스트림으로 재생됨
            // 미설정 시 기본 스트림(Ring/Notification)으로 재생되어 묵음 상태일 수 있음
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // API 28+에서 알람이 해제될 때까지 반복 재생
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
        }
        ringtone?.play()
    }

    override fun stop() {
        ringtone?.stop()
        ringtone = null
    }
}
