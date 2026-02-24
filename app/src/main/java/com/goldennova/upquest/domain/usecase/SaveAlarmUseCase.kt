package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.repository.AlarmRepository
import javax.inject.Inject

class SaveAlarmUseCase @Inject constructor(
    private val repository: AlarmRepository,
) {
    suspend operator fun invoke(alarm: Alarm): Result<Long> = runCatching {
        if (alarm.id == 0L) {
            repository.insertAlarm(alarm)
        } else {
            repository.updateAlarm(alarm)
            alarm.id
        }
    }
}
