package com.goldennova.upquest.di

import com.goldennova.upquest.data.alarm.RingtoneAlarmSoundPlayer
import com.goldennova.upquest.domain.alarm.AlarmSoundPlayer
import com.goldennova.upquest.domain.usecase.PhotoVerificationUseCase
import com.goldennova.upquest.domain.usecase.PhotoVerificationUseCaseImpl
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.Binds
import dagger.Module
import dagger.Provides
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

    companion object {
        // 레이블 신뢰도 최소 기준 (0.0 ~ 1.0)
        private const val CONFIDENCE_THRESHOLD = 0.7f

        @Provides
        @Singleton
        fun provideImageLabeler(): ImageLabeler =
            ImageLabeling.getClient(
                ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
                    .build(),
            )
    }
}
