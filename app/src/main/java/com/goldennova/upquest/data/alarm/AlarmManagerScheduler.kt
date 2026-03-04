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
import javax.inject.Inject

class AlarmManagerScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
) : AlarmScheduler {

    override fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) return
        val triggerAtMillis = calculateNextTriggerTime(alarm)
        val pendingIntent = buildPendingIntent(alarm.id) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
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
        buildPendingIntent(alarm.id)?.let { alarmManager.cancel(it) }
    }

    private fun buildPendingIntent(alarmId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            // alarmId를 requestCode로 사용해 알람별로 고유한 PendingIntent를 생성
            alarmId.toInt(),
            Intent(context, AlarmBroadcastReceiver::class.java).apply {
                putExtra(AlarmAlertActivity.EXTRA_ALARM_ID, alarmId)
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
