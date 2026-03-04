package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.repository.AlarmRepository
import javax.inject.Inject

class ToggleAlarmUseCase @Inject constructor(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    suspend operator fun invoke(id: Long, isEnabled: Boolean): Result<Unit> = runCatching {
        repository.toggleAlarm(id, isEnabled)
        // 토글 후 스케줄러 연동 (활성화 → schedule, 비활성화 → cancel)
        repository.getAlarmById(id)?.let { alarm ->
            if (isEnabled) alarmScheduler.schedule(alarm)
            else alarmScheduler.cancel(alarm)
        }
    }
}
