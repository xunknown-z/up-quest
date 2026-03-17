package com.goldennova.upquest.domain.usecase

import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAlarmsUseCase @Inject constructor(
    private val repository: AlarmRepository,
) {
    operator fun invoke(): Flow<List<Alarm>> = repository.getAlarms()
}
