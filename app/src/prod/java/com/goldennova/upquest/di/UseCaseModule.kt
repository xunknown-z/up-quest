package com.goldennova.upquest.di

import com.goldennova.upquest.data.alarm.RingtoneAlarmSoundPlayer
import com.goldennova.upquest.domain.alarm.AlarmSoundPlayer
import com.goldennova.upquest.domain.usecase.PhotoVerificationUseCase
import com.goldennova.upquest.domain.usecase.PhotoVerificationUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    @Singleton
    abstract fun bindPhotoVerificationUseCase(
        impl: PhotoVerificationUseCaseImpl,
    ): PhotoVerificationUseCase

    @Binds
    @Singleton
    abstract fun bindAlarmSoundPlayer(
        impl: RingtoneAlarmSoundPlayer,
    ): AlarmSoundPlayer
}
