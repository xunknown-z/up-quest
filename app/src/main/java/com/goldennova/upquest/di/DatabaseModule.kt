package com.goldennova.upquest.di

import android.content.Context
import androidx.room.Room
import com.goldennova.upquest.data.local.AppDatabase
import com.goldennova.upquest.data.local.dao.AlarmDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "upquest.db",
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()

    @Provides
    fun provideAlarmDao(database: AppDatabase): AlarmDao = database.alarmDao()
}
