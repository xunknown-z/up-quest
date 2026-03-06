package com.goldennova.upquest.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goldennova.upquest.domain.alarm.AlarmSoundPlayer
import com.goldennova.upquest.presentation.alarmalert.AlarmAlertActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * AlarmManager가 발송하는 브로드캐스트를 수신해 [AlarmAlertActivity]를 실행한다.
 *
 * Intent extra로 전달받은 alarmId를 그대로 Activity Intent에 담아 전달하며,
 * Hilt가 [AlarmAlertActivity]의 SavedStateHandle에 자동 주입한다.
 */
@AndroidEntryPoint
class AlarmBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmSoundPlayer: AlarmSoundPlayer

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmAlertActivity.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        // 알람음 재생 시작 (기본 알람음 사용)
        alarmSoundPlayer.play(uri = null)

        val activityIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            putExtra(AlarmAlertActivity.EXTRA_ALARM_ID, alarmId)
            // 백스택 없이 새 태스크로 실행 (잠금 화면 위 표시를 위해 필요)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(activityIntent)
    }
}
