package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.repository.AlarmRepository
import javax.inject.Inject

class GetAlarmByIdUseCase @Inject constructor(
    private val repository: AlarmRepository,
) {
    suspend operator fun invoke(id: Long): Result<Alarm?> = runCatching {
        repository.getAlarmById(id)
    }
}
