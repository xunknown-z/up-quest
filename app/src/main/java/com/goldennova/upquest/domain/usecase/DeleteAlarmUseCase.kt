package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.repository.AlarmRepository
import javax.inject.Inject

class DeleteAlarmUseCase @Inject constructor(
    private val repository: AlarmRepository,
) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching {
        repository.deleteAlarm(id)
    }
}
