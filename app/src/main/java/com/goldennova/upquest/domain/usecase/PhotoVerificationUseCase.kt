package com.goldennova.upquest.domain.usecase

interface PhotoVerificationUseCase {
    suspend fun verify(capturedPath: String, referencePath: String): Boolean
}
