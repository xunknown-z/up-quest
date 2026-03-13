package com.goldennova.upquest.di

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import com.goldennova.upquest.data.alarm.AlarmManagerScheduler
import com.goldennova.upquest.domain.alarm.AlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SchedulerModule {

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
        alarmManager: AlarmManager,
    ): AlarmScheduler = AlarmManagerScheduler(context, alarmManager, Build.VERSION.SDK_INT)
}
