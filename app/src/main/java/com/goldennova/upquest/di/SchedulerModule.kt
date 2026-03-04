package com.goldennova.upquest.di

import com.goldennova.upquest.data.alarm.AlarmManagerScheduler
import com.goldennova.upquest.domain.alarm.AlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AlarmManagerScheduler): AlarmScheduler
}
