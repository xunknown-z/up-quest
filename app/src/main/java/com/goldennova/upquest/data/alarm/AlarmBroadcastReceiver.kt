package com.goldennova.upquest.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goldennova.upquest.domain.alarm.AlarmSoundPlayer
import com.goldennova.upquest.domain.alarm.VibrationPlayer
import com.goldennova.upquest.presentation.alarmalert.AlarmAlertActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.net.toUri
import com.goldennova.upquest.domain.model.AlarmSoundMode

/**
 * AlarmManager가 발송하는 브로드캐스트를 수신해 알람음을 재생하고 알림을 발송한다.
 *
 * Android 10+ 백그라운드 Activity 실행 제한으로 인해 [AlarmAlertActivity]를 직접 실행하지 않고,
 * [NotificationHelper.showAlarmNotification]의 [setFullScreenIntent]를 통해 Activity를 띄운다.
 */
@AndroidEntryPoint
class AlarmBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmSoundPlayer: AlarmSoundPlayer

    @Inject
    lateinit var vibrationPlayer: VibrationPlayer

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmAlertActivity.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val label = intent.getStringExtra(EXTRA_ALARM_LABEL).orEmpty()
        val ringtoneUriString = intent.getStringExtra(EXTRA_RINGTONE_URI)
        val ringtoneUri = ringtoneUriString?.toUri()
        val soundMode = intent.getStringExtra(EXTRA_SOUND_MODE)
            ?.let { runCatching { AlarmSoundMode.valueOf(it) }.getOrNull() }
            ?: AlarmSoundMode.SOUND_AND_VIBRATION

        // 소리 모드에 따라 조건부 재생
        if (soundMode == AlarmSoundMode.SOUND_AND_VIBRATION) {
            alarmSoundPlayer.play(uri = ringtoneUri)
        }
        vibrationPlayer.vibrate()

        // setFullScreenIntent를 통해 잠금/백그라운드 상태에서도 AlarmAlertActivity를 표시한다
        notificationHelper.showAlarmNotification(alarmId, label)
    }

    companion object {
        /** AlarmManagerScheduler가 PendingIntent extra로 전달하는 알람 라벨 키 */
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"

        /** AlarmManagerScheduler가 PendingIntent extra로 전달하는 알람음 URI 키 */
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"

        /** AlarmManagerScheduler가 PendingIntent extra로 전달하는 소리 모드 키 */
        const val EXTRA_SOUND_MODE = "extra_sound_mode"
    }
}
