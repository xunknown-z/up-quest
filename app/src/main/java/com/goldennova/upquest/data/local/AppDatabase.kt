package com.goldennova.upquest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.goldennova.upquest.data.local.dao.AlarmDao
import com.goldennova.upquest.data.local.entity.AlarmEntity

@Database(
    entities = [AlarmEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
}
