package com.goldennova.upquest.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.goldennova.upquest.data.datasource.FakeAlarmDataSource
import com.goldennova.upquest.domain.model.Alarm
import com.goldennova.upquest.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAlarmRepository @Inject constructor(
    private val dataSource: FakeAlarmDataSource,
) : AlarmRepository {

    override fun getAlarms(): Flow<List<Alarm>> = dataSource.alarms

    override suspend fun getAlarmById(id: Long): Alarm? = dataSource.getAlarmById(id)

    override suspend fun insertAlarm(alarm: Alarm): Long = dataSource.insertAlarm(alarm)

    override suspend fun updateAlarm(alarm: Alarm) = dataSource.updateAlarm(alarm)

    override suspend fun deleteAlarm(id: Long) = dataSource.deleteAlarm(id)

    override suspend fun toggleAlarm(id: Long, isEnabled: Boolean) =
        dataSource.toggleAlarm(id, isEnabled)
}
