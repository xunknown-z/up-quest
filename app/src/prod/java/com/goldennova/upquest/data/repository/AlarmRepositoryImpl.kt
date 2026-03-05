package com.goldennova.upquest.data.repository

import com.goldennova.upquest.data.datasource.RoomAlarmDataSource
import com.goldennova.upquest.data.local.mapper.toDomain
import com.goldennova.upquest.data.local.mapper.toEntity
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val dataSource: RoomAlarmDataSource,
) : AlarmRepository {

    override fun getAlarms(): Flow<List<Alarm>> =
        dataSource.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAlarmById(id: Long): Alarm? =
        dataSource.getById(id)?.toDomain()

    override suspend fun insertAlarm(alarm: Alarm): Long =
        dataSource.insert(alarm.toEntity())

    override suspend fun updateAlarm(alarm: Alarm) =
        dataSource.update(alarm.toEntity())

    override suspend fun deleteAlarm(id: Long) =
        dataSource.deleteById(id)

    override suspend fun toggleAlarm(id: Long, isEnabled: Boolean) {
        // 기존 엔티티를 조회하여 isEnabled만 변경 후 업데이트
        val entity = dataSource.getById(id) ?: return
        dataSource.update(entity.copy(isEnabled = isEnabled))
    }
}
