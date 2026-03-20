package com.goldennova.upquest.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.presentation.alarmalert.AlarmAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmManagerScheduler(
    @param:ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val sdkVersion: Int,
) : AlarmScheduler {

    override fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) return
        val triggerAtMillis = calculateNextTriggerTime(alarm)
        val pendingIntent = buildPendingIntent(alarm) ?: return

        @Suppress("NewApi")
        if (sdkVersion >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            // SCHEDULE_EXACT_ALARM 권한 미허용 시 — 근사 알람으로 폴백
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    override fun cancel(alarm: Alarm) {
        buildPendingIntent(alarm)?.let { alarmManager.cancel(it) }
    }

    private fun buildPendingIntent(alarm: Alarm): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            // alarmId를 requestCode로 사용해 알람별로 고유한 PendingIntent를 생성
            alarm.id.toInt(),
            Intent(context, AlarmBroadcastReceiver::class.java).apply {
                putExtra(AlarmAlertActivity.EXTRA_ALARM_ID, alarm.id)
                // 알림 표시 시 라벨을 사용하기 위해 extra로 전달
                putExtra(AlarmBroadcastReceiver.EXTRA_ALARM_LABEL, alarm.label)
                // 선택된 알람음 URI 전달 (null이면 기본 알람음)
                putExtra(AlarmBroadcastReceiver.EXTRA_RINGTONE_URI, alarm.ringtoneUri)
                // 소리 모드 전달
                putExtra(AlarmBroadcastReceiver.EXTRA_SOUND_MODE, alarm.soundMode.name)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /**
     * 알람의 시/분과 반복 요일을 기준으로 다음 울릴 시각(epoch ms)을 계산한다.
     *
     * - 반복 없음: 오늘 지정 시각이 이미 지났으면 내일 같은 시각
     * - 반복 있음: 오늘부터 최대 7일 내에서 지정 요일 중 가장 가까운 미래 시각
     */
    private fun calculateNextTriggerTime(alarm: Alarm): Long {
        val now = LocalDateTime.now()
        val todayTrigger = now.toLocalDate().atTime(alarm.hour, alarm.minute)

        if (alarm.repeatDays.isEmpty()) {
            val trigger = if (todayTrigger.isAfter(now)) todayTrigger
            else todayTrigger.plusDays(1)
            return trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        for (daysAhead in 0L..7L) {
            val candidate = todayTrigger.plusDays(daysAhead)
            if (candidate.dayOfWeek in alarm.repeatDays && candidate.isAfter(now)) {
                return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }

        // 폴백 — 정상 케이스에서는 도달하지 않음
        return todayTrigger.plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
