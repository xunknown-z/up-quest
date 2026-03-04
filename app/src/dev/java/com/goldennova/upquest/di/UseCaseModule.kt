package com.goldennova.upquest.di

import com.goldennova.upquest.domain.usecase.PhotoVerificationUseCase
import com.goldennova.upquest.usecase.PhotoVerificationUseCaseImpl
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
}
