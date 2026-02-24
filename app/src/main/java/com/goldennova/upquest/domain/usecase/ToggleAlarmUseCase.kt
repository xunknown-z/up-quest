package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.repository.AlarmRepository
import javax.inject.Inject

class ToggleAlarmUseCase @Inject constructor(
    private val repository: AlarmRepository,
) {
    suspend operator fun invoke(id: Long, isEnabled: Boolean): Result<Unit> = runCatching {
        repository.toggleAlarm(id, isEnabled)
    }
}
