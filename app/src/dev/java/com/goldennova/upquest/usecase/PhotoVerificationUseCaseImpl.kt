package com.goldennova.upquest.usecase

import com.goldennova.upquest.domain.usecase.PhotoVerificationUseCase
import javax.inject.Inject

/** dev 빌드 전용 — 항상 인증 성공을 반환하는 Mock 구현체 */
class PhotoVerificationUseCaseImpl @Inject constructor() : PhotoVerificationUseCase {
    override suspend fun verify(capturedPath: String, referencePath: String): Boolean = true
}
