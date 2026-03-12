package com.goldennova.upquest.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goldennova.upquest.domain.alarm.AlarmSoundPlayer
import com.goldennova.upquest.presentation.alarmalert.AlarmAlertActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmAlertActivity.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val label = intent.getStringExtra(EXTRA_ALARM_LABEL).orEmpty()

        // 알람음 재생 시작 (기본 알람음 사용)
        alarmSoundPlayer.play(uri = null)

        // setFullScreenIntent를 통해 잠금/백그라운드 상태에서도 AlarmAlertActivity를 표시한다
        notificationHelper.showAlarmNotification(alarmId, label)
    }

    companion object {
        /** AlarmManagerScheduler가 PendingIntent extra로 전달하는 알람 라벨 키 */
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
    }
}
