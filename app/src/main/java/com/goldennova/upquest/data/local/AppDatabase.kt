package com.goldennova.upquest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.goldennova.upquest.data.local.dao.AlarmDao
import com.goldennova.upquest.data.local.entity.AlarmEntity

@Database(
    entities = [AlarmEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN ringtoneUri TEXT")
            }
        }
    }
}
