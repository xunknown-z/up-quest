package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.repository.AlarmRepository
import javax.inject.Inject

class SaveAlarmUseCase @Inject constructor(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    suspend operator fun invoke(alarm: Alarm): Result<Long> = runCatching {
        val savedId = if (alarm.id == 0L) {
            repository.insertAlarm(alarm)
        } else {
            repository.updateAlarm(alarm)
            alarm.id
        }
        // 저장된 ID로 알람 객체 보정 후 스케줄러 연동
        val savedAlarm = alarm.copy(id = savedId)
        if (savedAlarm.isEnabled) alarmScheduler.schedule(savedAlarm)
        else alarmScheduler.cancel(savedAlarm)
        savedId
    }
}
