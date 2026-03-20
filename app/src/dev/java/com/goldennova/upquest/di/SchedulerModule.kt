package com.goldennova.upquest.di

import android.content.Context
import android.os.Build
import com.goldennova.upquest.data.alarm.FakeAlarmScheduler
import com.goldennova.upquest.data.alarm.SystemVibrationPlayer
import com.goldennova.upquest.domain.alarm.AlarmScheduler
import com.goldennova.upquest.domain.alarm.VibrationPlayer
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
    fun provideAlarmScheduler(impl: FakeAlarmScheduler): AlarmScheduler = impl

    @Provides
    @Singleton
    fun provideVibrationPlayer(
        @ApplicationContext context: Context,
    ): VibrationPlayer = SystemVibrationPlayer(context, Build.VERSION.SDK_INT)
}
