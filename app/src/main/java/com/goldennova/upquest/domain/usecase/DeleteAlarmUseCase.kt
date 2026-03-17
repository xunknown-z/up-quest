package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.repository.AlarmRepository
import javax.inject.Inject

class DeleteAlarmUseCase @Inject constructor(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching {
        // 삭제 전 스케줄러 취소
        repository.getAlarmById(id)?.let { alarmScheduler.cancel(it) }
        repository.deleteAlarm(id)
    }
}
