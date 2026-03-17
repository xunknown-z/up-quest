package com.goldennova.upquest.data.datasource

import com.goldennova.upquest.data.local.dao.AlarmDao
import com.goldennova.upquest.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomAlarmDataSource @Inject constructor(
    private val dao: AlarmDao,
) {

    fun getAll(): Flow<List<AlarmEntity>> = dao.getAll()

    suspend fun getById(id: Long): AlarmEntity? = dao.getById(id)

    suspend fun insert(entity: AlarmEntity): Long = dao.insert(entity)

    suspend fun update(entity: AlarmEntity) = dao.update(entity)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
