package com.goldennova.upquest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.goldennova.upquest.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    /** 전체 알람 목록을 Flow로 반환한다. DB 변경 시 자동으로 재방출된다. */
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAll(): Flow<List<AlarmEntity>>

    /** 특정 ID의 알람을 반환한다. 존재하지 않으면 null. */
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    /** 알람을 삽입하고 생성된 ID를 반환한다. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AlarmEntity): Long

    /** 기존 알람을 업데이트한다. */
    @Update
    suspend fun update(entity: AlarmEntity)

    /** 특정 ID의 알람을 삭제한다. */
    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)
}
