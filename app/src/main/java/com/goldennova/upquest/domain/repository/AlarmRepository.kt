package com.goldennova.upquest.domain.repository

import com.goldennova.upquest.domain.model.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAlarms(): Flow<List<Alarm>>
    suspend fun getAlarmById(id: Long): Alarm?
    suspend fun insertAlarm(alarm: Alarm): Long
    suspend fun updateAlarm(alarm: Alarm)
    suspend fun deleteAlarm(id: Long)
    suspend fun toggleAlarm(id: Long, isEnabled: Boolean)
}
